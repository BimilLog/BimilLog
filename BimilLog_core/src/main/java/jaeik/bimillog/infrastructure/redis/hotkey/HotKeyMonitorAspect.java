package jaeik.bimillog.infrastructure.redis.hotkey;

import jaeik.bimillog.infrastructure.redis.post.RedisPostKeys;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * <h2>핫키 모니터링 AOP</h2>
 * <p>@HotKeyMonitor 애노테이션이 붙은 메서드 실행 전에 캐시 키 접근을 기록합니다.</p>
 * <p>10% 샘플링으로 ConcurrentHashMap에 카운트하여 핫키 판별에 사용됩니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 * @see HotKeyMonitor
 * @see HotKeyAccessRecorder
 * @deprecated 2.7.0부터 사용 중단. 스케줄러 기반 캐시 갱신으로 대체.
 */
@Deprecated(since = "2.7.0", forRemoval = true)
@Aspect
@Component
@RequiredArgsConstructor
public class HotKeyMonitorAspect {

    private final HotKeyAccessRecorder hotKeyAccessRecorder;

    /**
     * <h3>캐시 키 접근 기록</h3>
     * <p>@HotKeyMonitor 애노테이션의 value로 지정된 PostCacheFlag에 해당하는 캐시 키를 기록합니다.</p>
     *
     * @param hotKeyMonitor 핫키 모니터 애노테이션
     */
    @Before("@annotation(hotKeyMonitor)")
    public void recordAccess(HotKeyMonitor hotKeyMonitor) {
        hotKeyAccessRecorder.recordAccess(
                RedisPostKeys.getSimplePostHashKey(hotKeyMonitor.value())
        );
    }
}
