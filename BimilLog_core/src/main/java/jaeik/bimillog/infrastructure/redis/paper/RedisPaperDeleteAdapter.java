package jaeik.bimillog.infrastructure.redis.paper;

import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static jaeik.bimillog.infrastructure.redis.paper.RedisPaperKeys.REALTIME_PAPER_SCORE_KEY;

/**
 * <h2>롤링페이퍼 캐시 삭제 어댑터</h2>
 * <p>롤링페이퍼 캐시 삭제 포트의 Redis 구현체입니다.</p>
 * <p>실시간 인기 롤링페이퍼 목록에서 특정 회원의 롤링페이퍼 제거</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class RedisPaperDeleteAdapter {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * <h3>실시간 인기 롤링페이퍼 목록에서 회원 제거</h3>
     * <p>Redis Sorted Set에서 특정 회원의 롤링페이퍼를 제거합니다.</p>
     * <p>회원 탈퇴 또는 롤링페이퍼 삭제 시 호출됩니다.</p>
     *
     * @param memberId 제거할 회원 ID
     * @throws PaperCustomException Redis 삭제 중 오류가 발생한 경우
     * @author Jaeik
     * @since 2.0.0
     */
    public void removeMemberIdFromRealtimeScore(Long memberId) {
        try {
            redisTemplate.opsForZSet().remove(REALTIME_PAPER_SCORE_KEY, memberId.toString());
        } catch (Exception e) {
            throw new PaperCustomException(PaperErrorCode.REDIS_DELETE_ERROR, e);
        }
    }
}
