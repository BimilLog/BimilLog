package jaeik.bimillog.infrastructure.redis.paper;

import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

import static jaeik.bimillog.infrastructure.redis.RedisKey.*;

/**
 * <h2>롤링페이퍼 캐시 갱신 어댑터</h2>
 * <p>롤링페이퍼 캐시 갱신 포트의 Redis 구현체입니다.</p>
 * <p>실시간 인기 롤링페이퍼 점수 증가 및 감쇠 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPaperUpdateAdapter {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * <h3>실시간 인기 롤링페이퍼 점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 롤링페이퍼의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 메시지 작성 또는 방문 이벤트 발생 시 호출됩니다.</p>
     *
     * @param memberId 점수를 증가시킬 회원 ID (롤링페이퍼 소유자)
     * @param score    증가시킬 점수
     */
    public void incrementRealtimePopularPaperScore(Long memberId, double score) {
        try {
            redisTemplate.opsForZSet().incrementScore(REALTIME_PAPER_SCORE_KEY, memberId.toString(), score);
            log.info("{}의 롤링 페이퍼{}점 증가", memberId, score);
        } catch (Exception e) {
            log.warn("{}의 롤링 페이퍼 레디스 작업 실패", memberId);
            throw new CustomException(ErrorCode.PAPER_REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>실시간 인기 롤링페이퍼 전체 점수 지수감쇠 적용</h3>
     * <p>Redis Sorted Set의 모든 롤링페이퍼 점수에 0.97를 곱하고, 임계값(1점) 이하의 롤링페이퍼를 제거합니다.</p>
     * <p>PaperScheduledService 스케줄러에서 주기적으로 호출됩니다.</p>
     */
    public void applyRealtimePopularPaperScoreDecay() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
                "local members = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES') " +
                "for i = 1, #members, 2 do " +
                "    local member = members[i] " +
                "    local score = tonumber(members[i + 1]) " +
                "    local newScore = score * tonumber(ARGV[1]) " +
                "    redis.call('ZADD', KEYS[1], newScore, member) " +
                "end " +
                "return redis.call('ZCARD', KEYS[1])"
        );
        script.setResultType(Long.class);
        try {
            // 1. 모든 항목의 점수에 0.97 곱하기 (Lua 스크립트 사용)
            redisTemplate.execute(
                    script,
                    List.of(REALTIME_PAPER_SCORE_KEY),
                    REALTIME_PAPER_SCORE_DECAY_RATE
            );

            // 2. 임계값(1점) 이하의 롤링페이퍼 제거
            redisTemplate.opsForZSet().removeRangeByScore(REALTIME_PAPER_SCORE_KEY, 0, REALTIME_PAPER_SCORE_THRESHOLD);

        } catch (Exception e) {
            throw new CustomException(ErrorCode.PAPER_REDIS_WRITE_ERROR, e);
        }
    }
}
