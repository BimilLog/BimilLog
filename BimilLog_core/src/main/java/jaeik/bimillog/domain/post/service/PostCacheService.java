package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.out.PostQueryRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.post.RedisPostQueryAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostSaveAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <h2>PostCacheService</h2>
 * <p>캐시 조회 및 동기화 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>실시간, 주간, 레전드 인기글 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheService {
    private final RedisPostSaveAdapter redisPostSaveAdapter;
    private final PostQueryRepository postQueryRepository;
    private final RedisPostQueryAdapter redisPostQueryAdapter;
    private final PostCacheRefresh postCacheRefresh;

    /**
     * PER의 expiry gap (초 단위)
     * <p>TTL 마지막 120초 동안 확률적으로 캐시를 갱신합니다.</p>
     */
    private static final int EXPIRY_GAP_SECONDS = 120;

    /**
     * <h3>실시간 인기 게시글 조회</h3>
     * <p>Redis Sorted Set에서 postId 목록을 조회하고 posts:realtime Hash에서 상세 정보를 획득합니다.</p>
     * <p>확률적 선계산 기법을 적용하여 캐시 스탬피드를 방지합니다.</p>
     * <p>TTL 마지막 2분 동안 랜덤 확률로 비동기 갱신을 트리거합니다.</p>
     *
     * @return Redis에서 조회된 실시간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<PostSimpleDetail> getRealtimePosts() {
        try {
            // 1. score:realtime에서 상위 5개 postId 조회 (순서 포함)
            List<Long> realtimePostIds = redisPostQueryAdapter.getRealtimePopularPostIds();

            if (realtimePostIds.isEmpty()) {
                return List.of();
            }

            // 2. TTL 조회 및 확률적 조기 만료 체크
            checkAndRefreshCache(PostCacheFlag.REALTIME);

            // 3. 1차 캐시에서 Map으로 조회 (순서 정보 없음)
            Map<Long, PostSimpleDetail> cachedMap = redisPostQueryAdapter.getCachedPostMap(PostCacheFlag.REALTIME);

            // 4. realtimePostIds 순서대로 결과 구성
            List<PostSimpleDetail> resultPosts = new ArrayList<>();

            for (Long postId : realtimePostIds) {
                PostSimpleDetail detail = cachedMap.get(postId);
                if (detail == null) { // 캐시 미스 시 DB 조회
                    Optional<PostDetail> postDetailOpt = postQueryRepository.findPostDetailWithCounts(postId, null);
                    if (postDetailOpt.isPresent()) {
                        detail = postDetailOpt.get().toSimpleDetail();
                        redisPostSaveAdapter.cachePostList(PostCacheFlag.REALTIME, List.of(detail));
                    }
                }
                if (detail != null) {
                    resultPosts.add(detail);
                }
            }

            return resultPosts;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_REALTIME_ERROR, e);
        }
    }

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>Redis 캐시에서 주간 인기글 목록을 조회합니다.</p>
     * <p>PER 기법을 적용하여 캐시 스탬피드를 방지합니다.</p>
     * <p>TTL 마지막 2분 동안 랜덤 확률로 비동기 갱신을 트리거하며, 사용자는 기존 캐시를 즉시 반환받습니다.</p>
     *
     * @return Redis에서 조회된 주간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<PostSimpleDetail> getWeeklyPosts() {
        try {
            // TTL 조회 및 확률적 조기 만료 체크
            checkAndRefreshCache(PostCacheFlag.WEEKLY);
            return redisPostQueryAdapter.getCachedPostList(PostCacheFlag.WEEKLY);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_WEEKLY_ERROR, e);
        }
    }


    /**
     * <h3>레전드 인기 게시글 목록 조회</h3>
     * <p>캐시된 레전드 게시글을 페이지네이션으로 조회합니다.</p>
     * <p>확률적 선계산 기법을 적용하여 캐시 스탬피드를 방지합니다.</p>
     * <p>TTL 마지막 2분 동안 랜덤 확률로 비동기 갱신을 트리거합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 인기 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        try {
            // TTL 조회 및 확률적 조기 만료 체크
            checkAndRefreshCache(PostCacheFlag.LEGEND);
            return redisPostQueryAdapter.getCachedPostListPaged(pageable);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_LEGEND_ERROR, e);
        }
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>Redis에 캐시된 공지사항 목록을 조회합니다.</p>
     * <p>postIds 저장소 ID 개수와 캐시 목록 개수를 비교하여 정합성을 검증합니다.</p>
     * <p>개수 불일치 시 캐시 미스로 판단하고 postIds 저장소에서 ID 목록을 가져와 DB 조회 후 반환합니다.</p>
     *
     * @return 공지사항 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<PostSimpleDetail> getNoticePosts() {
        try {
            // 1. postIds:notice Set에서 실제 공지 ID 목록 조회
            List<Long> storedPostIds = redisPostQueryAdapter.getStoredPostIds(PostCacheFlag.NOTICE);

            // 2. posts:notice Hash에서 캐시된 목록 조회
            List<PostSimpleDetail> cachedList = redisPostQueryAdapter.getCachedPostList(PostCacheFlag.NOTICE);

            // 3. 개수 비교: 저장소 ID 개수 != 캐시 목록 개수 → 캐시 미스
            if (cachedList.size() != storedPostIds.size()) {
                postCacheRefresh.asyncRefreshCache(PostCacheFlag.NOTICE);
            }

            return cachedList;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_NOTICE_ERROR, e);
        }
    }

    /**
     * <h3>PER</h3>
     * <p>TTL 조회 및 갱신 시작</p>
     * @param flag 조회할 인기 게시글 유형
     * @author Jaeik
     * @since 2.4.0
     */
    private void checkAndRefreshCache(PostCacheFlag flag) {
        Long ttl = redisPostQueryAdapter.getPostListCacheTTL(flag);
        if (ttl != null && ttl > 0) {
            double randomFactor = ThreadLocalRandom.current().nextDouble();
            if (ttl - (randomFactor * EXPIRY_GAP_SECONDS) <= 0) {
                postCacheRefresh.asyncRefreshCache(flag);
            }
        }
    }
}