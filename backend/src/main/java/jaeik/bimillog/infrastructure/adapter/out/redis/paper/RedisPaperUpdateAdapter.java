package jaeik.bimillog.infrastructure.adapter.out.redis.paper;

import jaeik.bimillog.domain.paper.application.port.out.RedisPaperUpdatePort;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static jaeik.bimillog.infrastructure.adapter.out.redis.paper.RedisPaperKeys.*;

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
public class RedisPaperUpdateAdapter implements RedisPaperUpdatePort {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * <h3>실시간 인기 롤링페이퍼 점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 롤링페이퍼의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 메시지 작성 또는 방문 이벤트 발생 시 호출됩니다.</p>
     *
     * @param memberId 점수를 증가시킬 회원 ID (롤링페이퍼 소유자)
     * @param score    증가시킬 점수
     * @throws PaperCustomException Redis 쓰기 중 오류가 발생한 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void incrementRealtimePopularPaperScore(Long memberId, double score) {
        try {
            redisTemplate.opsForZSet().incrementScore(REALTIME_PAPER_SCORE_KEY, memberId.toString(), score);
            log.info("{}의 롤링 페이퍼{}점 증가", memberId, score);
        } catch (Exception e) {
            log.warn("{}의 롤링 페이퍼 레디스 작업 실패", memberId);
            throw new PaperCustomException(PaperErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>실시간 인기 롤링페이퍼 전체 점수 지수감쇠 적용</h3>
     * <p>Redis Sorted Set의 모든 롤링페이퍼 점수에 0.95를 곱하고, 임계값(1점) 이하의 롤링페이퍼를 제거합니다.</p>
     * <p>PaperScheduledService 스케줄러에서 주기적으로 호출됩니다.</p>
     *
     * @throws PaperCustomException Redis 쓰기 중 오류가 발생한 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void applyRealtimePopularPaperScoreDecay() {
        try {
            // 1. 모든 항목의 점수에 0.95 곱하기 (Lua 스크립트 사용)
            redisTemplate.execute(
                    SCORE_DECAY_SCRIPT,
                    List.of(REALTIME_PAPER_SCORE_KEY),
                    REALTIME_PAPER_SCORE_DECAY_RATE
            );

            // 2. 임계값(1점) 이하의 롤링페이퍼 제거
            redisTemplate.opsForZSet().removeRangeByScore(REALTIME_PAPER_SCORE_KEY, 0, REALTIME_PAPER_SCORE_THRESHOLD);

        } catch (Exception e) {
            throw new PaperCustomException(PaperErrorCode.REDIS_WRITE_ERROR, e);
        }
    }
}
