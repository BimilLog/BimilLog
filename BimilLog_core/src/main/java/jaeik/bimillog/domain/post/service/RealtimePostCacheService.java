package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.scheduler.RealTimePostScheduler;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <h2>RealtimePostCacheService</h2>
 * <p>실시간 인기글 목록 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>ZSET에서 top 5 ID를 조회하고, 글 단위 Hash(post:simple:{postId})에서 데이터를 가져옵니다.</p>
 * <p>캐시 미스된 글은 DB에서 조회하여 Hash를 생성합니다.</p>
 * <p>realtimeRedis 서킷브레이커가 적용되어 Redis 장애 시 Caffeine → DB 폴백 경로로 전환합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class RealtimePostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RedisPostHashAdapter redisPostHashAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final PostUtil postUtil;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";
    private static final int REALTIME_FALLBACK_LIMIT = 5;

    /**
     * 실시간 인기글 목록 조회
     * <p>ZSET에서 top 5 ID를 조회하고, 글 단위 Hash에서 pipeline으로 데이터를 가져옵니다.</p>
     * <p>누락된 글은 DB에서 조회하여 Hash를 생성합니다.</p>
     * <p>Redis 장애 시 서킷브레이커가 {@link #getRealtimePostsFallback}을 호출합니다.</p>
     */
    @CircuitBreaker(name = REALTIME_REDIS_CIRCUIT, fallbackMethod = "getRealtimePostsFallback")
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        List<Long> postIds = redisRealTimePostAdapter.getRangePostId();

        if (postIds.isEmpty()) {
            CacheMetricsLogger.miss(log, "realtime", "zset", "empty");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<PostSimpleDetail> cachedPosts = redisPostHashAdapter.getPostHashes(postIds);

        // 누락된 글이 있으면 DB에서 조회하여 Hash 생성
        if (cachedPosts.size() < postIds.size()) {
            List<Long> cachedIds = cachedPosts.stream().map(PostSimpleDetail::getId).toList();
            List<Long> missingIds = postIds.stream()
                    .filter(id -> !cachedIds.contains(id))
                    .toList();

            if (!missingIds.isEmpty()) {
                List<PostSimpleDetail> dbPosts = missingIds.stream()
                        .map(id -> postQueryRepository.findPostDetail(id, null).orElse(null))
                        .filter(detail -> detail != null)
                        .map(PostDetail::toSimpleDetail)
                        .toList();

                dbPosts.forEach(redisPostHashAdapter::createPostHash);

                cachedPosts = new ArrayList<>(cachedPosts);
                cachedPosts.addAll(dbPosts);
            }
        }

        if (cachedPosts.isEmpty()) {
            CacheMetricsLogger.miss(log, "realtime", "simple", "empty");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        CacheMetricsLogger.hit(log, "realtime", "simple", cachedPosts.size());

        // ZSET 순서(점수 내림차순)대로 정렬
        List<Long> orderedIds = postIds;
        List<PostSimpleDetail> finalPosts = cachedPosts;
        List<PostSimpleDetail> orderedPosts = orderedIds.stream()
                .map(id -> finalPosts.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null))
                .filter(p -> p != null)
                .toList();

        return postUtil.paginate(orderedPosts, pageable);
    }

    /**
     * <h3>서킷 OPEN 또는 Redis 예외 시 폴백</h3>
     * <p>Caffeine RealtimeScoreFallbackStore에서 점수 내림차순으로 글 ID를 조회하고, DB에서 상세 정보를 가져옵니다.</p>
     * <p>Caffeine 폴백도 실패하면 DB에서 직접 조회합니다.</p>
     */
    private Page<PostSimpleDetail> getRealtimePostsFallback(Pageable pageable, Throwable t) {
        log.warn("[CIRCUIT_FALLBACK] 실시간 인기글 Redis 장애: {}", t.getMessage());
        try {
            return getRealtimePostsFromCaffeine(pageable);
        } catch (Exception e) {
            log.warn("[CAFFEINE_FALLBACK] Caffeine 폴백 실패: {}", e.getMessage());
            return postQueryRepository.findRecentPopularPosts(pageable);
        }
    }

    /**
     * <h3>Caffeine 폴백 경로</h3>
     */
    private Page<PostSimpleDetail> getRealtimePostsFromCaffeine(Pageable pageable) {
        List<Long> postIds = realtimeScoreFallbackStore.getTopPostIds(0, REALTIME_FALLBACK_LIMIT);

        if (postIds.isEmpty()) {
            log.info("[CIRCUIT_OPEN] Caffeine 폴백 비어있음 - 빈 페이지 반환");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        log.info("[CIRCUIT_OPEN] Caffeine 폴백 데이터 있음 (count={}) - DB 배치 조회", postIds.size());
        return postQueryRepository.findPostSimpleDetailsByIds(postIds, pageable);
    }
}
