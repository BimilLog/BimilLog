package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.CachedPost;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.post.RedisPostKeys;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier1PostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostStoreAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <h2>PostCacheService</h2>
 * <p>캐시 조회 및 동기화 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>MGET 기반 개별 캐시 조회와 PER(Probabilistic Early Refresh)을 지원합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
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
     * <h3>실시간 인기 게시글 조회</h3>
     * <p>Redis Sorted Set에서 postId 목록을 조회하고 개별 캐시에서 상세 정보를 획득합니다.</p>
     * <p>MGET으로 한 번에 조회하며, 개별 PER을 적용합니다.</p>
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
            List<Long> postIds = redisRealTimePostStoreAdapter
                    .getRealtimePopularPostIds(pageable.getOffset(), pageable.getPageSize());

            if (postIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, totalCount);
            }

            // 3. MGET으로 캐시 조회 + PER 처리 + 캐시 미스 처리
            List<PostSimpleDetail> resultPosts = fetchPostsWithCache(PostCacheFlag.REALTIME, postIds);

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
            // 1. Tier2에서 전체 postIds 조회
            List<Long> allPostIds = redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.WEEKLY);
            if (allPostIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            // 2. 페이징 처리
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allPostIds.size());

            if (start >= allPostIds.size()) {
                return new PageImpl<>(List.of(), pageable, allPostIds.size());
            }

            List<Long> pagedPostIds = allPostIds.subList(start, end);

            // 3. MGET으로 캐시 조회 + PER 처리 + 캐시 미스 처리
            List<PostSimpleDetail> resultPosts = fetchPostsWithCache(PostCacheFlag.WEEKLY, pagedPostIds);

            return new PageImpl<>(resultPosts, pageable, allPostIds.size());
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
            // 1. Tier2에서 전체 postIds 조회
            List<Long> allPostIds = redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.LEGEND);
            if (allPostIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            // 2. 페이징 처리
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allPostIds.size());

            if (start >= allPostIds.size()) {
                return new PageImpl<>(List.of(), pageable, allPostIds.size());
            }

            List<Long> pagedPostIds = allPostIds.subList(start, end);

            // 3. MGET으로 캐시 조회 + PER 처리 + 캐시 미스 처리
            List<PostSimpleDetail> resultPosts = fetchPostsWithCache(PostCacheFlag.LEGEND, pagedPostIds);

            return new PageImpl<>(resultPosts, pageable, allPostIds.size());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_LEGEND_ERROR, e);
        }
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>Redis에 캐시된 공지사항 목록을 페이징으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 공지사항 페이지
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        try {
            // 1. Tier2에서 전체 postIds 조회
            List<Long> allPostIds = redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.NOTICE);
            if (allPostIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            // 2. 페이징 처리
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allPostIds.size());

            if (start >= allPostIds.size()) {
                return new PageImpl<>(List.of(), pageable, allPostIds.size());
            }

            List<Long> pagedPostIds = allPostIds.subList(start, end);

            // 3. MGET으로 캐시 조회 + PER 처리 + 캐시 미스 처리
            List<PostSimpleDetail> resultPosts = fetchPostsWithCache(PostCacheFlag.NOTICE, pagedPostIds);

            return new PageImpl<>(resultPosts, pageable, allPostIds.size());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_NOTICE_ERROR, e);
        }
    }

    /**
     * <h3>MGET 기반 캐시 조회 + PER + 캐시 미스 처리</h3>
     * <p>1. MGET으로 캐시 조회</p>
     * <p>2. PER 대상 게시글 비동기 갱신</p>
     * <p>3. 캐시 미스 게시글 DB 조회 후 캐시 저장</p>
     * <p>4. 요청 순서대로 결과 반환</p>
     *
     * @param type    캐시 유형
     * @param postIds 조회할 게시글 ID 목록 (순서 유지)
     * @return 순서가 유지된 게시글 목록
     */
    private List<PostSimpleDetail> fetchPostsWithCache(PostCacheFlag type, List<Long> postIds) {
        // 1. MGET으로 캐시 조회
        Map<Long, CachedPost> cachedPosts = redisTier1PostStoreAdapter.getCachedPosts(type, postIds);

        // 2. PER 대상 필터링 및 비동기 갱신
        List<Long> refreshIds = redisTier1PostStoreAdapter.filterRefreshNeeded(cachedPosts);
        if (!refreshIds.isEmpty()) {
            log.info("[PER] 비동기 갱신 트리거 - type={}, count={}", type, refreshIds.size());
            postCacheRefresh.asyncRefreshPosts(type, refreshIds);
        }

        // 3. 캐시 미스 처리
        List<Long> missedIds = redisTier1PostStoreAdapter.filterMissedIds(postIds, cachedPosts);
        if (!missedIds.isEmpty()) {
            log.info("[CACHE_MISS] DB 조회 시작 - type={}, count={}", type, missedIds.size());
            List<PostSimpleDetail> fromDb = fetchFromDbAndCache(type, missedIds);

            // 조회 결과를 cachedPosts에 추가
            for (PostSimpleDetail post : fromDb) {
                cachedPosts.put(post.getId(), new CachedPost(post, RedisPostKeys.POST_CACHE_TTL));
            }
        }

        // 4. 요청 순서대로 결과 구성
        return redisTier1PostStoreAdapter.toOrderedList(postIds, cachedPosts);
    }

    /**
     * <h3>DB에서 조회 후 캐시 저장</h3>
     *
     * @param type    캐시 유형
     * @param postIds 조회할 게시글 ID 목록
     * @return 조회된 게시글 목록
     */
    private List<PostSimpleDetail> fetchFromDbAndCache(PostCacheFlag type, List<Long> postIds) {
        List<PostSimpleDetail> posts = new ArrayList<>();

        for (Long postId : postIds) {
            Optional<PostDetail> postDetailOpt = postQueryRepository.findPostDetail(postId, null);
            if (postDetailOpt.isPresent()) {
                PostSimpleDetail simpleDetail = postDetailOpt.get().toSimpleDetail();
                posts.add(simpleDetail);

                // 개별 캐시 저장
                redisTier1PostStoreAdapter.cachePost(type, simpleDetail);
            }
        }

        return posts;
    }
}
