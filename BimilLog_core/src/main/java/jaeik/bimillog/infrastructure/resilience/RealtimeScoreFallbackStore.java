package jaeik.bimillog.infrastructure.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * <h2>실시간 인기글 폴백 저장소</h2>
 * <p>Redis 장애 시 실시간 인기글 점수를 ConcurrentHashMap에 임시 저장합니다.</p>
 * <p>서킷브레이커 OPEN 상태에서 fallbackMethod로 호출됩니다.</p>
 * <p>실시간 특성상 Redis 복구 시 이전 데이터 복구 없이 새로 시작합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.1
 */
@Component
@Slf4j
public class RealtimeScoreFallbackStore {
    private final ConcurrentHashMap<Long, DoubleAdder> scoreMap = new ConcurrentHashMap<>();

    /**
     * <h3>점수 증가</h3>
     * <p>fallbackMethod에서 호출됩니다.</p>
     * <p>Redis 장애 시 임시로 점수를 메모리에 저장합니다.</p>
     *
     * @param postId 게시글 ID
     * @param score  증가시킬 점수
     */
    public void incrementScore(Long postId, double score) {
        scoreMap.computeIfAbsent(postId, k -> new DoubleAdder()).add(score);
        log.debug("[FALLBACK_STORE] 점수 저장: postId={}, score={}", postId, score);
    }

    /**
     * <h3>특정 범위 내 postId 조회</h3>
     * <p>점수 내림차순으로 특정 범위 내 게시글 ID를 반환합니다.</p>
     *
     * @param start 시작지점
     * @param end 조회 개수
     * @return 점수 내림차순 정렬된 게시글 ID 목록
     */
    public List<Long> getTopPostIds(long start, long end) {
        return scoreMap.entrySet().stream()
                .filter(e -> e.getValue().sum() > 0)
                .sorted(Map.Entry.<Long, DoubleAdder>comparingByValue(
                        Comparator.comparingDouble(DoubleAdder::sum)).reversed())
                .skip(start)
                .limit(end)
                .map(Map.Entry::getKey)
                .toList();
    }


    /**
     * <h3>저장된 게시글 개수 조회</h3>
     *
     * @return 저장된 게시글 개수
     */
    public int size() {
        return scoreMap.size();
    }

    /**
     * <h3>데이터 존재 여부 확인</h3>
     *
     * @return 데이터가 있으면 true
     */
    public boolean hasData() {
        return !scoreMap.isEmpty();
    }

    /**
     * <h3>저장소 초기화</h3>
     * <p>Redis 복구 후 또는 테스트 시 저장소를 초기화합니다.</p>
     */
    public void clear() {
        scoreMap.clear();
        log.info("[FALLBACK_STORE] 저장소 초기화");
    }
}
