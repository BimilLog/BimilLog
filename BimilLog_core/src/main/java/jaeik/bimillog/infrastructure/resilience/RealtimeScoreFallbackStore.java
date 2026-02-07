package jaeik.bimillog.infrastructure.resilience;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * <h2>실시간 인기글 폴백 저장소</h2>
 * <p>Redis 장애 시 실시간 인기글 점수를 Caffeine 캐시에 임시 저장합니다.</p>
 * <p>서킷브레이커 OPEN 상태에서 fallbackMethod로 호출됩니다.</p>
 * <p>실시간 특성상 Redis 복구 시 이전 데이터 복구 없이 새로 시작합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@Slf4j
public class RealtimeScoreFallbackStore {
    private static final int MAX_SIZE = 10_000;
    private static final double DECAY_RATE = 0.9;
    private static final double SCORE_THRESHOLD = 1.0;

    private final Cache<Long, Double> scoreCache = Caffeine.newBuilder()
            .maximumSize(MAX_SIZE)
            .build();

    /**
     * <h3>점수 증가</h3>
     * <p>fallbackMethod에서 호출됩니다.</p>
     * <p>Redis 장애 시 임시로 점수를 메모리에 저장합니다.</p>
     *
     * @param postId 게시글 ID
     * @param score  증가시킬 점수
     */
    public void incrementScore(Long postId, double score) {
        scoreCache.asMap().merge(postId, score, Double::sum);
        log.debug("[FALLBACK_STORE] 점수 저장: postId={}, score={}", postId, score);
    }

    /**
     * <h3>특정 범위 내 postId 조회</h3>
     * <p>점수 내림차순으로 특정 범위 내 게시글 ID를 반환합니다.</p>
     *
     * @param start 시작지점
     * @param end   조회 개수
     * @return 점수 내림차순 정렬된 게시글 ID 목록
     */
    public List<Long> getTopPostIds(long start, long end) {
        return scoreCache.asMap().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .skip(start)
                .limit(end)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * <h3>점수 지수 감쇠 적용</h3>
     * <p>모든 게시글 점수에 DECAY_RATE(0.9)를 곱하고, 임계값(1점) 이하의 게시글을 제거합니다.</p>
     * <p>FeaturedPostScheduler 스케줄러에서 주기적으로 호출됩니다.</p>
     */
    public void applyDecay() {
        scoreCache.asMap().entrySet().removeIf(entry -> {
            double newScore = entry.getValue() * DECAY_RATE;
            if (newScore < SCORE_THRESHOLD) {
                return true;
            }
            entry.setValue(newScore);
            return false;
        });
        log.debug("[FALLBACK_STORE] 지수 감쇠 적용 완료. 현재 항목 수: {}", scoreCache.estimatedSize());
    }

    /**
     * <h3>저장된 게시글 개수 조회</h3>
     *
     * @return 저장된 게시글 개수
     */
    public long size() {
        return scoreCache.estimatedSize();
    }

    /**
     * <h3>데이터 존재 여부 확인</h3>
     *
     * @return 데이터가 있으면 true
     */
    public boolean hasData() {
        return scoreCache.estimatedSize() > 0;
    }

    /**
     * <h3>저장소 초기화</h3>
     * <p>Redis 복구 후 또는 테스트 시 저장소를 초기화합니다.</p>
     */
    public void clear() {
        scoreCache.invalidateAll();
        log.info("[FALLBACK_STORE] 저장소 초기화");
    }
}
