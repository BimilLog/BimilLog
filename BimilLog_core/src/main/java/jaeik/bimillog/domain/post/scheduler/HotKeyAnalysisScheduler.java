package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.hotkey.HotKeyAccessRecorder;
import jaeik.bimillog.infrastructure.redis.hotkey.HotKeyRedisAdapter;
import jaeik.bimillog.infrastructure.redis.hotkey.HotKeyTtlRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.redis.hotkey.RedisHotKeyKeys.HOTKEY_THRESHOLD;

/**
 * <h2>핫키 분석 스케줄러</h2>
 * <p>30초마다 로컬 샘플링 카운트를 분석하여 핫키를 판별하고 TTL을 갱신합니다.</p>
 * <p>cron 기반 실행으로 여러 인스턴스가 동시에 시작하여 ZSet 분산 합산의 정확도를 높입니다.</p>
 * <p>ZINCRBY는 락 없이 수행하고, TTL 갱신에만 분산락을 걸어 한 인스턴스만 갱신합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 * @deprecated 2.7.0부터 사용 중단. 스케줄러 기반 캐시 갱신으로 대체.
 */
@Deprecated(since = "2.7.0", forRemoval = true)
@Log(logResult = false, logExecutionTime = true, message = "핫키 분석")
@Service
@RequiredArgsConstructor
@Slf4j
public class HotKeyAnalysisScheduler {

    private final HotKeyAccessRecorder hotKeyAccessRecorder;
    private final HotKeyRedisAdapter hotKeyRedisAdapter;
    private final HotKeyTtlRegistry hotKeyTtlRegistry;

    /**
     * <h3>핫키 분석 및 TTL 갱신</h3>
     * <p>30초마다 실행되며, 로컬 샘플링 카운트에서 핫키를 추출하고
     * Redis ZSet에 분산 합산한 뒤, 분산락을 획득한 인스턴스가 TTL을 갱신합니다.</p>
     */
    @Scheduled(cron = "0/30 * * * * *")
    public void analyzeAndRefresh() {
        // 1. 더블 버퍼링 스왑 - 이전 주기의 카운트를 가져옴
        Map<String, Long> localCounts = hotKeyAccessRecorder.swapAndGet();
        if (localCounts.isEmpty()) {
            return;
        }

        // 2. 임계값 이상인 키만 필터 (핫키 후보)
        Map<String, Long> hotKeys = localCounts.entrySet().stream()
                .filter(e -> e.getValue() >= HOTKEY_THRESHOLD)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (hotKeys.isEmpty()) {
            return;
        }

        log.info("[HOTKEY] 핫키 후보 {}개 감지: {}", hotKeys.size(), hotKeys);

        // 3. Redis ZSet에 분산 합산 (ZINCRBY, 락 없음)
        try {
            hotKeyRedisAdapter.mergeAccessCounts(hotKeys);
        } catch (Exception e) {
            log.warn("[HOTKEY] ZSet 합산 실패: {}", e.getMessage());
            return;
        }

        // 4. 분산락 획득 시도 (TTL 갱신은 한 인스턴스만)
        if (!hotKeyRedisAdapter.tryAcquireTtlRefreshLock()) {
            log.debug("[HOTKEY] TTL 갱신 락 획득 실패 - 다른 인스턴스가 갱신 중");
            return;
        }

        try {
            // 5. ZSet에서 모든 핫키 읽기
            Set<Object> allHotKeys = hotKeyRedisAdapter.getAllHotKeys();
            if (allHotKeys == null || allHotKeys.isEmpty()) {
                return;
            }

            // 6. 각 핫키의 원래 TTL로 EXPIRE 갱신
            int refreshedCount = 0;
            for (Object keyObj : allHotKeys) {
                String cacheKey = keyObj.toString();
                if (!hotKeyTtlRegistry.isRefreshable(cacheKey)) {
                    continue;
                }
                Duration originalTtl = hotKeyTtlRegistry.getOriginalTtl(cacheKey);
                hotKeyRedisAdapter.refreshTtl(cacheKey, originalTtl);
                refreshedCount++;
            }

            // 7. ZSet 삭제 (갱신 완료)
            hotKeyRedisAdapter.deleteAccessZSet();
            log.info("[HOTKEY] TTL 갱신 완료: {}개 키", refreshedCount);
        } finally {
            hotKeyRedisAdapter.releaseTtlRefreshLock();
        }
    }
}
