package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPostUpdateAdapter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    /**
     * <h3>실시간 인기글 점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 게시글의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 조회/댓글/추천 이벤트 발생 시 호출됩니다.</p>
     *
     * @param postId 점수를 증가시킬 게시글 ID
     * @param score  증가시킬 점수 (조회: 2점, 댓글: 3점, 추천: 4점)
     * @author Jaeik
     * @since 2.0.0
     */
    public void incrementRealtimePopularScore(Long postId, double score) {
        redisTemplate.opsForZSet().incrementScore(REALTIME_POST_SCORE_KEY, postId.toString(), score);
    }

    /**
     * <h3>실시간 인기글 전체 점수 지수감쇠 적용</h3>
     * <p>Redis Sorted Set의 모든 게시글 점수에 0.9를 곱하고, 임계값(1점) 이하의 게시글을 제거합니다.</p>
     * <p>PostScheduledService 스케줄러에서 5분마다 호출됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void applyRealtimePopularScoreDecay() {
        // 1. 모든 항목의 점수에 0.9 곱하기 (Lua 스크립트 사용)
        redisTemplate.execute(
                SCORE_DECAY_SCRIPT,
                List.of(REALTIME_POST_SCORE_KEY),
                REALTIME_POST_SCORE_DECAY_RATE
        );

        // 2. 임계값(1점) 이하의 게시글 제거
        redisTemplate.opsForZSet().removeRangeByScore(REALTIME_POST_SCORE_KEY, 0, REALTIME_POST_SCORE_THRESHOLD);
    }

    /**
     * <h3>캐시 갱신 분산 락 획득 (Redisson RLock)</h3>
     * <p>waitTime=0초, leaseTime=5초</p>
     * <p>⚠️ 문제 재현: leaseTime이 5초인데 캐시 갱신이 5초 이상 걸리면 락 해제됨 (캐시 스탬피드)</p>
     * <p>RLock은 내부적으로 스레드 ID와 UUID를 관리하여 소유권을 검증합니다.</p>
     *
     * @param type 캐시 유형
     * @return 락 획득 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean tryAcquireCacheRefreshLock(PostCacheFlag type) {
        String lockKey = "lock:cache:refresh:" + type.name();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);

            if (acquired) {
                log.debug("캐시 갱신 락 획득 성공: type={}, lockKey={}", type, lockKey);
            } else {
                log.debug("캐시 갱신 락 획득 실패 : type={}", type);
            }

            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("캐시 갱신 락 획득 중 인터럽트 발생: type={}", type, e);
            return false;
        }
    }

    /**
     * <h3>캐시 갱신 분산 락 해제 (Redisson RLock)</h3>
     * <p>RLock.unlock()은 내부적으로 소유권을 검증하므로 별도 Lua Script 불필요.</p>
     *
     * @param type 캐시 유형
     * @author Jaeik
     * @since 2.0.0
     */
    public void releaseCacheRefreshLock(PostCacheFlag type) {
        String lockKey = "lock:cache:refresh:" + type.name();
        RLock lock = redissonClient.getLock(lockKey);

        // isHeldByCurrentThread(): 현재 스레드가 락을 보유 중인지 확인
        if (lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                log.debug("캐시 갱신 락 해제 성공: type={}, lockKey={}", type, lockKey);
            } catch (IllegalMonitorStateException e) {
                // 락이 이미 만료되었거나 다른 스레드가 해제한 경우
                log.warn("캐시 갱신 락 해제 실패 (이미 만료됨): type={}", type, e);
            }
        } else {
            log.debug("캐시 갱신 락 해제 스킵 (현재 스레드 미보유): type={}", type);
        }
    }
}
