package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
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
                () -> postQueryRepository.findWeeklyPopularPosts(pageable));
    }

    /**
     * 전설 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        return getCachedPostsDirect(PostCacheFlag.LEGEND, FallbackType.LEGEND, pageable,
                () -> postQueryRepository.findLegendaryPosts(pageable));
    }

    /**
     * 공지사항 목록 조회
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        return getCachedPostsDirect(PostCacheFlag.NOTICE, FallbackType.NOTICE, pageable,
                () -> postQueryRepository.findNoticePosts(pageable));
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

                // 개수 비교로 캐시 동기화 (실시간만 유지)
                handleRealtimeCacheSync(cachedPosts.size(), totalCount, postIds);

                resultPosts = postIds.stream().map(cachedPosts::get).filter(Objects::nonNull).toList();
            }

            return new PageImpl<>(resultPosts, pageable, totalCount);

        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] REALTIME Redis 장애: {}", e.getMessage());
            return dbFallbackGateway.execute(FallbackType.REALTIME, pageable, fallbackSupplier);
        }
    }

    /**
     * <h3>실시간 캐시 동기화</h3>
     * <p>실시간만 개수 비교를 통한 캐시 동기화를 수행합니다.</p>
     */
    private void handleRealtimeCacheSync(int cachedSize, long totalCount, List<Long> postIds) {
        if (cachedSize != totalCount) {
            if (redisSimplePostAdapter.shouldRefreshOnMismatch(PostCacheFlag.REALTIME)) {
                postCacheRefresh.asyncRefreshAllPosts(PostCacheFlag.REALTIME, postIds);
            }
        } else if (redisSimplePostAdapter.shouldRefreshHash(PostCacheFlag.REALTIME)) {
            postCacheRefresh.asyncRefreshAllPosts(PostCacheFlag.REALTIME, postIds);
        }
    }
}
