package jaeik.bimillog.domain.post.scheduler;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <h2>실시간 인기글 캐시 갱신 스케줄러</h2>
 * <p>1분마다 실시간 인기글 캐시를 갱신합니다.</p>
 * <p>ZSET에서 top 5 ID를 조회하고, 글 단위 Hash가 없는 글만 DB에서 조회하여 생성합니다.</p>
 * <p>이미 존재하는 글의 카운트는 HINCRBY로 이미 최신 상태이므로 재조회하지 않습니다.</p>
 * <p>분산 락을 사용하여 다중 인스턴스 환경에서 하나의 인스턴스만 갱신을 수행합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Log(logResult = false, logExecutionTime = true, message = "실시간 캐시 갱신 스케줄러")
@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimePostScheduler {

    private final PostQueryRepository postQueryRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RedisPostHashAdapter redisPostHashAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";
    private static final int REALTIME_FALLBACK_LIMIT = 5;
    private static final int MAX_RETRY = 2;
    private static final long RETRY_INITIAL_DELAY_MS = 2000;
    private static final int RETRY_MULTIPLIER = 4;

    private boolean isRealtimeRedisCircuitOpen() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(REALTIME_REDIS_CIRCUIT);
        CircuitBreaker.State state = cb.getState();
        return state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN;
    }

    /**
     * <h3>1분마다 실시간 인기글 캐시 갱신 (증분)</h3>
     * <p>분산 락 획득 → ZSET에서 top 5 ID → 없는 글만 DB 조회 → Hash 생성 → 락 해제</p>
     * <p>실패 시 최대 2회 재시도합니다 (2s → 8s 지수 백오프).</p>
     */
    @Scheduled(fixedRate = 60000)
    public void refreshRealtimeCache() {
        String lockValue = redisSimplePostAdapter.tryAcquireSchedulerLock();
        if (lockValue == null) {
            log.debug("[SCHEDULER] 분산 락 획득 실패 - 다른 인스턴스가 갱신 중");
            return;
        }

        try {
            List<Long> postIds = isRealtimeRedisCircuitOpen()
                    ? realtimeScoreFallbackStore.getTopPostIds(0, REALTIME_FALLBACK_LIMIT)
                    : redisRealTimePostAdapter.getRangePostId();

            if (postIds.isEmpty()) return;

            ensurePostHashesExist(postIds);
        } catch (Exception e) {
            log.error("[SCHEDULER] REALTIME 캐시 갱신 실패: {}", e.getMessage());
        } finally {
            redisSimplePostAdapter.releaseSchedulerLock(lockValue);
        }
    }

    /**
     * <h3>글 단위 Hash가 없는 글만 DB에서 조회하여 생성</h3>
     * <p>이미 존재하는 글은 HINCRBY로 카운트가 이미 최신이므로 스킵합니다.</p>
     */
    private void ensurePostHashesExist(List<Long> postIds) {
        long delay = RETRY_INITIAL_DELAY_MS;

        for (int attempt = 0; true; attempt++) {
            try {
                List<Long> missingIds = postIds.stream()
                        .filter(postId -> !redisPostHashAdapter.existsPostHash(postId))
                        .toList();

                if (missingIds.isEmpty()) {
                    log.debug("[SCHEDULER] 모든 실시간 인기글 Hash 존재 - 갱신 불필요");
                    return;
                }

                List<PostSimpleDetail> posts = missingIds.stream()
                        .map(postId -> postQueryRepository.findPostDetail(postId, null).orElse(null))
                        .filter(Objects::nonNull)
                        .map(PostDetail::toSimpleDetail)
                        .toList();

                posts.forEach(redisPostHashAdapter::createPostHash);
                log.info("[SCHEDULER] REALTIME 캐시 증분 갱신: 신규={}개, 기존={}개",
                        posts.size(), postIds.size() - missingIds.size());
                return;
            } catch (Exception e) {
                if (attempt == MAX_RETRY) {
                    log.error("[SCHEDULER] REALTIME 캐시 갱신 최종 실패 ({}회 시도): {}",
                            MAX_RETRY + 1, e.getMessage());
                    return;
                }
                log.warn("[SCHEDULER] REALTIME 캐시 갱신 실패 (시도 {}/{}), {}ms 후 재시도: {}",
                        attempt + 1, MAX_RETRY + 1, delay, e.getMessage());
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                delay *= RETRY_MULTIPLIER;
            }
        }
    }

    /**
     * <h3>실시간 인기 게시글 점수 지수감쇠 적용</h3>
     * <p>스프링 스케줄러를 통해 10분마다 실시간 인기글 점수에 감쇠를 적용합니다.</p>
     * <p>서킷 닫힘(Redis 정상): Redis ZSet에 감쇠 적용</p>
     * <p>서킷 열림(Redis 장애): Caffeine 폴백 저장소에 감쇠 적용</p>
     */
    @Scheduled(fixedRate = 60000 * 10) // 10분마다
    public void applyRealtimeScoreDecay() {
        if (isRealtimeRedisCircuitOpen()) {
            try {
                realtimeScoreFallbackStore.applyDecay();
            } catch (Exception e) {
                log.error("Fallback 저장소 지수감쇠 적용 실패", e);
            }
        } else {
            try {
                redisRealTimePostAdapter.applyRealtimePopularScoreDecay();
            } catch (Exception e) {
                log.error("Redis 실시간 인기글 점수 지수감쇠 적용 실패", e);
            }
        }
    }
}
