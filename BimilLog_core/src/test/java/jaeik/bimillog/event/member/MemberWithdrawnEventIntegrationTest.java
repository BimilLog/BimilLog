package jaeik.bimillog.event.member;

import jaeik.bimillog.domain.admin.application.service.AdminCommandService;
import jaeik.bimillog.domain.auth.application.port.in.AuthTokenUseCase;
import jaeik.bimillog.domain.auth.application.port.in.KakaoTokenUseCase;
import jaeik.bimillog.domain.auth.application.port.in.SocialWithdrawUseCase;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.global.application.port.out.GlobalSocialStrategyPort;
import jaeik.bimillog.domain.global.application.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.global.application.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.application.port.in.MemberCommandUseCase;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.event.MemberWithdrawnEvent;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.bimillog.domain.post.application.port.in.PostCommandUseCase;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private SseUseCase sseUseCase;

    @MockitoSpyBean
    private SocialWithdrawUseCase socialWithdrawUseCase;

    @MockitoBean
    private CommentCommandUseCase commentCommandUseCase;

    @MockitoBean
    private PostCommandUseCase postCommandUseCase;

    @MockitoBean
    private AuthTokenUseCase authTokenUseCase;

    @MockitoBean
    private FcmUseCase fcmUseCase;

    @MockitoBean
    private NotificationCommandUseCase notificationCommandUseCase;

    @MockitoBean
    private PaperCommandUseCase paperCommandUseCase;

    @MockitoBean
    private AdminCommandService adminCommandService;

    @MockitoBean
    private KakaoTokenUseCase kakaoTokenUseCase;

    @MockitoBean
    private MemberCommandUseCase memberCommandUseCase;

    @MockitoBean
    private GlobalSocialStrategyPort globalSocialStrategyPort;

    private static final SocialPlatformStrategy NOOP_PLATFORM_STRATEGY = new SocialPlatformStrategy(
            SocialProvider.KAKAO,
            new SocialAuthStrategy() {
                @Override
                public SocialProvider getProvider() {
                    return SocialProvider.KAKAO;
                }

                @Override
                public SocialMemberProfile getSocialToken(String code) {
                    throw new UnsupportedOperationException("테스트 전략에서는 소셜 토큰 발급을 지원하지 않습니다.");
                }

                @Override
                public void getUserInfo(String accessToken) {
                    // no-op
                }

                @Override
                public void unlink(String socialId) {
                    // no-op
                }

                @Override
                public void logout(String accessToken) {
                    // no-op
                }

                @Override
                public void forceLogout(String socialId) {
                    // no-op
                }
            }
    ) {};

    @BeforeEach
    void setUpSocialStrategy() {
        doReturn(NOOP_PLATFORM_STRATEGY).when(globalSocialStrategyPort).getStrategy(any());
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 워크플로우 - 모든 데이터 정리 완료")
    void userWithdrawnEventWorkflow_ShouldCompleteAllCleanupTasks() {
        // Given
        Long memberId = 1L;
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(memberId, "testSocialId", SocialProvider.KAKAO);

        // When & Then
        publishAndVerify(event, () -> {
            // 1. SSE 연결 정리
            verify(sseUseCase).deleteEmitters(eq(memberId), eq(null));
            // 2. 소셜 계정 연동 해제 전략 조회
            verify(globalSocialStrategyPort).getStrategy(eq(SocialProvider.KAKAO));
            // 3. 댓글 처리
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(memberId));
            // 4. 게시글 삭제
            verify(postCommandUseCase).deleteAllPostsByMemberId(eq(memberId));
            // 5. JWT 토큰 무효화
            verify(authTokenUseCase).deleteTokens(eq(memberId), eq(null));
            // 6. FCM 토큰 삭제
            verify(fcmUseCase).deleteFcmTokens(eq(memberId), eq(null));
            // 7. 알림 삭제
            verify(notificationCommandUseCase).deleteAllNotification(eq(memberId));
            // 8. 롤링페이퍼 메시지 삭제
            verify(paperCommandUseCase).deleteMessageInMyPaper(eq(memberId), eq(null));
            // 9. 신고자 익명화
            verify(adminCommandService).anonymizeReporterByUserId(eq(memberId));
            // 10. 카카오 토큰 삭제
            verify(kakaoTokenUseCase).deleteByMemberId(eq(memberId));
            // 11. 계정 정보 삭제
            verify(memberCommandUseCase).removeMemberAccount(eq(memberId));
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
            // SSE 연결 정리
            verify(sseUseCase).deleteEmitters(eq(1L), eq(null));
            verify(sseUseCase).deleteEmitters(eq(2L), eq(null));
            verify(sseUseCase).deleteEmitters(eq(3L), eq(null));

            // 소셜 계정 연동 해제 전략 조회
            verify(globalSocialStrategyPort, times(3)).getStrategy(eq(SocialProvider.KAKAO));

            // 댓글 처리
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(1L));
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(2L));
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(3L));

            // 게시글 삭제
            verify(postCommandUseCase).deleteAllPostsByMemberId(eq(1L));
            verify(postCommandUseCase).deleteAllPostsByMemberId(eq(2L));
            verify(postCommandUseCase).deleteAllPostsByMemberId(eq(3L));

            // JWT 토큰 무효화
            verify(authTokenUseCase).deleteTokens(eq(1L), eq(null));
            verify(authTokenUseCase).deleteTokens(eq(2L), eq(null));
            verify(authTokenUseCase).deleteTokens(eq(3L), eq(null));

            // FCM 토큰 삭제
            verify(fcmUseCase).deleteFcmTokens(eq(1L), eq(null));
            verify(fcmUseCase).deleteFcmTokens(eq(2L), eq(null));
            verify(fcmUseCase).deleteFcmTokens(eq(3L), eq(null));

            // 알림 삭제
            verify(notificationCommandUseCase).deleteAllNotification(eq(1L));
            verify(notificationCommandUseCase).deleteAllNotification(eq(2L));
            verify(notificationCommandUseCase).deleteAllNotification(eq(3L));

            // 롤링페이퍼 메시지 삭제
            verify(paperCommandUseCase).deleteMessageInMyPaper(eq(1L), eq(null));
            verify(paperCommandUseCase).deleteMessageInMyPaper(eq(2L), eq(null));
            verify(paperCommandUseCase).deleteMessageInMyPaper(eq(3L), eq(null));

            // 신고자 익명화
            verify(adminCommandService).anonymizeReporterByUserId(eq(1L));
            verify(adminCommandService).anonymizeReporterByUserId(eq(2L));
            verify(adminCommandService).anonymizeReporterByUserId(eq(3L));

            // 카카오 토큰 삭제
            verify(kakaoTokenUseCase).deleteByMemberId(eq(1L));
            verify(kakaoTokenUseCase).deleteByMemberId(eq(2L));
            verify(kakaoTokenUseCase).deleteByMemberId(eq(3L));

            // 계정 정보 삭제
            verify(memberCommandUseCase).removeMemberAccount(eq(1L));
            verify(memberCommandUseCase).removeMemberAccount(eq(2L));
            verify(memberCommandUseCase).removeMemberAccount(eq(3L));
        });
    }

    @Test
    @DisplayName("소셜 연결 해제 실패 시에도 나머지 정리를 수행")
    void socialUnlinkFailure_ShouldContinueCleanup() {
        // Given
        Long memberId = 42L;
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(memberId, "failingSocialId", SocialProvider.KAKAO);

        AtomicBoolean unlinkAttempted = new AtomicBoolean(false);

        SocialPlatformStrategy failingStrategy = new SocialPlatformStrategy(
                SocialProvider.KAKAO,
                new SocialAuthStrategy() {
                    @Override
                    public SocialProvider getProvider() {
                        return SocialProvider.KAKAO;
                    }

                    @Override
                    public SocialMemberProfile getSocialToken(String code) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void getUserInfo(String accessToken) {
                        // no-op
                    }

                    @Override
                    public void unlink(String socialId) {
                        unlinkAttempted.set(true);
                        throw new RuntimeException("소셜 해제 실패");
                    }

                    @Override
                    public void logout(String accessToken) {
                        // no-op
                    }

                    @Override
                    public void forceLogout(String socialId) {
                        // no-op
                    }
                }
        ) {};

        doReturn(failingStrategy).when(globalSocialStrategyPort).getStrategy(SocialProvider.KAKAO);

        // When & Then
        publishAndVerify(event, () -> {
            verify(sseUseCase).deleteEmitters(eq(memberId), eq(null));
            verify(globalSocialStrategyPort).getStrategy(eq(SocialProvider.KAKAO));
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(memberId));
            verify(postCommandUseCase).deleteAllPostsByMemberId(eq(memberId));
            verify(authTokenUseCase).deleteTokens(eq(memberId), eq(null));
            verify(fcmUseCase).deleteFcmTokens(eq(memberId), eq(null));
            verify(notificationCommandUseCase).deleteAllNotification(eq(memberId));
            verify(paperCommandUseCase).deleteMessageInMyPaper(eq(memberId), eq(null));
            verify(adminCommandService).anonymizeReporterByUserId(eq(memberId));
            verify(kakaoTokenUseCase).deleteByMemberId(eq(memberId));
            verify(memberCommandUseCase).removeMemberAccount(eq(memberId));
        });

        assertThat(unlinkAttempted).isTrue();
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - 단일 리스너의 순차 처리")
    void eventProcessingWithException_SequentialProcessing() {
        // Given
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(1L, "testSocialId1", SocialProvider.KAKAO);

        // 댓글 처리 실패 시뮬레이션
        doThrow(new RuntimeException("댓글 처리 실패")).when(commentCommandUseCase).processUserCommentsOnWithdrawal(1L);

        // When & Then - 예외 발생 시 해당 시점 이전까지만 처리됨 (순차 처리)
        publishAndExpectException(event, () -> {
            // SSE 연결 정리는 먼저 실행됨
            verify(sseUseCase).deleteEmitters(eq(1L), eq(null));
            // 소셜 계정 연동 해제 전략 조회도 실행됨
            verify(globalSocialStrategyPort).getStrategy(eq(SocialProvider.KAKAO));
            // 댓글 처리에서 예외 발생
            verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(1L));
        });
    }
}