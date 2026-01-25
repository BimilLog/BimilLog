package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;

/**
 * <h2>PostCacheService</h2>
 * <p>인기글(공지/실시간/주간/레전드) 목록 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>주간/레전드/공지는 Tier1 Hash에서 직접 조회합니다 (Tier2 제거됨).</p>
 * <p>실시간만 Score 저장소(Tier2)와 함께 사용합니다.</p>
 * <p>목록 캐시만 관리하며, 상세 캐시는 PostQueryService에서 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final FeaturedPostRepository featuredPostRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final PostCacheRefresh postCacheRefresh;
    private final DbFallbackGateway dbFallbackGateway;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * 실시간 인기글 목록 조회
     * <p>실시간은 Score 저장소(Tier2)를 사용하여 점수 기반 정렬이 필요합니다.</p>
     */
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        return getRealtimeCachedPosts(pageable,
                () -> postQueryRepository.findRecentPopularPosts(pageable));
    }

    /**
     * 주간 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        return getCachedPostsDirect(PostCacheFlag.WEEKLY, FallbackType.WEEKLY, pageable,
                () -> findFeaturedPostsByType(PostCacheFlag.WEEKLY, pageable));
    }

    /**
     * 전설 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        return getCachedPostsDirect(PostCacheFlag.LEGEND, FallbackType.LEGEND, pageable,
                () -> findFeaturedPostsByType(PostCacheFlag.LEGEND, pageable));
    }

    /**
     * 공지사항 목록 조회
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        return getCachedPostsDirect(PostCacheFlag.NOTICE, FallbackType.NOTICE, pageable,
                () -> findFeaturedPostsByType(PostCacheFlag.NOTICE, pageable));
    }

    /**
     * <h3>featured_post 테이블에서 조회 (DB 폴백용)</h3>
     * <p>featured_post 테이블에서 postId 목록을 조회 후 PostSimpleDetail로 변환합니다.</p>
     *
     * @param type     특집 유형 (WEEKLY, LEGEND, NOTICE)
     * @param pageable 페이징 정보
     * @return PostSimpleDetail 페이지
     */
    private Page<PostSimpleDetail> findFeaturedPostsByType(PostCacheFlag type, Pageable pageable) {
        List<Long> allPostIds = featuredPostRepository.findPostIdsByType(type);

        if (allPostIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allPostIds.size());

        if (start >= allPostIds.size()) {
            return new PageImpl<>(List.of(), pageable, allPostIds.size());
        }

        List<Long> pagedPostIds = allPostIds.subList(start, end);
        List<PostSimpleDetail> posts = postQueryRepository.findPostSimpleDetailsByIds(pagedPostIds);

        return new PageImpl<>(posts, pageable, allPostIds.size());
    }

    /**
     * <h3>주간/레전드/공지 캐시 조회)</h3>
     * <p>모든 게시글을 조회하고 페이징합니다.</p>
     *
     * @param type 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @param fallbackType 폴백 유형
     * @param pageable 페이징 정보
     * @param fallbackSupplier DB 폴백 공급자
     * @return 페이징된 게시글 목록
     */
    private Page<PostSimpleDetail> getCachedPostsDirect(PostCacheFlag type, FallbackType fallbackType,
                                                         Pageable pageable, Supplier<Page<PostSimpleDetail>> fallbackSupplier) {
        try {
            // Tier1 Hash에서 직접 조회 (개수 비교 없음)
            List<PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPostsList(type);

            if (cachedPosts.isEmpty()) {
                // 캐시 미스 → DB 폴백 (스케줄러가 다음 주기에 캐시 갱신)
                log.info("[CACHE_MISS] {} 캐시 미스 - DB 폴백 후 스케줄러 대기", type);
                return dbFallbackGateway.execute(fallbackType, pageable, fallbackSupplier);
            }

            // 페이징 처리
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), cachedPosts.size());

            if (start >= cachedPosts.size()) {
                return new PageImpl<>(List.of(), pageable, cachedPosts.size());
            }

            List<PostSimpleDetail> pagedPosts = cachedPosts.subList(start, end);

            return new PageImpl<>(pagedPosts, pageable, cachedPosts.size());
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", type, e.getMessage());
            return dbFallbackGateway.execute(fallbackType, pageable, fallbackSupplier);
        }
    }

    /**
     * <h3>실시간 인기글 캐시 조회</h3>
     *
     * @param pageable 페이징 정보
     * @param fallbackSupplier DB 폴백 공급자
     * @return 페이징된 게시글 목록
     */
    private Page<PostSimpleDetail> getRealtimeCachedPosts(Pageable pageable,
                                                           Supplier<Page<PostSimpleDetail>> fallbackSupplier) {
        try {
            // 범위 내 postId 목록 조회 (Score 저장소에서)
            List<Long> postIds = redisRealTimePostAdapter.getRangePostId(
                    PostCacheFlag.REALTIME, pageable.getOffset(), pageable.getPageSize());

            // 전체 카운트는 범위 내 ID로 설정 (실시간은 양이 많을 것을 우려)
            long totalCount = postIds.size();

            // 전체 카운트가 0이면 빈 페이지 반환
            if (totalCount == 0) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            List<PostSimpleDetail> resultPosts;
            // 실시간 전용 서킷 브레이커 로직 처리
            if (circuitBreakerRegistry.circuitBreaker("realtimeRedis").getState() == OPEN) {
                // 서킷이 열려있으면 DB에서 상세 정보 조회 (DB 서킷 브레이커 적용)
                resultPosts = dbFallbackGateway.executeList(
                        FallbackType.REALTIME,
                        postIds,
                        () -> postQueryRepository.findPostSimpleDetailsByIds(postIds)
                );
            } else {
                // Tier1에서 상세 정보 조회
                Map<Long, PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.REALTIME);

                // ID 비교로 캐시 동기화 (실시간만 유지)
                handleRealtimeCacheSync(cachedPosts.keySet(), postIds);

                resultPosts = postIds.stream().map(cachedPosts::get).filter(Objects::nonNull).toList();
            }

            return new PageImpl<>(resultPosts, pageable, totalCount);

        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] REALTIME Redis 장애: {}", e.getMessage());
            return dbFallbackGateway.execute(FallbackType.REALTIME, pageable, fallbackSupplier);
        }
    }

    /**
     * <h3>실시간 캐시 동기화 (ID 기반)</h3>
     * <p>Tier2(ZSet)의 postId와 Tier1(Hash)의 postId를 비교하여 캐시 동기화를 수행합니다.</p>
     * <ul>
     *   <li>ID 불일치: SET NX 락을 획득한 스레드만 비동기 갱신, 모든 스레드는 과거 캐시 반환</li>
     *   <li>ID 일치: TTL 기반 PER로 갱신 여부 결정</li>
     * </ul>
     *
     * @param tier1PostIds Tier1(Hash)에서 조회한 postId Set
     * @param tier2PostIds Tier2(ZSet)에서 조회한 postId 목록
     */
    private void handleRealtimeCacheSync(Set<Long> tier1PostIds, List<Long> tier2PostIds) {
        boolean idsMatch = redisSimplePostAdapter.isCacheIdsMatch(tier2PostIds, tier1PostIds);

        if (!idsMatch) {
            // ID 불일치: SET NX 락을 획득한 스레드만 갱신
            if (redisSimplePostAdapter.tryAcquireRefreshLock()) {
                log.info("[CACHE_SYNC] 실시간 캐시 ID 불일치 - 갱신 시작 (tier1={}, tier2={})",
                        tier1PostIds.size(), tier2PostIds.size());
                postCacheRefresh.asyncRefreshAllPostsWithLock(PostCacheFlag.REALTIME, tier2PostIds);
            } else {
                log.debug("[CACHE_SYNC] 실시간 캐시 ID 불일치 - 다른 스레드가 갱신 중, 과거 캐시 반환");
            }
        } else if (redisSimplePostAdapter.shouldRefreshHash(PostCacheFlag.REALTIME)) {
            // ID 일치 + TTL 임박: PER 기반 갱신
            log.debug("[CACHE_SYNC] 실시간 캐시 TTL 임박 - PER 갱신");
            postCacheRefresh.asyncRefreshAllPosts(PostCacheFlag.REALTIME, tier2PostIds);
        }
    }
}
