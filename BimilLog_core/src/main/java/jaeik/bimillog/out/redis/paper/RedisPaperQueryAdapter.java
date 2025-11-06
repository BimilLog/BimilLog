package jaeik.bimillog.out.redis.paper;

import jaeik.bimillog.domain.paper.application.port.out.RedisPaperQueryPort;
import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static jaeik.bimillog.out.redis.paper.RedisPaperKeys.REALTIME_PAPER_SCORE_KEY;

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
@Slf4j
public class RedisPaperQueryAdapter implements RedisPaperQueryPort {

    private final RedisTemplate<String, Object> redisTemplate;


    /**
     * <h3>실시간 인기 롤링페이퍼 조회 (Rank, Score 포함)</h3>
     * <p>Redis Sorted Set에서 점수가 높은 롤링페이퍼 정보를 지정된 범위로 조회합니다.</p>
     * <p>memberId, rank, popularityScore가 채워진 PopularPaperInfo 리스트를 반환합니다.</p>
     *
     * @param start 시작 인덱스 (0부터 시작)
     * @param end 종료 인덱스 (포함)
     * @return List<PopularPaperInfo> 지정된 범위의 롤링페이퍼 정보 (memberId, rank, popularityScore만 설정됨)
     * @throws PaperCustomException Redis 읽기 중 오류가 발생한 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<PopularPaperInfo> getRealtimePopularPapersWithRankAndScore(int start, int end) {
        try {
            // Sorted Set에서 점수와 함께 지정된 범위 조회
            Set<ZSetOperations.TypedTuple<Object>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(REALTIME_PAPER_SCORE_KEY, start, end);

            if (tuples == null || tuples.isEmpty()) {
                CacheMetricsLogger.miss(log, "paper:realtime", REALTIME_PAPER_SCORE_KEY, "sorted_set_empty");
                return Collections.emptyList();
            }

            List<PopularPaperInfo> result = new ArrayList<>();
            int rank = start + 1; // 시작 인덱스 기준으로 랭킹 계산

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

            CacheMetricsLogger.hit(log, "paper:realtime", REALTIME_PAPER_SCORE_KEY, result.size());
            return result;
        } catch (Exception e) {
            throw new PaperCustomException(PaperErrorCode.REDIS_READ_ERROR, e);
        }
    }
}
