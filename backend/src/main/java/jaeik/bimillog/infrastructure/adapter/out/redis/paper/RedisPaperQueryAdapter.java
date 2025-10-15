package jaeik.bimillog.infrastructure.adapter.out.redis.paper;

import jaeik.bimillog.domain.paper.application.port.out.RedisPaperQueryPort;
import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
public class RedisPaperQueryAdapter implements RedisPaperQueryPort {

    private final RedisTemplate<String, Object> redisTemplate;


    /**
     * <h3>실시간 인기 롤링페이퍼 조회 (Rank, Score 포함)</h3>
     * <p>Redis Sorted Set에서 점수가 높은 상위 10개의 롤링페이퍼 정보를 조회합니다.</p>
     * <p>memberId, rank, popularityScore가 채워진 PopularPaperInfo 리스트를 반환합니다.</p>
     *
     * @return List&lt;PopularPaperInfo&gt; 상위 10개 롤링페이퍼 정보 (memberId, rank, popularityScore만 설정됨)
     * @throws PaperCustomException Redis 읽기 중 오류가 발생한 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<PopularPaperInfo> getRealtimePopularPapersWithRankAndScore() {
        try {
            // Sorted Set에서 점수와 함께 상위 10개 조회
            Set<ZSetOperations.TypedTuple<Object>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(REALTIME_PAPER_SCORE_KEY, 0, 9);

            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptyList();
            }

            List<PopularPaperInfo> result = new ArrayList<>();
            int rank = 1;

            for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                if (tuple.getValue() == null || tuple.getScore() == null) {
                    continue;
                }

                PopularPaperInfo info = new PopularPaperInfo();
                info.setMemberId(Long.valueOf(tuple.getValue().toString()));
                info.setRank(rank++);
                info.setPopularityScore(tuple.getScore());
                result.add(info);
            }

            return result;
        } catch (Exception e) {
            throw new PaperCustomException(PaperErrorCode.REDIS_READ_ERROR, e);
        }
    }
}
