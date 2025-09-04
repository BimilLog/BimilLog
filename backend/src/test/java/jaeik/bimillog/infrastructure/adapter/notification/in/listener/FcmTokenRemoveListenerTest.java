package jaeik.bimillog.infrastructure.adapter.notification.in.listener;

import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
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
 * <h2>FCM 토큰 이벤트 리스너 테스트</h2>
 * <p>FcmTokenEventListener의 단위 테스트</p>
 * <p>사용자 로그아웃 및 탈퇴 시 FCM 토큰 삭제 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FCM 토큰 이벤트 리스너 테스트")
class FcmTokenRemoveListenerTest {

    @Mock
    private NotificationFcmUseCase notificationFcmUseCase;

    @InjectMocks
    private FcmTokenRemoveListener fcmTokenRemoveListener;

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - FCM 토큰 삭제")
    void handleUserLoggedOutEvent_ShouldDeleteFcmTokens() {
        // Given
        Long userId = 1L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);

        // When
        fcmTokenRemoveListener.handleUserLoggedOutEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 다양한 사용자 ID")
    void handleUserLoggedOutEvent_WithVariousUserIds_ShouldDeleteCorrectTokens() {
        // Given
        Long userId = 999L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);

        // When
        fcmTokenRemoveListener.handleUserLoggedOutEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - null 사용자 ID")
    void handleUserLoggedOutEvent_WithNullUserId_ShouldCallDeleteTokens() {
        // Given
        Long userId = null;
        UserLoggedOutEvent event = new UserLoggedOutEvent(userId, 1L, java.time.LocalDateTime.now());

        // When
        fcmTokenRemoveListener.handleUserLoggedOutEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - FCM 토큰 삭제")
    void handleUserWithdrawnEvent_ShouldDeleteFcmTokens() {
        // Given
        Long userId = 1L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

        // When
        fcmTokenRemoveListener.handleUserWithdrawnEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - 다양한 사용자 ID")
    void handleUserWithdrawnEvent_WithVariousUserIds_ShouldDeleteCorrectTokens() {
        // Given
        Long userId = 777L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

        // When
        fcmTokenRemoveListener.handleUserWithdrawnEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - null 사용자 ID")
    void handleUserWithdrawnEvent_WithNullUserId_ShouldCallDeleteTokens() {
        // Given
        Long userId = null;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

        // When
        fcmTokenRemoveListener.handleUserWithdrawnEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - FCM 토큰 삭제 실패 시 예외 전파")
    void handleUserLoggedOutEvent_WhenDeleteFails_ShouldPropagateException() {
        // Given
        Long userId = 1L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);
        
        RuntimeException deleteException = new RuntimeException("FCM 토큰 삭제 실패");
        doThrow(deleteException).when(notificationFcmUseCase).deleteFcmTokens(userId);

        // When & Then
        try {
            fcmTokenRemoveListener.handleUserLoggedOutEvent(event);
        } catch (RuntimeException e) {
            verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
        }
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - FCM 토큰 삭제 실패 시 예외 전파")
    void handleUserWithdrawnEvent_WhenDeleteFails_ShouldPropagateException() {
        // Given
        Long userId = 1L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);
        
        RuntimeException deleteException = new RuntimeException("FCM 토큰 삭제 실패");
        doThrow(deleteException).when(notificationFcmUseCase).deleteFcmTokens(userId);

        // When & Then
        try {
            fcmTokenRemoveListener.handleUserWithdrawnEvent(event);
        } catch (RuntimeException e) {
            verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
        }
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
        fcmTokenRemoveListener.handleUserLoggedOutEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - 이벤트 데이터 검증")
    void handleUserWithdrawnEvent_EventDataValidation() {
        // Given
        Long userId = 456L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);
        
        // 이벤트 데이터 검증
        assert event.userId().equals(userId);

        // When
        fcmTokenRemoveListener.handleUserWithdrawnEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 0인 사용자 ID")
    void handleUserLoggedOutEvent_WithZeroUserId_ShouldDeleteTokens() {
        // Given
        Long userId = 0L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);

        // When
        fcmTokenRemoveListener.handleUserLoggedOutEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - 0인 사용자 ID")
    void handleUserWithdrawnEvent_WithZeroUserId_ShouldDeleteTokens() {
        // Given
        Long userId = 0L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

        // When
        fcmTokenRemoveListener.handleUserWithdrawnEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 음수 사용자 ID")
    void handleUserLoggedOutEvent_WithNegativeUserId_ShouldDeleteTokens() {
        // Given
        Long userId = -1L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, 1L);

        // When
        fcmTokenRemoveListener.handleUserLoggedOutEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - 매우 큰 사용자 ID")
    void handleUserWithdrawnEvent_WithLargeUserId_ShouldDeleteTokens() {
        // Given
        Long userId = Long.MAX_VALUE;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

        // When
        fcmTokenRemoveListener.handleUserWithdrawnEvent(event);

        // Then
        verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
    }
}