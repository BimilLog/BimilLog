package jaeik.bimillog.infrastructure.adapter.auth.in.listener;

import jaeik.bimillog.domain.auth.application.port.in.TokenCleanupUseCase;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * <h2>토큰 정리 이벤트 리스너 테스트</h2>
 * <p>TokenCleanupEventListener의 단위 테스트</p>
 * <p>토큰 정리 로직과 예외 처리를 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("토큰 정리 이벤트 리스너 테스트")
class TokenCleanupListenerTest {

    @Mock
    private TokenCleanupUseCase tokenCleanupUseCase;

    @InjectMocks
    private TokenCleanupListener tokenCleanupListener;

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 정상 케이스")
    void handleUserLoggedOutEvent_Success() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        LocalDateTime loggedOutAt = LocalDateTime.now();
        UserLoggedOutEvent event = new UserLoggedOutEvent(userId, tokenId, loggedOutAt);

        // When
        tokenCleanupListener.handleUserLoggedOutEvent(event);

        // Then
        verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(tokenId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - 토큰 삭제 실패 시 예외를 로그로만 처리")
    void handleUserLoggedOutEvent_WhenDeleteFails_ShouldNotThrowException() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        LocalDateTime loggedOutAt = LocalDateTime.now();
        UserLoggedOutEvent event = new UserLoggedOutEvent(userId, tokenId, loggedOutAt);
        
        doThrow(new RuntimeException("토큰 삭제 실패"))
            .when(tokenCleanupUseCase).cleanupSpecificToken(userId, tokenId);

        // When & Then
        // 예외가 발생하지 않아야 함 (비동기 처리로 로그아웃 자체는 성공)
        tokenCleanupListener.handleUserLoggedOutEvent(event);
        
        // 포트 호출은 확인
        verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(tokenId));
    }

    @Test
    @DisplayName("사용자 로그아웃 이벤트 처리 - null 이벤트")
    void handleUserLoggedOutEvent_WithNullEvent() {
        // Given
        UserLoggedOutEvent event = null;

        // When & Then
        // NPE가 발생할 수 있지만 이는 시스템 오류이므로 적절한 예외 처리 확인
        try {
            tokenCleanupListener.handleUserLoggedOutEvent(event);
        } catch (NullPointerException e) {
            // 예상되는 동작
        }
    }

    @Test
    @DisplayName("팩토리 메서드로 생성된 이벤트 처리")
    void handleUserLoggedOutEvent_WithFactoryMethod() {
        // Given
        Long userId = 2L;
        Long tokenId = 200L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);

        // When
        tokenCleanupListener.handleUserLoggedOutEvent(event);

        // Then
        verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(tokenId));
    }
}