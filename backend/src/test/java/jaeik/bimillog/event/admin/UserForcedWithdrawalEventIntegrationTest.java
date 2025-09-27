package jaeik.bimillog.event.admin;

import jaeik.bimillog.domain.admin.event.UserForcedWithdrawalEvent;
import jaeik.bimillog.domain.auth.application.port.in.UserBanUseCase;
import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.user.application.port.in.WithdrawUseCase;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import jaeik.bimillog.testutil.EventTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;


@DisplayName("관리자 강제 탈퇴 요청 이벤트 워크플로우 통합 테스트")
@Tag("integration")
class UserForcedWithdrawalEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private WithdrawUseCase withdrawUseCase;

    @MockitoBean
    private UserBanUseCase userBanUseCase;

    @MockitoBean
    private CommentCommandUseCase commentCommandUseCase;

    @Test
    @DisplayName("관리자 강제 탈퇴 요청 이벤트 워크플로우 - 모든 후속 처리 완료")
    void adminWithdrawRequestedEventWorkflow_ShouldCompleteAllProcessing() {
        // Given
        Long userId = 1L;
        String reason = "관리자 강제 탈퇴";
        UserForcedWithdrawalEvent event = EventTestDataBuilder.createAdminWithdrawEvent(userId, reason);

        // When & Then
        publishAndVerify(event, () -> {
            // 사용자 블랙리스트 등록
            verify(withdrawUseCase).addToBlacklist(eq(userId));
            // JWT 토큰 무효화
            verify(userBanUseCase).blacklistAllUserTokens(eq(userId), eq("관리자 강제 탈퇴"));
            // 댓글 처리
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId));
        });
    }

    @Test
    @DisplayName("다중 관리자 강제 탈퇴 요청 이벤트 동시 처리")
    void multipleAdminWithdrawRequestedEvents_ShouldProcessConcurrently() {
        // Given
        UserForcedWithdrawalEvent event1 = EventTestDataBuilder.createAdminWithdrawEvent(1L, "스팸 행위");
        UserForcedWithdrawalEvent event2 = EventTestDataBuilder.createAdminWithdrawEvent(2L, "지속적 규칙 위반");
        UserForcedWithdrawalEvent event3 = EventTestDataBuilder.createAdminWithdrawEvent(3L, "부적절한 컨텐츠 게시");

        // When & Then - 동시에 여러 강제 탈퇴 이벤트 발행
        publishEventsAndVerify(new Object[]{event1, event2, event3}, () -> {
            verify(withdrawUseCase).addToBlacklist(eq(1L));
            verify(withdrawUseCase).addToBlacklist(eq(2L));
            verify(withdrawUseCase).addToBlacklist(eq(3L));

            verify(userBanUseCase).blacklistAllUserTokens(eq(1L), eq("관리자 강제 탈퇴"));
            verify(userBanUseCase).blacklistAllUserTokens(eq(2L), eq("관리자 강제 탈퇴"));
            verify(userBanUseCase).blacklistAllUserTokens(eq(3L), eq("관리자 강제 탈퇴"));

            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(1L));
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(2L));
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(3L));
        });
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - 댓글 처리 실패")
    void eventProcessingWithException_CommentProcessingFailure() {
        // Given
        Long userId = 1L;
        UserForcedWithdrawalEvent event = EventTestDataBuilder.createAdminWithdrawEvent(userId, "예외 테스트");

        // 댓글 처리 실패 시뮬레이션
        doThrow(new RuntimeException("댓글 처리 실패"))
                .when(commentCommandUseCase).processUserCommentsOnWithdrawal(userId);

        // When & Then - 예외가 발생해도 다른 리스너들은 호출되어야 함
        publishAndExpectException(event, () -> {
            verify(withdrawUseCase).addToBlacklist(eq(userId));
            verify(userBanUseCase).blacklistAllUserTokens(eq(userId), eq("관리자 강제 탈퇴"));
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId));
        });
    }
}