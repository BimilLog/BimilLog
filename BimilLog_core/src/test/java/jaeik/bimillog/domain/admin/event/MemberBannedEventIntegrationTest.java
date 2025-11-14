package jaeik.bimillog.domain.admin.event;

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
 * <h2>사용자 차단 이벤트 워크플로우 통합 테스트</h2>
 * <p>관리자가 사용자를 차단할 때 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("사용자 차단 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class MemberBannedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private SocialLogoutService socialLogoutService;

    @MockitoBean
    private FcmCommandService fcmCommandService;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private GlobalSocialTokenCommandAdapter globalSocialTokenCommandAdapter;

    @Test
    @DisplayName("사용자 차단 이벤트 워크플로우 - 강제 로그아웃 및 정리 완료")
    void userBannedEventWorkflow_ShouldCompleteBanAndCleanup() {
        // Given
        Long memberId = 1L;
        String socialId = "testKakaoId123";
        SocialProvider provider = SocialProvider.KAKAO;
        MemberBannedEvent event = new MemberBannedEvent(memberId, socialId, provider);

        // When & Then
        publishAndVerify(event, () -> {
            // SSE 연결 정리
            verify(sseService).deleteEmitters(eq(memberId), eq(null));
            // 소셜 플랫폼 강제 로그아웃
            verify(socialLogoutService).forceLogout(eq(socialId), eq(provider));
            // FCM 토큰 삭제
            // JWT 토큰 무효화
            verify(authTokenService).deleteTokens(eq(memberId), eq(null));
            // 소셜 토큰 삭제
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(memberId));
        });
    }

    @Test
    @DisplayName("여러 사용자 차단 이벤트 동시 처리")
    void multipleUserBannedEvents_ShouldProcessConcurrently() {
        // Given
        MemberBannedEvent event1 = new MemberBannedEvent(1L, "kakao123", SocialProvider.KAKAO);
        MemberBannedEvent event2 = new MemberBannedEvent(2L, "kakao456", SocialProvider.KAKAO);
        MemberBannedEvent event3 = new MemberBannedEvent(3L, "kakao789", SocialProvider.KAKAO);

        // When & Then - 동시에 여러 사용자 차단 이벤트 발행
        publishEventsAndVerify(new Object[]{event1, event2, event3}, () -> {
            // SSE 연결 정리
            verify(sseService).deleteEmitters(eq(1L), eq(null));
            verify(sseService).deleteEmitters(eq(2L), eq(null));
            verify(sseService).deleteEmitters(eq(3L), eq(null));

            // 소셜 플랫폼 강제 로그아웃
            verify(socialLogoutService).forceLogout(eq("kakao123"), eq(SocialProvider.KAKAO));
            verify(socialLogoutService).forceLogout(eq("kakao456"), eq(SocialProvider.KAKAO));
            verify(socialLogoutService).forceLogout(eq("kakao789"), eq(SocialProvider.KAKAO));

            // FCM 토큰 삭제

            // JWT 토큰 무효화
            verify(authTokenService).deleteTokens(eq(1L), eq(null));
            verify(authTokenService).deleteTokens(eq(2L), eq(null));
            verify(authTokenService).deleteTokens(eq(3L), eq(null));

            // 소셜 토큰 삭제
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(1L));
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(2L));
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(3L));
        });
    }

    @Test
    @DisplayName("다양한 소셜 제공자의 차단 이벤트 처리")
    void userBannedEventsWithVariousProviders_ShouldProcessCorrectly() {
        // Given - 다양한 소셜 제공자
        MemberBannedEvent kakaoEvent = new MemberBannedEvent(1L, "kakaoUser", SocialProvider.KAKAO);
        // 향후 다른 소셜 제공자 추가 시 테스트 확장 가능

        // When & Then - 모든 제공자별로 적절히 처리되어야 함
        publishAndVerify(kakaoEvent, () -> {
            verify(sseService).deleteEmitters(eq(1L), eq(null));
            verify(socialLogoutService).forceLogout(eq("kakaoUser"), eq(SocialProvider.KAKAO));
            verify(authTokenService).deleteTokens(eq(1L), eq(null));
            verify(globalSocialTokenCommandAdapter).deleteByMemberId(eq(1L));
        });
    }
}
