package jaeik.growfarm.infrastructure.adapter.notification.in.listener.handler;

import jaeik.growfarm.domain.auth.event.UserLoggedOutEvent;
import jaeik.growfarm.domain.notification.application.port.out.SsePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * <h2>SSE 연결 정리 이벤트 핸들러 테스트</h2>
 * <p>SseEmitterCleanupEventHandler의 단위 테스트</p>
 * <p>사용자 로그아웃 시 SSE 연결 정리 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SSE 연결 정리 이벤트 핸들러 테스트")
class SseEmitterCleanupEventHandlerTest {

    @Mock
    private SsePort ssePort;

    @InjectMocks
    private SseEmitterCleanupEventHandler sseEmitterCleanupEventHandler;

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - SSE 연결 정리")
    void handleUserLoggedOutEvent_ShouldCleanupSseConnections() {
        // Given
        Long userId = 1L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteAllEmitterByUserId(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 다양한 사용자 ID")
    void handleUserLoggedOutEvent_WithVariousUserIds_ShouldCleanupCorrectly() {
        // Given
        Long userId = 999L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteAllEmitterByUserId(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - null 사용자 ID")
    void handleUserLoggedOutEvent_WithNullUserId_ShouldCallCleanup() {
        // Given
        Long userId = null;
        UserLoggedOutEvent event = new UserLoggedOutEvent(userId, 1L, java.time.LocalDateTime.now());

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteAllEmitterByUserId(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - SSE 정리 실패 시 예외 로깅만 하고 계속 진행")
    void handleUserLoggedOutEvent_WhenCleanupFails_ShouldLogErrorAndContinue() {
        // Given
        Long userId = 1L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);
        
        RuntimeException cleanupException = new RuntimeException("SSE 정리 실패");
        doThrow(cleanupException).when(ssePort).deleteAllEmitterByUserId(userId);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteAllEmitterByUserId(eq(userId));
        // 예외가 발생해도 메서드는 정상적으로 종료되어야 함 (비동기 처리)
        // 실제로는 로그만 남기고 예외를 전파하지 않음
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 이벤트 데이터 검증")
    void handleUserLoggedOutEvent_EventDataValidation() {
        // Given
        Long userId = 123L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);
        
        // 이벤트 데이터 검증
        assert event.userId().equals(userId);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteAllEmitterByUserId(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 0인 사용자 ID")
    void handleUserLoggedOutEvent_WithZeroUserId_ShouldCleanup() {
        // Given
        Long userId = 0L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteAllEmitterByUserId(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 음수 사용자 ID")
    void handleUserLoggedOutEvent_WithNegativeUserId_ShouldCleanup() {
        // Given
        Long userId = -1L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteAllEmitterByUserId(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 매우 큰 사용자 ID")
    void handleUserLoggedOutEvent_WithLargeUserId_ShouldCleanup() {
        // Given
        Long userId = Long.MAX_VALUE;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteAllEmitterByUserId(eq(userId));
    }
}