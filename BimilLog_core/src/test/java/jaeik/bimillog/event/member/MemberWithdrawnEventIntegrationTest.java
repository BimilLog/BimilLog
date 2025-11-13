package jaeik.bimillog.event.member;

import jaeik.bimillog.domain.admin.service.AdminCommandService;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialWithdrawService;
import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.global.out.GlobalSocialStrategyAdapter;
import jaeik.bimillog.domain.global.out.GlobalSocialTokenCommandAdapter;
import jaeik.bimillog.domain.global.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.global.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.service.MemberCommandService;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.event.MemberWithdrawnEvent;
import jaeik.bimillog.domain.notification.service.FcmCommandService;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.domain.notification.service.SseService;
import jaeik.bimillog.domain.paper.service.PaperCommandService;
import jaeik.bimillog.domain.post.service.PostCommandService;
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
    private SseService sseService;

    @MockitoSpyBean
    private SocialWithdrawService socialWithdrawService;

    @MockitoBean
    private CommentCommandService CommentCommandService;

    @MockitoBean
    private PostCommandService postCommandService;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private FcmCommandService fcmCommandService;

    @MockitoBean
    private NotificationCommandService notificationCommandService;

    @MockitoBean
    private PaperCommandService paperCommandService;

    @MockitoBean
    private AdminCommandService adminCommandService;

    @MockitoBean
    private GlobalSocialTokenCommandAdapter globalSocialTokenCommandAdapter;

    @MockitoBean
    private MemberCommandService memberCommandService;

    @MockitoBean
    private GlobalSocialStrategyAdapter globalSocialStrategyAdapter;

    private static final SocialPlatformStrategy NOOP_PLATFORM_STRATEGY = new SocialPlatformStrategy(
            SocialProvider.KAKAO,
            new SocialAuthStrategy() {
                @Override
                public SocialProvider getProvider() {
                    return SocialProvider.KAKAO;
                }

                @Override
                public SocialMemberProfile getSocialToken(String code, String state) {
                    throw new UnsupportedOperationException("테스트 전략에서는 소셜 토큰 발급을 지원하지 않습니다.");
                }

                @Override
                public void unlink(String socialId, String accessToken) {
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

                @Override
                public String refreshAccessToken(String refreshToken) throws Exception {
                    return "test-refreshed-token";
                }
            }
    ) {};

    @BeforeEach
    void setUpSocialStrategy() {
        doReturn(NOOP_PLATFORM_STRATEGY).when(globalSocialStrategyAdapter).getStrategy(any());
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
            verify(sseService).deleteEmitters(eq(memberId), eq(null));
            // 2. 소셜 계정 연동 해제 전략 조회
            verify(globalSocialStrategyAdapter).getStrategy(eq(SocialProvider.KAKAO));
            // 3. 댓글 처리
            verify(CommentCommandService).processUserCommentsOnWithdrawal(eq(memberId));
            // 4. 게시글 삭제
            verify(postCommandService).deleteAllPostsByMemberId(eq(memberId));
            // 5. JWT 토큰 무효화
            verify(authTokenService).deleteTokens(eq(memberId), eq(null));
            // 6. FCM 토큰 삭제
            // 7. 알림 삭제
            verify(notificationCommandService).deleteAllNotification(eq(memberId));
            // 8. 롤링페이퍼 메시지 삭제
            verify(paperCommandService).deleteMessageInMyPaper(eq(memberId), eq(null));
            // 9. 신고자 익명화
            verify(adminCommandService).anonymizeReporterByUserId(eq(memberId));
            // 10. 소셜 토큰 삭제
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(memberId));
            // 11. 계정 정보 삭제
            verify(memberCommandService).removeMemberAccount(eq(memberId));
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
            verify(sseService).deleteEmitters(eq(1L), eq(null));
            verify(sseService).deleteEmitters(eq(2L), eq(null));
            verify(sseService).deleteEmitters(eq(3L), eq(null));

            // 소셜 계정 연동 해제 전략 조회
            verify(globalSocialStrategyAdapter, times(3)).getStrategy(eq(SocialProvider.KAKAO));

            // 댓글 처리
            verify(CommentCommandService).processUserCommentsOnWithdrawal(eq(1L));
            verify(CommentCommandService).processUserCommentsOnWithdrawal(eq(2L));
            verify(CommentCommandService).processUserCommentsOnWithdrawal(eq(3L));

            // 게시글 삭제
            verify(postCommandService).deleteAllPostsByMemberId(eq(1L));
            verify(postCommandService).deleteAllPostsByMemberId(eq(2L));
            verify(postCommandService).deleteAllPostsByMemberId(eq(3L));

            // JWT 토큰 무효화
            verify(authTokenService).deleteTokens(eq(1L), eq(null));
            verify(authTokenService).deleteTokens(eq(2L), eq(null));
            verify(authTokenService).deleteTokens(eq(3L), eq(null));

            // FCM 토큰 삭제

            // 알림 삭제
            verify(notificationCommandService).deleteAllNotification(eq(1L));
            verify(notificationCommandService).deleteAllNotification(eq(2L));
            verify(notificationCommandService).deleteAllNotification(eq(3L));

            // 롤링페이퍼 메시지 삭제
            verify(paperCommandService).deleteMessageInMyPaper(eq(1L), eq(null));
            verify(paperCommandService).deleteMessageInMyPaper(eq(2L), eq(null));
            verify(paperCommandService).deleteMessageInMyPaper(eq(3L), eq(null));

            // 신고자 익명화
            verify(adminCommandService).anonymizeReporterByUserId(eq(1L));
            verify(adminCommandService).anonymizeReporterByUserId(eq(2L));
            verify(adminCommandService).anonymizeReporterByUserId(eq(3L));

            // 소셜 토큰 삭제
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(1L));
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(2L));
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(3L));

            // 계정 정보 삭제
            verify(memberCommandService).removeMemberAccount(eq(1L));
            verify(memberCommandService).removeMemberAccount(eq(2L));
            verify(memberCommandService).removeMemberAccount(eq(3L));
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
                    public SocialMemberProfile getSocialToken(String code, String state) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void unlink(String socialId, String accessToken) {
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

                    @Override
                    public String refreshAccessToken(String refreshToken) throws Exception {
                        return "test-refreshed-token";
                    }
                }
        ) {};

        doReturn(failingStrategy).when(globalSocialStrategyAdapter).getStrategy(SocialProvider.KAKAO);

        // When & Then
        publishAndVerify(event, () -> {
            verify(sseService).deleteEmitters(eq(memberId), eq(null));
            verify(globalSocialStrategyAdapter).getStrategy(eq(SocialProvider.KAKAO));
            verify(CommentCommandService).processUserCommentsOnWithdrawal(eq(memberId));
            verify(postCommandService).deleteAllPostsByMemberId(eq(memberId));
            verify(authTokenService).deleteTokens(eq(memberId), eq(null));
            verify(notificationCommandService).deleteAllNotification(eq(memberId));
            verify(paperCommandService).deleteMessageInMyPaper(eq(memberId), eq(null));
            verify(adminCommandService).anonymizeReporterByUserId(eq(memberId));
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(memberId));
            verify(memberCommandService).removeMemberAccount(eq(memberId));
        });

        assertThat(unlinkAttempted).isTrue();
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - 단일 리스너의 순차 처리")
    void eventProcessingWithException_SequentialProcessing() {
        // Given
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(1L, "testSocialId1", SocialProvider.KAKAO);

        // 댓글 처리 실패 시뮬레이션
        doThrow(new RuntimeException("댓글 처리 실패")).when(CommentCommandService).processUserCommentsOnWithdrawal(1L);

        // When & Then - 예외 발생 시 해당 시점 이전까지만 처리됨 (순차 처리)
        publishAndExpectException(event, () -> {
            // SSE 연결 정리는 먼저 실행됨
            verify(sseService).deleteEmitters(eq(1L), eq(null));
            // 소셜 계정 연동 해제 전략 조회도 실행됨
            verify(globalSocialStrategyAdapter).getStrategy(eq(SocialProvider.KAKAO));
            // 댓글 처리에서 예외 발생
            verify(CommentCommandService).processUserCommentsOnWithdrawal(eq(1L));
        });
    }
}
