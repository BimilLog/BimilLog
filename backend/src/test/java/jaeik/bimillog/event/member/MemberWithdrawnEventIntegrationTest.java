package jaeik.bimillog.event.member;

import jaeik.bimillog.domain.auth.application.port.in.SocialWithdrawUseCase;
import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.domain.member.event.MemberWithdrawnEvent;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * <h2>사용자 탈퇴 이벤트 워크플로우 통합 테스트</h2>
 * <p>사용자 탈퇴 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("사용자 탈퇴 이벤트 워크플로우 통합 테스트")
@Tag("integration")
class MemberWithdrawnEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private CommentCommandUseCase commentCommandUseCase;

    @MockitoBean
    private FcmUseCase fcmUseCase;

    @MockitoBean
    private SocialWithdrawUseCase socialWithdrawUseCase;

    @Test
    @DisplayName("사용자 탈퇴 이벤트 워크플로우 - 댓글 처리, FCM 토큰 삭제, 소셜 연결 해제 완료")
    void userWithdrawnEventWorkflow_ShouldCompleteAllCleanupTasks() {
        // Given
        Long userId = 1L;
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(userId, "testSocialId", SocialProvider.KAKAO);

        // When & Then
        publishAndVerify(event, () -> {
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId));
            verify(fcmUseCase).deleteFcmTokens(eq(userId), eq(null));
            verify(socialWithdrawUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq("testSocialId"));
        });
    }

    @Test
    @DisplayName("여러 사용자 탈퇴 이벤트 동시 처리")
    void multipleUserWithdrawnEvents() {
        // Given
        MemberWithdrawnEvent event1 = new MemberWithdrawnEvent(1L, "testSocialId1", SocialProvider.KAKAO);
        MemberWithdrawnEvent event2 = new MemberWithdrawnEvent(2L, "testSocialId2", SocialProvider.KAKAO);
        MemberWithdrawnEvent event3 = new MemberWithdrawnEvent(3L, "testSocialId3", SocialProvider.KAKAO);

        // When & Then - 여러 사용자 탈퇴 이벤트 발행
        publishEventsAndVerify(new Object[]{event1, event2, event3}, () -> {
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(1L));
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(2L));
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(3L));
            verify(fcmUseCase).deleteFcmTokens(eq(1L), eq(null));
            verify(fcmUseCase).deleteFcmTokens(eq(2L), eq(null));
            verify(fcmUseCase).deleteFcmTokens(eq(3L), eq(null));
            verify(socialWithdrawUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq("testSocialId1"));
            verify(socialWithdrawUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq("testSocialId2"));
            verify(socialWithdrawUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq("testSocialId3"));
        });
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - 리스너들이 독립적으로 처리")
    void eventProcessingWithException_ListenersProcessIndependently() {
        // Given
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(1L, "testSocialId1", SocialProvider.KAKAO);

        // 댓글 처리 실패 시뮬레이션
        doThrow(new RuntimeException("댓글 처리 실패")).when(commentCommandUseCase).processUserCommentsOnWithdrawal(1L);

        // When & Then - 예외가 발생해도 리스너들이 독립적으로 처리되어야 함
        publishAndExpectException(event, () -> {
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(1L));
            // FCM 토큰 삭제와 소셜 연결 해제는 별도 리스너이므로 댓글 처리 실패와 관계없이 처리되어야 함
            verify(fcmUseCase).deleteFcmTokens(eq(1L), eq(null));
            // SocialUnlinkListener는 createDefaultWithdrawEvent의 기본값 사용
            verify(socialWithdrawUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq("testSocialId1"));
        });
    }
}