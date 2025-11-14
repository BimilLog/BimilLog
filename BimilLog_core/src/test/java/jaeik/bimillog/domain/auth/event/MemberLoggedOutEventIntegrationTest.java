package jaeik.bimillog.domain.auth.event;

import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialLogoutService;
import jaeik.bimillog.domain.global.out.GlobalSocialTokenCommandAdapter;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.notification.service.FcmCommandService;
import jaeik.bimillog.domain.notification.service.SseService;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
@Tag("integration")
public class MemberLoggedOutEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private SocialLogoutService socialLogoutService;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private FcmCommandService fcmCommandService;

    @MockitoBean
    private GlobalSocialTokenCommandAdapter globalSocialTokenCommandAdapter;

    @Test
    @DisplayName("사용자 로그아웃 이벤트 워크플로우 - 토큰 정리와 SSE 정리까지 완료")
    void userLoggedOutEventWorkflow_ShouldCompleteCleanupTasks() {
        // Given
        Long memberId = 1L;
        Long tokenId = 100L;
        MemberLoggedOutEvent event = new MemberLoggedOutEvent(memberId, tokenId, SocialProvider.KAKAO);

        // When & Then
        publishAndVerify(event, () -> {
            // SSE 연결 정리
            verify(sseService).deleteEmitters(eq(memberId), eq(tokenId));
            // 소셜 플랫폼 로그아웃
            verifySocialLogout(memberId, tokenId);
            // AuthToken 삭제 (FCM 토큰도 함께 삭제됨)
            verify(authTokenService).deleteTokens(eq(memberId), eq(tokenId));
            // 소셜 토큰 삭제
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(memberId));
        });
    }

    private void verifySocialLogout(Long memberId, Long tokenId) {
        try {
            verify(socialLogoutService).socialLogout(eq(memberId), eq(SocialProvider.KAKAO));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("여러 사용자 로그아웃 이벤트 동시 처리")
    void multipleUserLoggedOutEvents_ShouldProcessConcurrently() {
        // Given
        MemberLoggedOutEvent event1 = new MemberLoggedOutEvent(1L, 101L, SocialProvider.KAKAO);
        MemberLoggedOutEvent event2 = new MemberLoggedOutEvent(2L, 102L, SocialProvider.KAKAO);
        MemberLoggedOutEvent event3 = new MemberLoggedOutEvent(3L, 103L, SocialProvider.KAKAO);

        // When & Then - 동시에 여러 로그아웃 이벤트 발행
        publishEventsAndVerify(new Object[]{event1, event2, event3}, () -> {
            // SSE 연결 정리
            verify(sseService).deleteEmitters(eq(1L), eq(101L));
            verify(sseService).deleteEmitters(eq(2L), eq(102L));
            verify(sseService).deleteEmitters(eq(3L), eq(103L));

            // 소셜 플랫폼 로그아웃
            verifySocialLogout(1L, 101L);
            verifySocialLogout(2L, 102L);
            verifySocialLogout(3L, 103L);

            // AuthToken 삭제 (FCM 토큰도 함께 삭제됨)
            verify(authTokenService).deleteTokens(eq(1L), eq(101L));
            verify(authTokenService).deleteTokens(eq(2L), eq(102L));
            verify(authTokenService).deleteTokens(eq(3L), eq(103L));

            // 소셜 토큰 삭제
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(1L));
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(2L));
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(3L));
        });
    }

}
