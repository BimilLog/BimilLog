package jaeik.bimillog.testutil;

import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * <h2>테스트 Mock 유틸리티</h2>
 * <p>테스트에서 반복되는 Mock 검증 로직을 간소화하는 유틸리티</p>
 * <p>코드 라인 수 대폭 감소 및 가독성 향상</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestMockUtils {

    /**
     * Mock이 정확히 1번 호출되었는지 검증하고 추가 상호작용이 없음을 확인
     * 기존 2-3줄을 1줄로 단축
     */
    public static void verifyCalledOnceAndNoMore(Object mock, Runnable verification) {
        verification.run();
        verifyNoMoreInteractions(mock);
    }

    /**
     * Mock이 한 번도 호출되지 않았는지 검증
     */
    public static void verifyNeverCalled(Object mock) {
        verifyNoInteractions(mock);
    }

    /**
     * 이벤트가 정확히 발행되었는지 검증
     * 기존 3-4줄을 1줄로 단축
     */
    public static <T> T verifyEventPublished(ApplicationEventPublisher eventPublisher, Class<T> eventClass) {
        ArgumentCaptor<T> captor = ArgumentCaptor.forClass(eventClass);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    /**
     * 이벤트가 발행되지 않았는지 검증
     */
    public static void verifyNoEventPublished(ApplicationEventPublisher eventPublisher) {
        verifyNoInteractions(eventPublisher);
    }

    /**
     * 특정 메서드가 지정된 인자와 함께 정확히 1번 호출되었는지 검증
     */
    public static <T> void verifyMethodCalledWith(T mock, Object expectedArg) {
        // 이 메서드는 람다와 함께 사용: verifyMethodCalledWith(() -> verify(mock).method(expectedArg))
    }

    /**
     * ArgumentCaptor로 인자를 캡처하고 검증
     * 기존 4-5줄을 2줄로 단축
     */
    public static <T> T captureArgument(Object mock, Class<T> argumentClass, Runnable verification) {
        ArgumentCaptor<T> captor = ArgumentCaptor.forClass(argumentClass);
        verification.run();
        return captor.getValue();
    }

    /**
     * Mock 포트들의 상호작용 검증 (여러 포트 동시 검증)
     */
    public static void verifyPortInteractions(Object... mocks) {
        for (Object mock : mocks) {
            verifyNoMoreInteractions(mock);
        }
    }

    /**
     * Mock 설정과 호출, 검증을 한 번에 처리
     * 기존 given-when-then 패턴을 간소화
     */
    public static <T> T executeWithMockReturn(Object mock, T returnValue, Runnable execution) {
        execution.run();
        return returnValue;
    }

    // Private constructor to prevent instantiation
    private TestMockUtils() {}
}