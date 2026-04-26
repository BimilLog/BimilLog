package jaeik.bimillog.infrastructure.monitoring;

/**
 * <h2>스레드 분류 카테고리</h2>
 * <p>부하테스트 분석 시 스레드 종류를 구분하기 위한 분류 기준이다.</p>
 * <p>톰캣 풀의 idle/busy 구분은 Spring Boot Actuator의 {@code tomcat_threads_*} 메트릭으로 위임하고,
 * 본 분류기는 톰캣/비동기/시스템 카테고리만 구분한다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
public enum ThreadCategory {
    /** 톰캣 워커 스레드 ({@code http-nio-} prefix). 풀 안/밖 구분은 Actuator 메트릭으로 확인한다. */
    TOMCAT,
    /** 비동기 풀(@Async ThreadPoolTaskExecutor) 스레드. 풀별 구분은 하지 않는다. */
    ASYNC,
    /** 위 어디에도 속하지 않는 시스템 스레드 (Hikari, Lettuce, ForkJoin, GC 등). */
    SYSTEM
}
