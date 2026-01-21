package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.port.RedisTier2CachePort;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
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
 * <p>인기글(공지/실시간/주간/레전드) 목록 캐시 조회 및 동기화 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>Hash 기반 캐시와 개수 비교를 통한 캐시 미스 감지, Redis TTL 기반 PER을 지원합니다.</p>
 * <p>목록 캐시만 관리하며, 상세 캐시는 PostQueryService에서 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Service
@Slf4j
public class PostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final PostCacheRefresh postCacheRefresh;
    private final DbFallbackGateway dbFallbackGateway;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<PostCacheFlag, RedisTier2CachePort> adapterMap;

    // 현재 실시간 인기글의 전체글은 5로 간주한다 향후 변경가능
    private static final long REALTIME_LIMIT = 5;

    public PostCacheService(PostQueryRepository postQueryRepository, RedisSimplePostAdapter redisSimplePostAdapter,
                            PostCacheRefresh postCacheRefresh, DbFallbackGateway dbFallbackGateway,
                            CircuitBreakerRegistry circuitBreakerRegistry, List<RedisTier2CachePort> adapters) {
        this.postQueryRepository = postQueryRepository;
        this.redisSimplePostAdapter = redisSimplePostAdapter;
        this.postCacheRefresh = postCacheRefresh;
        this.dbFallbackGateway = dbFallbackGateway;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.adapterMap = new EnumMap<>(PostCacheFlag.class);
        for (RedisTier2CachePort redisPort : adapters) {
            List<PostCacheFlag> supportedTypes = redisPort.getSupportedTypes();
            for (PostCacheFlag type : supportedTypes) {
                adapterMap.putIfAbsent(type, redisPort);
            }
        }
    }

    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        return getCachedPosts(PostCacheFlag.REALTIME, FallbackType.REALTIME, pageable,
                () -> postQueryRepository.findRecentPopularPosts(pageable));
    }

    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        return getCachedPosts(PostCacheFlag.WEEKLY, FallbackType.WEEKLY, pageable,
                () -> postQueryRepository.findWeeklyPopularPosts(pageable));
    }

    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        return getCachedPosts(PostCacheFlag.LEGEND, FallbackType.LEGEND, pageable,
                () -> postQueryRepository.findLegendaryPosts(pageable));
    }

    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        return getCachedPosts(PostCacheFlag.NOTICE, FallbackType.NOTICE, pageable,
                () -> postQueryRepository.findNoticePosts(pageable));
    }

    /**
     * 인기글 목록 조회
     */
    private Page<PostSimpleDetail> getCachedPosts(PostCacheFlag type, FallbackType fallbackType,
                                                  Pageable pageable, Supplier<Page<PostSimpleDetail>> fallbackSupplier) {
        try {
            RedisTier2CachePort adapter = adapterMap.get(type);

            // 1. 범위 내 postId 목록 조회
            List<Long> postIds = adapter.getRangePostId(type, pageable.getOffset(), pageable.getPageSize());

            // 2. 전체 카운트 결정 (실시간은 고정값 5, 나머지는 전체 ID 개수)
            long totalCount = REALTIME_LIMIT;

            List<Long> allPostIds = adapter.getAllPostId(type);

            if (type != PostCacheFlag.REALTIME) {
                totalCount = allPostIds.size();
            }

            if (allPostIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, totalCount);
            }

            List<PostSimpleDetail> resultPosts;

            // 3. 실시간 전용 서킷 브레이커 로직 처리
            if (type == PostCacheFlag.REALTIME && circuitBreakerRegistry.circuitBreaker("realtimeRedis").getState() == OPEN) {
                // 서킷이 열려있으면 DB에서 상세 정보 조회
                resultPosts = postQueryRepository.findPostSimpleDetailsByIds(postIds);
            } else {
                // 서킷이 닫혀있거나 실시간이 아닌 경우
                Map<Long, PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPosts(type);
                handleCacheSync(type, cachedPosts.size(), totalCount, allPostIds);
                resultPosts = postIds.stream().map(cachedPosts::get).filter(Objects::nonNull).toList();
            }

            return new PageImpl<>(resultPosts, pageable, totalCount);

        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", type, e.getMessage());
            return dbFallbackGateway.execute(fallbackType, pageable, fallbackSupplier);
        }
    }

    private void handleCacheSync(PostCacheFlag flag, int cachedSize, long totalCount, List<Long> allPostIds) {
        if (cachedSize != totalCount) {
            postCacheRefresh.asyncRefreshWithLock(flag, allPostIds);
        } else if (redisSimplePostAdapter.shouldRefreshHash(flag)) {
            postCacheRefresh.asyncRefreshAllPosts(flag, allPostIds);
        }
    }
}
