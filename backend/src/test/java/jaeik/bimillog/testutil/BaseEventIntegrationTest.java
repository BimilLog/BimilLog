package jaeik.bimillog.testutil;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * <h2>이벤트 통합 테스트 베이스 클래스</h2>
 * <p>모든 Event 관련 통합 테스트가 상속받아 사용하는 기본 클래스</p>
 * <p>ApplicationEventPublisher, Awaitility 설정 및 헬퍼 메서드를 자동으로 제공</p>
 *
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>ApplicationEventPublisher 자동 주입</li>
 *   <li>비동기 이벤트 검증을 위한 Awaitility 헬퍼 메서드</li>
 *   <li>다중 이벤트 발행 유틸리티</li>
 *   <li>공통 타임아웃 설정</li>
 * </ul>
 *
 * <h3>사용 예시:</h3>
 * <pre>
 * class CommentEventTest extends BaseEventIntegrationTest {
 *
 *     {@literal @}Test
 *     void test() {
 *         // 이벤트 발행
 *         publishEvent(event);
 *
 *         // 비동기 검증
 *         verifyAsync(() -> {
 *             verify(mockUseCase).doSomething();
 *         });
 *     }
 * }
 * </pre>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@SpringBootTest
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
public abstract class BaseEventIntegrationTest extends BaseIntegrationTest {

    /**
     * 이벤트 발행을 위한 ApplicationEventPublisher
     */
    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    /**
     * 기본 비동기 처리 타임아웃 (5초)
     */
    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    /**
     * 빠른 비동기 처리 타임아웃 (3초)
     */
    protected static final Duration FAST_TIMEOUT = Duration.ofSeconds(3);

    /**
     * 느린 비동기 처리 타임아웃 (10초)
     */
    protected static final Duration SLOW_TIMEOUT = Duration.ofSeconds(10);

    /**
     * 각 테스트 메서드 실행 전 설정 초기화
     */
    @BeforeEach
    protected void setUpEventTest() {
        // 상위 클래스의 설정 실행
        super.setUpBase();

        // 하위 클래스의 추가 설정
        setUpChildEvent();
    }

    /**
     * 하위 클래스에서 추가 설정이 필요한 경우 오버라이드
     */
    protected void setUpChildEvent() {
        // 하위 클래스에서 필요시 오버라이드
    }

    /**
     * 단일 이벤트 발행
     * @param event 발행할 이벤트
     */
    protected void publishEvent(Object event) {
        eventPublisher.publishEvent(event);
    }

    /**
     * 다중 이벤트 동시 발행
     * @param events 발행할 이벤트 배열
     */
    protected void publishEvents(Object... events) {
        Arrays.stream(events).forEach(eventPublisher::publishEvent);
    }

    /**
     * 이벤트 리스트 발행
     * @param events 발행할 이벤트 리스트
     */
    protected void publishEvents(Iterable<?> events) {
        events.forEach(eventPublisher::publishEvent);
    }

    /**
     * 비동기 검증 (기본 타임아웃 사용)
     * @param verification 검증 로직
     */
    protected void verifyAsync(Runnable verification) {
        verifyAsync(verification, DEFAULT_TIMEOUT);
    }

    /**
     * 비동기 검증 (사용자 정의 타임아웃)
     * @param verification 검증 로직
     * @param timeout 타임아웃 설정
     */
    protected void verifyAsync(Runnable verification, Duration timeout) {
        Awaitility.await()
                .atMost(timeout)
                .untilAsserted(verification::run);
    }

    /**
     * 빠른 비동기 검증 (3초 타임아웃)
     * @param verification 검증 로직
     */
    protected void verifyAsyncFast(Runnable verification) {
        verifyAsync(verification, FAST_TIMEOUT);
    }

    /**
     * 느린 비동기 검증 (10초 타임아웃)
     * @param verification 검증 로직
     */
    protected void verifyAsyncSlow(Runnable verification) {
        verifyAsync(verification, SLOW_TIMEOUT);
    }



    /**
     * 이벤트 발행 후 즉시 비동기 검증
     * @param event 발행할 이벤트
     * @param verification 검증 로직
     */
    protected void publishAndVerify(Object event, Runnable verification) {
        publishEvent(event);
        verifyAsync(verification);
    }

    /**
     * 이벤트 발행 후 즉시 비동기 검증 (사용자 정의 타임아웃)
     * @param event 발행할 이벤트
     * @param verification 검증 로직
     * @param timeout 타임아웃 설정
     */
    protected void publishAndVerify(Object event, Runnable verification, Duration timeout) {
        publishEvent(event);
        verifyAsync(verification, timeout);
    }

    /**
     * 다중 이벤트 발행 후 비동기 검증
     * @param events 발행할 이벤트 배열
     * @param verification 검증 로직
     */
    protected void publishEventsAndVerify(Object[] events, Runnable verification) {
        publishEvents(events);
        verifyAsync(verification);
    }

    /**
     * 예외 발생을 기대하는 이벤트 테스트
     * @param event 발행할 이벤트
     * @param verification 예외 검증 로직
     */
    protected void publishAndExpectException(Object event, Runnable verification) {
        publishEvent(event);
        // 예외가 발생해도 리스너는 호출되어야 함
        verifyAsync(verification);
    }
}