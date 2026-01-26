package jaeik.bimillog.infrastructure.redis.hotkey;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.redis.hotkey.RedisHotKeyKeys.SAMPLING_DENOMINATOR;

/**
 * <h2>핫키 접근 기록기</h2>
 * <p>캐시 키 접근 시 10% 확률로 샘플링하여 ConcurrentHashMap에 카운트합니다.</p>
 * <p>AtomicReference 더블 버퍼링으로 스케줄러 분석 중 데이터 손실을 방지합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
public class HotKeyAccessRecorder {

    private final AtomicReference<ConcurrentHashMap<String, LongAdder>> accessCounts =
            new AtomicReference<>(new ConcurrentHashMap<>());

    /**
     * <h3>캐시 키 접근 기록 (10% 샘플링)</h3>
     * <p>ThreadLocalRandom으로 10% 확률일 때만 카운트를 증가시킵니다.</p>
     *
     * @param cacheKey Redis 캐시 키 (예: "post:realtime:simple")
     */
    public void recordAccess(String cacheKey) {
        if (ThreadLocalRandom.current().nextInt(SAMPLING_DENOMINATOR) != 0) {
            return;
        }
        accessCounts.get().computeIfAbsent(cacheKey, k -> new LongAdder()).increment();
    }

    /**
     * <h3>더블 버퍼링 스왑</h3>
     * <p>현재 카운트 맵을 새 맵으로 교체하고 이전 맵의 스냅샷을 반환합니다.</p>
     * <p>스왑 이후 새 요청은 새 맵에 기록되므로 데이터 손실이 없습니다.</p>
     *
     * @return 이전 주기의 접근 카운트 맵 (key: 캐시 키, value: 샘플링된 접근 횟수)
     */
    public Map<String, Long> swapAndGet() {
        ConcurrentHashMap<String, LongAdder> previous = accessCounts.getAndSet(new ConcurrentHashMap<>());
        return previous.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()));
    }
}
