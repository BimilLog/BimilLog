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
@RequiredArgsConstructor
public class PostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisTier2PostAdapter redisTier2PostAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final PostCacheRefresh postCacheRefresh;
    private final DbFallbackGateway dbFallbackGateway;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * 실시간 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        return getCachedPosts(PostCacheFlag.REALTIME, FallbackType.REALTIME, pageable,
                () -> postQueryRepository.findRecentPopularPosts(pageable));
    }

    /**
     * 주간 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        return getCachedPosts(PostCacheFlag.WEEKLY, FallbackType.WEEKLY, pageable,
                () -> postQueryRepository.findWeeklyPopularPosts(pageable));
    }

    /**
     * 전설 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        return getCachedPosts(PostCacheFlag.LEGEND, FallbackType.LEGEND, pageable,
                () -> postQueryRepository.findLegendaryPosts(pageable));
    }

    /**
     * 공지사항 목록 조회
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        return getCachedPosts(PostCacheFlag.NOTICE, FallbackType.NOTICE, pageable,
                () -> postQueryRepository.findNoticePosts(pageable));
    }

    /**
     * 목록 캐시 조회 공통 메서드
     */
    private Page<PostSimpleDetail> getCachedPosts(PostCacheFlag type, FallbackType fallbackType,
                                                  Pageable pageable, Supplier<Page<PostSimpleDetail>> fallbackSupplier) {
        try {
            // 범위 내 postId 목록 조회
            List<Long> postIds = getRangePostId(type, pageable.getOffset(), pageable.getPageSize());

            // 전체 postId 목록 조회 (실시간은 0 반환) (실시간은 양이 많을 것을 우려해서 범위를 5로 제한한 범위 내 postId를 기준으로 사용)
            List<Long> allPostIds = getAllPostId(type);

            // 전체 카운트 결정 범위 내 ID 값으로 설정
            long totalCount = postIds.size();

            // 실시간 제외하고 페이징 토탈 카운트를 전체 postId로 설정 이렇게하면 실시간은 범위 내 값으로 설정 됨
            if (type != PostCacheFlag.REALTIME) {
                totalCount = allPostIds.size();
            }

            // 전체 카운트가 0이면 빈 페이지 반환
            if (totalCount == 0){
                return new PageImpl<>(List.of(), pageable, totalCount);
            }

            List<PostSimpleDetail> resultPosts;
            // 실시간 전용 서킷 브레이커 로직 처리
            if (type == PostCacheFlag.REALTIME && circuitBreakerRegistry.circuitBreaker("realtimeRedis").getState() == OPEN) {
                // 서킷이 열려있으면 DB에서 상세 정보 조회 (DB 서킷 브레이커 적용)
                resultPosts = dbFallbackGateway.executeList(
                        FallbackType.REALTIME,
                        postIds,
                        () -> postQueryRepository.findPostSimpleDetailsByIds(postIds)
                );
            } else {
                // 타입별 모든 인기글 목록 캐시 조회
                Map<Long, PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPosts(type);

                // 서킷이 닫혀있는 실시간
                if (type == PostCacheFlag.REALTIME) {
                    handleCacheSync(type, cachedPosts.size(), totalCount, postIds);
                } else {
                    handleCacheSync(type, cachedPosts.size(), totalCount, allPostIds);
                }
                resultPosts = postIds.stream().map(cachedPosts::get).filter(Objects::nonNull).toList();
            }

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

    /**
     * 타입에 따라 적절한 어댑터에서 범위 내 postId 목록 조회
     */
    private List<Long> getRangePostId(PostCacheFlag type, long start, long end) {
        if (type == PostCacheFlag.REALTIME) {
            return redisRealTimePostAdapter.getRangePostId(type, start, end);
        }
        return redisTier2PostAdapter.getRangePostId(type, start, end);
    }

    /**
     * 타입에 따라 적절한 어댑터에서 전체 postId 목록 조회
     */
    private List<Long> getAllPostId(PostCacheFlag type) {
        if (type == PostCacheFlag.REALTIME) {
            return redisRealTimePostAdapter.getAllPostId(type);
        }
        return redisTier2PostAdapter.getAllPostId(type);
    }
}
