package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;

/**
 * <h2>PostCacheService</h2>
 * <p>인기글(공지/실시간/주간/레전드) 목록 캐시 조회 및 동기화 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>Hash 기반 캐시와 개수 비교를 통한 캐시 미스 감지, Redis TTL 기반 PER을 지원합니다.</p>
 * <p>목록 캐시만 관리하며, 상세 캐시는 PostQueryService에서 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RedisTier2PostAdapter redisTier2PostAdapter;
    private final PostCacheRefresh postCacheRefresh;
    private final DbFallbackGateway dbFallbackGateway;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * 실시간 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        return getCachedRealtimePosts(pageable,
                () -> postQueryRepository.findRecentPopularPosts(pageable));
    }

    /**
     * 주간 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        return getCachedTier2Posts(PostCacheFlag.WEEKLY, FallbackType.WEEKLY, pageable,
                () -> postQueryRepository.findWeeklyPopularPosts(pageable));
    }

    /**
     * 전설 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        return getCachedTier2Posts(PostCacheFlag.LEGEND, FallbackType.LEGEND, pageable,
                () -> postQueryRepository.findLegendaryPosts(pageable));
    }

    /**
     * 공지사항 목록 조회
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        return getCachedTier2Posts(PostCacheFlag.NOTICE, FallbackType.NOTICE, pageable,
                () -> postQueryRepository.findNoticePosts(pageable));
    }

    /**
     * 실시간 인기글 캐시 조회 전용 메서드
     */
    private Page<PostSimpleDetail> getCachedRealtimePosts(Pageable pageable,
                                                          Supplier<Page<PostSimpleDetail>> fallbackSupplier) {
        try {
            // 범위 내 postId 목록 조회
            List<Long> postIds = redisRealTimePostAdapter.getRangePostId(PostCacheFlag.REALTIME, pageable.getOffset(), pageable.getPageSize());

            // 전체 카운트 결정 범위 내 ID 값으로 설정
            long totalCount = postIds.size();

            // 전체 카운트가 0이면 빈 페이지 반환
            if (totalCount == 0) {
                return new PageImpl<>(List.of(), pageable, totalCount);
            }

            List<PostSimpleDetail> resultPosts;
            // 실시간 전용 서킷 브레이커 로직 처리
            if (circuitBreakerRegistry.circuitBreaker("realtimeRedis").getState() == OPEN) {
                // 서킷이 열려있으면 DB에서 상세 정보 조회
                resultPosts = postQueryRepository.findPostSimpleDetailsByIds(postIds);
            } else {
                // 타입별 모든 인기글 목록 캐시 조회
                Map<Long, PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.REALTIME);
                handleCacheSync(PostCacheFlag.REALTIME, cachedPosts.size(), totalCount, postIds);
                resultPosts = postIds.stream().map(cachedPosts::get).filter(Objects::nonNull).toList();
            }

            return new PageImpl<>(resultPosts, pageable, totalCount);

        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", PostCacheFlag.REALTIME, e.getMessage());
            return dbFallbackGateway.execute(FallbackType.REALTIME, pageable, fallbackSupplier);
        }
    }

    /**
     * 주간/레전드/공지 캐시 조회 전용 메서드
     */
    private Page<PostSimpleDetail> getCachedTier2Posts(PostCacheFlag type, FallbackType fallbackType,
                                                       Pageable pageable, Supplier<Page<PostSimpleDetail>> fallbackSupplier) {
        try {
            // 범위 내 postId 목록 조회
            List<Long> postIds = redisTier2PostAdapter.getRangePostId(type, pageable.getOffset(), pageable.getPageSize());

            // 전체 postId 목록 조회
            List<Long> allPostIds = redisTier2PostAdapter.getAllPostId(type);

            // 전체 카운트 결정
            long totalCount = allPostIds.size();

            // 전체 카운트가 0이면 빈 페이지 반환
            if (totalCount == 0) {
                return new PageImpl<>(List.of(), pageable, totalCount);
            }

            // 타입별 모든 인기글 목록 캐시 조회
            Map<Long, PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPosts(type);

            handleCacheSync(type, cachedPosts.size(), totalCount, allPostIds);
            List<PostSimpleDetail> resultPosts = postIds.stream().map(cachedPosts::get).filter(Objects::nonNull).toList();

            return new PageImpl<>(resultPosts, pageable, totalCount);

        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", type, e.getMessage());
            return dbFallbackGateway.execute(fallbackType, pageable, fallbackSupplier);
        }
    }

    /**
     * 캐시갱신 메서드
     */
    private void handleCacheSync(PostCacheFlag flag, int cachedSize, long totalCount, List<Long> allPostIds) {
        if (cachedSize != totalCount) {
            postCacheRefresh.asyncRefreshWithLock(flag, allPostIds);
        } else if (redisSimplePostAdapter.shouldRefreshHash(flag)) {
            postCacheRefresh.asyncRefreshAllPosts(flag, allPostIds);
        }
    }
}
