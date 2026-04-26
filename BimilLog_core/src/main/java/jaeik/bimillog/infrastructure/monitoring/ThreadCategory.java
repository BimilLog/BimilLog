package jaeik.bimillog.infrastructure.monitoring;

/**
 * <h2>스레드 분류 카테고리</h2>
 * <p>부하테스트 분석 시 스레드 풀 병목 식별을 위한 분류 기준이다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
public enum ThreadCategory {
    /** 톰캣 풀 안에서 작업을 대기 중인 스레드 (= 여유). */
    TOMCAT_IDLE,
    /** 톰캣 풀에서 나와 요청을 처리 중인 스레드. Thread.State 라벨로 다시 세분화된다. */
    TOMCAT_BUSY,
    /** 비동기 풀(@Async ThreadPoolTaskExecutor) 스레드. 풀별 구분은 하지 않는다. */
    ASYNC,
    /** 위 어디에도 속하지 않는 시스템 스레드 (Hikari, Lettuce, ForkJoin, GC 등). */
    SYSTEM
}
