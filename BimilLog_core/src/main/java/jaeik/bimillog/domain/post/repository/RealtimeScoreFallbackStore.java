package jaeik.bimillog.domain.post.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>실시간 인기글 폴백 저장소</h2>
 * <p>Redis 장애 시 실시간 인기글 점수를 Caffeine 캐시에 임시 저장합니다.</p>
 * <p>서킷브레이커 OPEN 상태에서 fallbackMethod로 호출됩니다.</p>
 * <p>Redis 복구(CLOSED 전환) 시 누적 점수와 삭제 로그를 Redis에 반영한 뒤 초기화합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@Slf4j
public class RealtimeScoreFallbackStore {
    private static final int MAX_SIZE = 10_000;
    private static final double DECAY_RATE = 0.97;
    private static final double SCORE_THRESHOLD = 1.0;

    private final Cache<Long, Double> scoreCache = Caffeine.newBuilder()
            .maximumSize(MAX_SIZE)
            .build();

    // OPEN 구간 중 삭제된 게시글 ID 추적 (CLOSED 전환 시 Redis에서도 제거)
    private final Set<Long> deletedPostIds = ConcurrentHashMap.newKeySet();

    // 웜업 기준점 (CLOSED 전환 시 증분 계산용)
    private final Map<Long, Double> baseline = new HashMap<>();

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
     * <p>PostCacheScheduler 스케줄러에서 주기적으로 호출됩니다.</p>
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
     * <h3>게시글 제거</h3>
     * <p>삭제된 게시글을 폴백 저장소에서 제거합니다.</p>
     *
     * @param postId 제거할 게시글 ID
     */
    public void removePost(Long postId) {
        scoreCache.invalidate(postId);
        deletedPostIds.add(postId);
        log.debug("[FALLBACK_STORE] 게시글 제거 및 삭제 로그 기록: postId={}", postId);
    }

    /**
     * <h3>Redis 점수로 웜업</h3>
     * <p>스케줄러가 주기적으로 Redis Top N 점수를 Caffeine에 반영합니다.</p>
     * <p>서킷 CLOSED 상태일 때만 호출되어 Caffeine이 항상 최신 상태를 유지합니다.</p>
     *
     * @param redisScores Redis ZSet에서 가져온 postId → score 맵
     */
    public void warmUp(Map<Long, Double> redisScores) {
        scoreCache.invalidateAll();
        baseline.clear();
        redisScores.forEach((postId, score) -> {
            scoreCache.put(postId, score);
            baseline.put(postId, score);
        });
        log.debug("[FALLBACK_STORE] Redis Top{} 웜업 완료", redisScores.size());
    }

    /**
     * <h3>삭제 로그 조회</h3>
     * <p>CLOSED 전환 시 Redis에서 제거할 게시글 ID 목록을 반환합니다.</p>
     *
     * @return OPEN 구간 중 삭제된 게시글 ID 불변 복사본
     */
    public Set<Long> getDeletedPostIds() {
        return Set.copyOf(deletedPostIds);
    }

    /**
     * <h3>OPEN 구간 증분만 조회</h3>
     * <p>CLOSED 전환 시 Redis에 ZINCRBY할 증분 점수만 반환합니다.</p>
     * <p>웜업 기준점(baseline)을 빼서 OPEN 구간에 실제 적립된 점수만 계산합니다.</p>
     * <p>웜업 이후 새로 유입된 게시글(baseline에 없는)은 전체 점수가 증분입니다.</p>
     *
     * @return postId → 증분 점수 (양수/음수 모두 포함, 감쇠 보상 포함)
     */
    public Map<Long, Double> getDeltaScores() {
        Map<Long, Double> delta = new HashMap<>();
        scoreCache.asMap().forEach((postId, totalScore) -> {
            double base = baseline.getOrDefault(postId, 0.0);
            double diff = totalScore - base;
            if (diff != 0) {
                delta.put(postId, diff);
            }
        });
        return delta;
    }

    /**
     * <h3>동기화 완료된 점수 항목 제거</h3>
     * <p>배치 단위로 Redis 동기화 성공 후 해당 항목만 Caffeine에서 제거합니다.</p>
     *
     * @param postIds Redis에 성공적으로 반영된 게시글 ID 목록
     */
    public void removeSyncedScores(Collection<Long> postIds) {
        scoreCache.invalidateAll(postIds);
        log.debug("[FALLBACK_STORE] 동기화 완료 점수 제거: {}건", postIds.size());
    }

    /**
     * <h3>동기화 완료된 삭제 로그 제거</h3>
     * <p>배치 단위로 Redis 삭제 재처리 성공 후 해당 항목만 삭제 로그에서 제거합니다.</p>
     *
     * @param postIds Redis에서 성공적으로 제거된 게시글 ID 목록
     */
    public void removeSyncedDeletedPostIds(Collection<Long> postIds) {
        deletedPostIds.removeAll(postIds);
        log.debug("[FALLBACK_STORE] 동기화 완료 삭제 로그 제거: {}건", postIds.size());
    }

}
