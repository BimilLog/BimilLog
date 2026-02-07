package jaeik.bimillog.infrastructure.redis.post;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * <h2>레디스 실시간 인기글 저장소 어댑터</h2>
 * <p>실시간 인기글 ZSet 기반으로 인기글 ID를 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisRealTimePostAdapter {
    private static final String REALTIME_SCORE_KEY = getScoreStorageKey(PostCacheFlag.REALTIME);

    private final RedisTemplate<String, Long> redisTemplate;
    private final RealtimeScoreFallbackStore fallbackStore;

    /**
     * 실시간 인기글 점수 Sorted Set 키
     * <p>Value Type: ZSet (postId, score)</p>
     */
    public static final String REALTIME_POST_SCORE_KEY = "post:realtime:score";

    /**
     * 실시간 인기글 점수 감쇠율 (0.97)
     * <p>5분마다 모든 게시글 점수에 곱해져 시간 경과에 따른 인기도 감소를 반영합니다.</p>
     */
    public static final double REALTIME_POST_SCORE_DECAY_RATE = 0.97;

    /**
     * 실시간 인기글 제거 임계값 (1.0)
     * <p>이 값 이하의 점수를 가진 게시글은 실시간 인기글 목록에서 제거됩니다.</p>
     */
    public static final double REALTIME_POST_SCORE_THRESHOLD = 1.0;

    /**
     * 게시글 캐시 접두사
     */
    public static final String POST_PREFIX = "post:";



    /**
     * <h3>실시간 인기글 조회</h3>
     * <p>실시간 인기글 에서 점수가 높은 게시글 ID순서대로 조회합니다.</p>
     * <p>서킷브레이커는 {@link jaeik.bimillog.domain.post.service.RealtimePostCacheService}에서 관리합니다.</p>
     *
     * @param start 시작 위치
     * @param end 조회 개수
     * @return List<Long> 게시글 ID 목록 (점수 내림차순)
     */
    public List<Long> getRangePostId(PostCacheFlag type, long start, long end) {
        Set<Long> set = redisTemplate.opsForZSet().reverseRange(REALTIME_SCORE_KEY, 0, 4);
        return new ArrayList<>(Optional.ofNullable(set).orElseGet(Collections::emptySet));
    }

    /**
     * <h3>점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 게시글의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 조회/댓글/추천 이벤트 발생 시 호출됩니다.</p>
     * <p>Redis 장애 시 CircuitBreaker가 자동으로 fallbackMethod를 호출합니다.</p>
     *
     * @param postId 점수를 증가시킬 게시글 ID
     * @param score  증가시킬 점수 (조회: 2점, 댓글: 3점, 추천: 4점)
     */
    @CircuitBreaker(name = "realtimeRedis", fallbackMethod = "incrementScoreFallback")
    public void incrementRealtimePopularScore(Long postId, double score) {
        redisTemplate.opsForZSet().incrementScore(REALTIME_SCORE_KEY, postId, score);
    }

    /**
     * <h3>점수 증가 폴백</h3>
     * <p>서킷 OPEN 또는 Redis 실패 시 ConcurrentHashMap에 점수를 저장합니다.</p>
     *
     * @param postId 게시글 ID
     * @param score  증가시킬 점수
     * @param t      발생한 예외
     */
    @SuppressWarnings("unused")
    private void incrementScoreFallback(Long postId, double score, Throwable t) {
        log.warn("[CIRCUIT_FALLBACK] Redis 실패, 폴백 저장소 사용: postId={}, error={}", postId, t.getMessage());
        fallbackStore.incrementScore(postId, score);
    }

    /**
     * 실시간 인기글 점수 감쇠를 위한 Lua 스크립트
     * <p>Redis Sorted Set의 모든 게시글 점수에 SCORE_DECAY_RATE(0.9)를 곱합니다.</p>
     */
    public static final RedisScript<Long> SCORE_DECAY_SCRIPT;

    static {
        String luaScript =
                "local members = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES') " +
                        "for i = 1, #members, 2 do " +
                        "    local member = members[i] " +
                        "    local score = tonumber(members[i + 1]) " +
                        "    local newScore = score * tonumber(ARGV[1]) " +
                        "    redis.call('ZADD', KEYS[1], newScore, member) " +
                        "end " +
                        "return redis.call('ZCARD', KEYS[1])";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        SCORE_DECAY_SCRIPT = script;
    }

    /**
     * <h3>점수 감쇠 (스케쥴러)</h3>
     * <p>Redis Sorted Set의 모든 게시글 점수에 0.9를 곱하고, 임계값(1점) 이하의 게시글을 제거합니다.</p>
     * <p>FeaturedPostScheduler 스케줄러에서 5분마다 호출됩니다.</p>
     */
    public void applyRealtimePopularScoreDecay() {
        // 1. 모든 항목의 점수에 0.9 곱하기 (Lua 스크립트 사용)
        redisTemplate.execute(
                SCORE_DECAY_SCRIPT,
                List.of(REALTIME_SCORE_KEY),
                REALTIME_POST_SCORE_DECAY_RATE
        );

        // 2. 임계값(1점) 이하의 게시글 제거
        redisTemplate.opsForZSet().removeRangeByScore(REALTIME_SCORE_KEY, 0, REALTIME_POST_SCORE_THRESHOLD);
    }

    /**
     * <h3>캐시 삭제</h3>
     * <p>score:realtime Sorted Set에서 특정 postId를 삭제합니다.</p>
     * <p>게시글 삭제 시 실시간 인기글 점수 정리를 위해 호출됩니다.</p>
     *
     * @param postId 제거할 게시글 ID
     */
    public void removePostIdFromRealtimeScore(Long postId) {
        redisTemplate.opsForZSet().remove(REALTIME_SCORE_KEY, postId);
    }

    /**
     * <h3>점수 저장소 키 생성</h3>
     * <p>실시간 인기글 점수를 저장하기 위한 Redis Sorted Set 키를 생성합니다.</p>
     *
     * @param type 게시글 캐시 유형 (REALTIME만 지원)
     * @return 생성된 Redis 키 (형식: post:realtime:score)
     * @throws IllegalArgumentException REALTIME 이외의 타입이 전달된 경우
     */
    public static String getScoreStorageKey(PostCacheFlag type) {
        if (type != PostCacheFlag.REALTIME) {
            throw new IllegalArgumentException("점수 저장소는 REALTIME 타입만 지원합니다: " + type);
        }
        return POST_PREFIX + type.name().toLowerCase() + ":score";
    }


}
