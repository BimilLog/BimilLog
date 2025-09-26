package jaeik.bimillog.event.auth;

import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.user.application.port.in.WithdrawUseCase;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import jaeik.bimillog.testutil.EventTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * <h2>사용자 로그아웃 이벤트 워크플로우 통합 테스트</h2>
 * <p>사용자 로그아웃 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("사용자 로그아웃 이벤트 워크플로우 통합 테스트")
@Tag("fast-integration")
public class UserLoggedOutEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private WithdrawUseCase withdrawUseCase;

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;

    @Test
    @DisplayName("사용자 로그아웃 이벤트 워크플로우 - 토큰 정리와 SSE 정리까지 완료")
    void userLoggedOutEventWorkflow_ShouldCompleteCleanupTasks() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        LocalDateTime loggedOutAt = LocalDateTime.now();
        UserLoggedOutEvent event = EventTestDataBuilder.createLogoutEvent(userId, tokenId, loggedOutAt);

        // When & Then
        publishAndVerify(event, () -> {
            // 특정 토큰 정리
            verify(withdrawUseCase).cleanupSpecificToken(eq(userId), eq(tokenId));
            // 특정 기기의 SSE 연결 정리
            verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
            // FCM 토큰 삭제
            verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
        });
    }


    @Test
    @DisplayName("여러 사용자 로그아웃 이벤트 동시 처리")
    void multipleUserLoggedOutEvents_ShouldProcessConcurrently() {
        // Given
        UserLoggedOutEvent event1 = EventTestDataBuilder.createDefaultLogoutEvent(1L);
        UserLoggedOutEvent event2 = EventTestDataBuilder.createDefaultLogoutEvent(2L);
        UserLoggedOutEvent event3 = EventTestDataBuilder.createDefaultLogoutEvent(3L);

        // When & Then - 동시에 여러 로그아웃 이벤트 발행
        publishEventsAndVerify(new Object[]{event1, event2, event3}, () -> {
            verify(withdrawUseCase).cleanupSpecificToken(eq(1L), eq(101L));
            verify(withdrawUseCase).cleanupSpecificToken(eq(2L), eq(102L));
            verify(withdrawUseCase).cleanupSpecificToken(eq(3L), eq(103L));

            verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(1L), eq(101L));
            verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(2L), eq(102L));
            verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(3L), eq(103L));

            verify(notificationFcmUseCase).deleteFcmTokens(eq(1L));
            verify(notificationFcmUseCase).deleteFcmTokens(eq(2L));
            verify(notificationFcmUseCase).deleteFcmTokens(eq(3L));
        });
    }

}
