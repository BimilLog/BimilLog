package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.infrastructure.redis.hotkey.HotKeyAccessRecorder;
import jaeik.bimillog.infrastructure.redis.hotkey.HotKeyRedisAdapter;
import jaeik.bimillog.infrastructure.redis.hotkey.HotKeyTtlRegistry;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@Tag("unit")
@DisplayName("HotKeyAnalysisScheduler 단위 테스트")
class HotKeyAnalysisSchedulerTest extends BaseUnitTest {

    @Mock
    private HotKeyAccessRecorder hotKeyAccessRecorder;

    @Mock
    private HotKeyRedisAdapter hotKeyRedisAdapter;

    @Mock
    private HotKeyTtlRegistry hotKeyTtlRegistry;

    @InjectMocks
    private HotKeyAnalysisScheduler scheduler;

    @Test
    @DisplayName("로컬 카운트가 비어있으면 아무 작업도 하지 않는다")
    void shouldDoNothingWhenLocalCountsEmpty() {
        // given
        given(hotKeyAccessRecorder.swapAndGet()).willReturn(Collections.emptyMap());

        // when
        scheduler.analyzeAndRefresh();

        // then
        then(hotKeyRedisAdapter).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("임계값 미만 키만 있으면 ZSet 합산을 하지 않는다")
    void shouldSkipWhenNoHotKeysAboveThreshold() {
        // given
        given(hotKeyAccessRecorder.swapAndGet()).willReturn(Map.of("post:realtime:simple", 50L));

        // when
        scheduler.analyzeAndRefresh();

        // then
        then(hotKeyRedisAdapter).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("핫키가 있으면 ZSet에 합산하고 분산락을 시도한다")
    void shouldMergeToZSetAndTryLock() {
        // given
        Map<String, Long> localCounts = Map.of("post:realtime:simple", 200L);
        given(hotKeyAccessRecorder.swapAndGet()).willReturn(localCounts);
        given(hotKeyRedisAdapter.tryAcquireTtlRefreshLock()).willReturn(false);

        // when
        scheduler.analyzeAndRefresh();

        // then
        then(hotKeyRedisAdapter).should().mergeAccessCounts(Map.of("post:realtime:simple", 200L));
        then(hotKeyRedisAdapter).should().tryAcquireTtlRefreshLock();
        then(hotKeyRedisAdapter).should(never()).getAllHotKeys();
    }

    @Test
    @DisplayName("분산락 획득 성공 시 TTL을 갱신하고 ZSet을 삭제한다")
    void shouldRefreshTtlAndDeleteZSetWhenLockAcquired() {
        // given
        String cacheKey = "post:realtime:simple";
        Duration originalTtl = Duration.ofMinutes(1);

        given(hotKeyAccessRecorder.swapAndGet()).willReturn(Map.of(cacheKey, 200L));
        given(hotKeyRedisAdapter.tryAcquireTtlRefreshLock()).willReturn(true);
        given(hotKeyRedisAdapter.getAllHotKeys()).willReturn(Set.of((Object) cacheKey));
        given(hotKeyTtlRegistry.isRefreshable(cacheKey)).willReturn(true);
        given(hotKeyTtlRegistry.getOriginalTtl(cacheKey)).willReturn(originalTtl);

        // when
        scheduler.analyzeAndRefresh();

        // then
        then(hotKeyRedisAdapter).should().mergeAccessCounts(Map.of(cacheKey, 200L));
        then(hotKeyRedisAdapter).should().refreshTtl(cacheKey, originalTtl);
        then(hotKeyRedisAdapter).should().deleteAccessZSet();
        then(hotKeyRedisAdapter).should().releaseTtlRefreshLock();
    }

    @Test
    @DisplayName("갱신 대상이 아닌 키는 TTL 갱신을 스킵한다")
    void shouldSkipNonRefreshableKeys() {
        // given
        String refreshableKey = "post:realtime:simple";
        String nonRefreshableKey = "paper:realtime:score";

        given(hotKeyAccessRecorder.swapAndGet()).willReturn(
                Map.of(refreshableKey, 200L, nonRefreshableKey, 300L));
        given(hotKeyRedisAdapter.tryAcquireTtlRefreshLock()).willReturn(true);
        given(hotKeyRedisAdapter.getAllHotKeys()).willReturn(
                Set.of((Object) refreshableKey, (Object) nonRefreshableKey));
        given(hotKeyTtlRegistry.isRefreshable(refreshableKey)).willReturn(true);
        given(hotKeyTtlRegistry.isRefreshable(nonRefreshableKey)).willReturn(false);
        given(hotKeyTtlRegistry.getOriginalTtl(refreshableKey)).willReturn(Duration.ofMinutes(1));

        // when
        scheduler.analyzeAndRefresh();

        // then
        then(hotKeyRedisAdapter).should().refreshTtl(eq(refreshableKey), any(Duration.class));
        then(hotKeyRedisAdapter).should(never()).refreshTtl(eq(nonRefreshableKey), any(Duration.class));
    }

    @Test
    @DisplayName("TTL 갱신 중 예외 발생 시에도 분산락을 해제한다")
    void shouldReleaseLockEvenWhenExceptionOccurs() {
        // given
        given(hotKeyAccessRecorder.swapAndGet()).willReturn(Map.of("post:realtime:simple", 200L));
        given(hotKeyRedisAdapter.tryAcquireTtlRefreshLock()).willReturn(true);
        given(hotKeyRedisAdapter.getAllHotKeys()).willThrow(new RuntimeException("Redis 장애"));

        // when
        try {
            scheduler.analyzeAndRefresh();
        } catch (Exception ignored) {
        }

        // then
        then(hotKeyRedisAdapter).should().releaseTtlRefreshLock();
    }

    @Test
    @DisplayName("ZSet 합산 실패 시 분산락 획득을 시도하지 않는다")
    void shouldNotTryLockWhenMergeFails() {
        // given
        given(hotKeyAccessRecorder.swapAndGet()).willReturn(Map.of("post:realtime:simple", 200L));
        org.mockito.BDDMockito.willThrow(new RuntimeException("Redis 연결 실패"))
                .given(hotKeyRedisAdapter).mergeAccessCounts(anyMap());

        // when
        scheduler.analyzeAndRefresh();

        // then
        then(hotKeyRedisAdapter).should(never()).tryAcquireTtlRefreshLock();
    }

    @Test
    @DisplayName("분산락 획득 실패 시 TTL 갱신을 스킵한다")
    void shouldSkipRefreshWhenLockNotAcquired() {
        // given
        given(hotKeyAccessRecorder.swapAndGet()).willReturn(Map.of("post:realtime:simple", 200L));
        given(hotKeyRedisAdapter.tryAcquireTtlRefreshLock()).willReturn(false);

        // when
        scheduler.analyzeAndRefresh();

        // then
        then(hotKeyRedisAdapter).should(never()).getAllHotKeys();
        then(hotKeyRedisAdapter).should(never()).refreshTtl(anyString(), any(Duration.class));
        then(hotKeyRedisAdapter).should(never()).deleteAccessZSet();
        then(hotKeyRedisAdapter).should(never()).releaseTtlRefreshLock();
    }
}
