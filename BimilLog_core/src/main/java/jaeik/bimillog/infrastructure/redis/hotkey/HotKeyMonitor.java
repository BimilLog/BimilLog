package jaeik.bimillog.infrastructure.redis.hotkey;

import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;

import java.lang.annotation.*;

/**
 * <h2>핫키 모니터링 애노테이션</h2>
 * <p>AOP를 통해 캐시 조회 메서드에서 핫키 접근을 자동 기록합니다.</p>
 * <p>메서드 실행 전에 해당 캐시 키의 접근 횟수를 10% 샘플링으로 기록합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 * @see HotKeyMonitorAspect
 * @see HotKeyAccessRecorder
 * @deprecated 2.7.0부터 사용 중단. 스케줄러 기반 캐시 갱신으로 대체.
 */
@Deprecated(since = "2.7.0", forRemoval = true)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HotKeyMonitor {

    /**
     * 모니터링할 캐시 유형
     *
     * @return 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     */
    PostCacheFlag value();
}
