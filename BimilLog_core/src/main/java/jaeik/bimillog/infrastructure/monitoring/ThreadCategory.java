package jaeik.bimillog.infrastructure.monitoring;

/**
 * <h2>스레드 분류 카테고리</h2>
 * <p>부하테스트 분석 시 스레드 종류 + 톰캣 풀 안/밖 상태를 구분하기 위한 분류 기준이다.</p>
 * <p>톰캣 풀 카운트 자체는 Spring Boot Actuator의 {@code tomcat_threads_*} 메트릭이 정확하지만,
 * 본 분류기는 idle/busy별로 Thread.State 분해를 노출하기 위해 별도로 분류한다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
public enum ThreadCategory {
    /** 톰캣 워커 스레드 중 풀 안에서 다음 작업을 대기 중인 스레드. */
    TOMCAT_IDLE,
    /** 톰캣 워커 스레드 중 요청을 처리 중인 스레드. */
    TOMCAT_BUSY,
    /** 비동기 풀(@Async ThreadPoolTaskExecutor) 스레드. 풀별 구분은 하지 않는다. */
    ASYNC,
    /**
     * 위 어디에도 속하지 않는 시스템 스레드.
     * <p>Hikari, Lettuce, ForkJoin, GC뿐 아니라 톰캣 NIO 인프라
     * (Acceptor, ClientPoller 등 워커가 아닌 {@code http-nio-} 스레드)도 여기에 포함된다.</p>
     */
    SYSTEM
}
