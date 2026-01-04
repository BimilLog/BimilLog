package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;

/**
 * <h2>레디스 실시간 인기글 저장소 어댑터</h2>
 * <p>티어2 이자 실시간 인기글 ID를 관리한다.</p>
 *
 * @author Jaeik
 * @version 2.4.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisRealTimePostStoreAdapter {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * <h3>상위 5개 글 ID 목록 조회</h3>
     * <p>Redis Sorted Set에서 점수가 높은 상위 5개의 게시글 ID를 조회합니다.</p>
     *
     * @return List<Long> 상위 5개 게시글 ID 목록 (점수 내림차순)
     */
    public List<Long> getRealtimePopularPostIds() {
        Set<Object> postIds = redisTemplate.opsForZSet().reverseRange(REALTIME_POST_SCORE_KEY, 0, 4);
        if (postIds == null || postIds.isEmpty()) {
            CacheMetricsLogger.miss(log, "post:realtime", REALTIME_POST_SCORE_KEY, "sorted_set_empty");
            return Collections.emptyList();
        }

        List<Long> ids =  postIds.stream()
                .map(Object::toString)
                .map(Long::valueOf)
                .toList();;

        CacheMetricsLogger.hit(log, "post:realtime", REALTIME_POST_SCORE_KEY, ids.size());
        return ids;
    }

    /**
     * <h3>점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 게시글의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 조회/댓글/추천 이벤트 발생 시 호출됩니다.</p>
     *
     * @param postId 점수를 증가시킬 게시글 ID
     * @param score  증가시킬 점수 (조회: 2점, 댓글: 3점, 추천: 4점)
     */
    public void incrementRealtimePopularScore(Long postId, double score) {
        redisTemplate.opsForZSet().incrementScore(REALTIME_POST_SCORE_KEY, postId.toString(), score);
    }

    /**
     * <h3>점수 감쇠 (스케쥴러)</h3>
     * <p>Redis Sorted Set의 모든 게시글 점수에 0.9를 곱하고, 임계값(1점) 이하의 게시글을 제거합니다.</p>
     * <p>PostScheduledService 스케줄러에서 5분마다 호출됩니다.</p>
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
     * <h3>캐시 삭제</h3>
     * <p>score:realtime Sorted Set에서 특정 postId를 삭제합니다.</p>
     * <p>게시글 삭제 시 실시간 인기글 점수 정리를 위해 호출됩니다.</p>
     *
     * @param postId 제거할 게시글 ID
     */
    public void removePostIdFromRealtimeScore(Long postId) {
        redisTemplate.opsForZSet().remove(REALTIME_POST_SCORE_KEY, postId.toString());
    }
}
