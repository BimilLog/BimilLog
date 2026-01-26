package jaeik.bimillog.infrastructure.redis.hotkey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("HotKeyAccessRecorder 단위 테스트")
class HotKeyAccessRecorderTest {

    private HotKeyAccessRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new HotKeyAccessRecorder();
    }

    @Test
    @DisplayName("10% 샘플링으로 접근 횟수를 기록한다")
    void shouldRecordAccessWithSampling() {
        // given
        String cacheKey = "post:realtime:simple";
        int totalCalls = 10_000;

        // when
        for (int i = 0; i < totalCalls; i++) {
            recorder.recordAccess(cacheKey);
        }

        // then
        Map<String, Long> counts = recorder.swapAndGet();
        long recorded = counts.getOrDefault(cacheKey, 0L);

        // 10% 샘플링이므로 대략 1000 근처 (오차 허용: 500~1500)
        assertThat(recorded).isBetween(500L, 1500L);
    }

    @Test
    @DisplayName("swapAndGet 호출 시 이전 맵을 반환하고 새 맵으로 교체된다")
    void shouldSwapAndReturnPreviousMap() {
        // given
        String cacheKey = "post:realtime:simple";
        for (int i = 0; i < 10_000; i++) {
            recorder.recordAccess(cacheKey);
        }

        // when
        Map<String, Long> firstSwap = recorder.swapAndGet();
        Map<String, Long> secondSwap = recorder.swapAndGet();

        // then
        assertThat(firstSwap).containsKey(cacheKey);
        assertThat(firstSwap.get(cacheKey)).isPositive();
        assertThat(secondSwap).isEmpty();
    }

    @Test
    @DisplayName("여러 캐시 키를 독립적으로 기록한다")
    void shouldRecordMultipleKeysIndependently() {
        // given
        String key1 = "post:realtime:simple";
        String key2 = "post:weekly:simple";

        for (int i = 0; i < 10_000; i++) {
            recorder.recordAccess(key1);
            recorder.recordAccess(key2);
        }

        // when
        Map<String, Long> counts = recorder.swapAndGet();

        // then
        assertThat(counts).containsKeys(key1, key2);
        assertThat(counts.get(key1)).isPositive();
        assertThat(counts.get(key2)).isPositive();
    }

    @Test
    @DisplayName("동시 접근 시 데이터 손실 없이 기록한다")
    void shouldHandleConcurrentAccess() throws InterruptedException {
        // given
        String cacheKey = "post:realtime:simple";
        int threadCount = 10;
        int callsPerThread = 10_000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                for (int i = 0; i < callsPerThread; i++) {
                    recorder.recordAccess(cacheKey);
                }
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();

        // then
        Map<String, Long> counts = recorder.swapAndGet();
        long recorded = counts.getOrDefault(cacheKey, 0L);

        // 총 100,000회 호출, 10% 샘플링 → 약 10,000 (넓은 오차 허용)
        assertThat(recorded).isBetween(5_000L, 15_000L);
    }

    @Test
    @DisplayName("기록이 없으면 빈 맵을 반환한다")
    void shouldReturnEmptyMapWhenNoRecords() {
        // when
        Map<String, Long> counts = recorder.swapAndGet();

        // then
        assertThat(counts).isEmpty();
    }
}
