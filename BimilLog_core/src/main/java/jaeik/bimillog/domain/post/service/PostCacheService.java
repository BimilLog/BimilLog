package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier1PostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostStoreAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final PostQueryRepository postQueryRepository;
    private final RedisTier1PostStoreAdapter redisTier1PostStoreAdapter;
    private final RedisTier2PostStoreAdapter redisTier2PostStoreAdapter;
    private final RedisRealTimePostStoreAdapter redisRealTimePostStoreAdapter;
    private final PostCacheRefresh postCacheRefresh;

    /**
     * PER의 expiry gap (초 단위)
     * <p>TTL 마지막 120초 동안 확률적으로 캐시를 갱신합니다.</p>
     */
    private static final int EXPIRY_GAP_SECONDS = 60;

    /**
     * <h3>실시간 인기 게시글 조회</h3>
     * <p>Redis Sorted Set에서 postId 목록을 조회하고 posts:realtime Hash에서 상세 정보를 획득합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return Redis에서 조회된 실시간 인기 게시글 페이지
     */
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        try {
            // 1. 총 개수 조회
            long totalCount = redisRealTimePostStoreAdapter.getRealtimePopularPostCount();
            if (totalCount == 0) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            // 2. score:realtime에서 페이징된 postId 조회 (순서 포함)
            List<Long> realtimePostIds = redisRealTimePostStoreAdapter
                    .getRealtimePopularPostIds(pageable.getOffset(), pageable.getPageSize());

            if (realtimePostIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, totalCount);
            }

            // 3. TTL 조회 및 확률적 조기 만료 체크
            checkAndRefreshCache(PostCacheFlag.REALTIME);

            // 4. 1차 캐시에서 Map으로 조회 (순서 정보 없음)
            Map<Long, PostSimpleDetail> cachedMap = redisTier1PostStoreAdapter.getCachedPostMap(PostCacheFlag.REALTIME);

            // 5. realtimePostIds 순서대로 결과 구성
            List<PostSimpleDetail> resultPosts = new ArrayList<>();

            for (Long postId : realtimePostIds) {
                PostSimpleDetail detail = cachedMap.get(postId);
                if (detail == null) { // 캐시 미스 시 DB 조회
                    Optional<PostDetail> postDetailOpt = postQueryRepository.findPostDetailWithCounts(postId, null);
                    if (postDetailOpt.isPresent()) {
                        detail = postDetailOpt.get().toSimpleDetail();
                        redisTier1PostStoreAdapter.cachePostList(PostCacheFlag.REALTIME, List.of(detail));
                    }
                }
                if (detail != null) {
                    resultPosts.add(detail);
                }
            }

            return new PageImpl<>(resultPosts, pageable, totalCount);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_REALTIME_ERROR, e);
        }
    }

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>Redis 캐시에서 주간 인기글 목록을 페이징으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return Redis에서 조회된 주간 인기 게시글 페이지
     */
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        try {
            // TTL 조회 및 확률적 조기 만료 체크
            checkAndRefreshCache(PostCacheFlag.WEEKLY);
            return redisTier1PostStoreAdapter.getCachedPostListPaged(PostCacheFlag.WEEKLY, pageable);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_WEEKLY_ERROR, e);
        }
    }


    /**
     * <h3>레전드 인기 게시글 목록 조회</h3>
     * <p>캐시된 레전드 게시글을 페이지네이션으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 인기 게시글 목록 페이지
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        try {
            // TTL 조회 및 확률적 조기 만료 체크
            checkAndRefreshCache(PostCacheFlag.LEGEND);
            return redisTier1PostStoreAdapter.getCachedPostListPaged(PostCacheFlag.LEGEND, pageable);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_LEGEND_ERROR, e);
        }
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>Redis에 캐시된 공지사항 목록을 페이징으로 조회합니다.</p>
     * <p>postIds 저장소 ID 개수와 캐시 목록 개수를 비교하여 정합성을 검증합니다.</p>
     * <p>개수 불일치 시 캐시 미스로 판단하고 비동기 갱신을 트리거합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 공지사항 페이지
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        try {
            // 1. postIds:notice Set에서 실제 공지 ID 개수 조회
            long storedCount = redisTier2PostStoreAdapter.getStoredPostIdsCount(PostCacheFlag.NOTICE);

            // 2. posts:notice Hash에서 캐시된 개수 조회
            long cachedCount = redisTier1PostStoreAdapter.getCachedPostCount(PostCacheFlag.NOTICE);

            // 3. 개수 비교: 저장소 ID 개수 != 캐시 목록 개수 → 캐시 미스
            if (cachedCount != storedCount) {
                postCacheRefresh.asyncRefreshCache(PostCacheFlag.NOTICE);
            }

            return redisTier1PostStoreAdapter.getCachedPostListPaged(PostCacheFlag.NOTICE, pageable);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_NOTICE_ERROR, e);
        }
    }

    /**
     * <h3>TTL 갱신 체크</h3>
     * <p>TTL 조회 및 갱신 시작</p>
     * @param flag 조회할 인기 게시글 유형
     */
    private void checkAndRefreshCache(PostCacheFlag flag) {
        Long ttl = redisTier1PostStoreAdapter.getPostListCacheTTL(flag);
        if (ttl != null && ttl > 0) {
            double randomFactor = ThreadLocalRandom.current().nextDouble();
            if (ttl - (randomFactor * EXPIRY_GAP_SECONDS) <= 0) {
                log.info("TTL갱신시작{}", flag);
                postCacheRefresh.asyncRefreshCache(flag);
            }
        }
    }
}