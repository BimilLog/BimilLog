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
    @DisplayName("사용자 로그아웃 이벤트 처리 - 특정 기기 SSE 연결 정리")
    void handleUserLoggedOutEvent_ShouldCleanupSpecificDeviceSseConnections() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 다양한 사용자 ID와 토큰 ID")
    void handleUserLoggedOutEvent_WithVariousUserIds_ShouldCleanupCorrectly() {
        // Given
        Long userId = 999L;
        Long tokenId = 200L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - null 사용자 ID")
    void handleUserLoggedOutEvent_WithNullUserId_ShouldCallCleanup() {
        // Given
        Long userId = null;
        Long tokenId = 1L;
        UserLoggedOutEvent event = new UserLoggedOutEvent(userId, tokenId, java.time.LocalDateTime.now());

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - SSE 정리 실패 시 예외 로깅만 하고 계속 진행")
    void handleUserLoggedOutEvent_WhenCleanupFails_ShouldLogErrorAndContinue() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);
        
        RuntimeException cleanupException = new RuntimeException("SSE 정리 실패");
        doThrow(cleanupException).when(ssePort).deleteEmitterByUserIdAndTokenId(userId, tokenId);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
        // 예외가 발생해도 메서드는 정상적으로 종료되어야 함 (비동기 처리)
        // 실제로는 로그만 남기고 예외를 전파하지 않음
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 이벤트 데이터 검증")
    void handleUserLoggedOutEvent_EventDataValidation() {
        // Given
        Long userId = 123L;
        Long tokenId = 456L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);
        
        // 이벤트 데이터 검증
        assert event.userId().equals(userId);
        assert event.tokenId().equals(tokenId);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 0인 사용자 ID")
    void handleUserLoggedOutEvent_WithZeroUserId_ShouldCleanup() {
        // Given
        Long userId = 0L;
        Long tokenId = 1L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 음수 사용자 ID")
    void handleUserLoggedOutEvent_WithNegativeUserId_ShouldCleanup() {
        // Given
        Long userId = -1L;
        Long tokenId = 1L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 매우 큰 사용자 ID")
    void handleUserLoggedOutEvent_WithLargeUserId_ShouldCleanup() {
        // Given
        Long userId = Long.MAX_VALUE;
        Long tokenId = Long.MAX_VALUE;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);

        // When
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(event);

        // Then
        verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 다중 기기 시나리오 검증")
    void handleUserLoggedOutEvent_MultiDeviceScenario() {
        // Given
        Long userId = 100L;
        Long tokenId1 = 201L;  // A 기기
        Long tokenId2 = 202L;  // B 기기
        
        UserLoggedOutEvent eventA = UserLoggedOutEvent.of(userId, tokenId1);
        UserLoggedOutEvent eventB = UserLoggedOutEvent.of(userId, tokenId2);

        // When - A 기기만 로그아웃
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(eventA);

        // Then - A 기기의 토큰만 정리되어야 함
        verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId1));
        
        // When - B 기기도 로그아웃
        sseEmitterCleanupEventHandler.handleUserLoggedOutEvent(eventB);
        
        // Then - B 기기의 토큰도 정리되어야 함
        verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId2));
    }
}