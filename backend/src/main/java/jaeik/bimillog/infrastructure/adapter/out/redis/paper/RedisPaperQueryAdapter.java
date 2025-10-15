package jaeik.bimillog.infrastructure.adapter.out.redis.paper;

import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static jaeik.bimillog.infrastructure.adapter.out.redis.paper.RedisPaperKeys.REALTIME_PAPER_SCORE_KEY;

/**
 * <h2>롤링페이퍼 캐시 조회 어댑터</h2>
 * <p>롤링페이퍼 캐시 조회 포트의 Redis 구현체입니다.</p>
 * <p>실시간 인기 롤링페이퍼 목록 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class RedisPaperQueryAdapter {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * <h3>실시간 인기 롤링페이퍼 memberId 목록 조회</h3>
     * <p>Redis Sorted Set에서 점수가 높은 상위 10개의 회원 ID를 조회합니다.</p>
     * <p>롤링페이퍼는 회원당 1개만 존재하므로 memberId를 반환합니다.</p>
     *
     * @return List<Long> 상위 10개 회원 ID 목록 (점수 내림차순)
     * @throws PaperCustomException Redis 읽기 중 오류가 발생한 경우
     * @author Jaeik
     * @since 2.0.0
     */
    public List<Long> getRealtimePopularMemberIds() {
        try {
            // Sorted Set에서 점수 높은 순으로 상위 10개 조회
            Set<Object> memberIds = redisTemplate.opsForZSet().reverseRange(REALTIME_PAPER_SCORE_KEY, 0, 9);
            if (memberIds == null || memberIds.isEmpty()) {
                return Collections.emptyList();
            }

            return memberIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .toList();
        } catch (Exception e) {
            throw new PaperCustomException(PaperErrorCode.REDIS_READ_ERROR, e);
        }
    }
}
