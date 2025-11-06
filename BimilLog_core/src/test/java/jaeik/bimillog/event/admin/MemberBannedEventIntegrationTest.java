package jaeik.bimillog.event.admin;

import jaeik.bimillog.domain.admin.event.MemberBannedEvent;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.KakaoTokenService;
import jaeik.bimillog.domain.auth.service.SocialLogoutService;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
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
    private SseUseCase sseUseCase;

    @MockitoBean
    private SocialLogoutService socialLogoutService;

    @MockitoBean
    private FcmUseCase fcmUseCase;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private KakaoTokenService kakaoTokenService;

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
            verify(sseUseCase).deleteEmitters(eq(memberId), eq(null));
            // 소셜 플랫폼 강제 로그아웃
            verify(socialLogoutService).forceLogout(eq(socialId), eq(provider));
            // FCM 토큰 삭제
            verify(fcmUseCase).deleteFcmTokens(eq(memberId), eq(null));
            // JWT 토큰 무효화
            verify(authTokenService).deleteTokens(eq(memberId), eq(null));
            // 카카오 토큰 삭제
            verify(kakaoTokenService).deleteByMemberId(eq(memberId));
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
            verify(sseUseCase).deleteEmitters(eq(1L), eq(null));
            verify(sseUseCase).deleteEmitters(eq(2L), eq(null));
            verify(sseUseCase).deleteEmitters(eq(3L), eq(null));

            // 소셜 플랫폼 강제 로그아웃
            verify(socialLogoutService).forceLogout(eq("kakao123"), eq(SocialProvider.KAKAO));
            verify(socialLogoutService).forceLogout(eq("kakao456"), eq(SocialProvider.KAKAO));
            verify(socialLogoutService).forceLogout(eq("kakao789"), eq(SocialProvider.KAKAO));

            // FCM 토큰 삭제
            verify(fcmUseCase).deleteFcmTokens(eq(1L), eq(null));
            verify(fcmUseCase).deleteFcmTokens(eq(2L), eq(null));
            verify(fcmUseCase).deleteFcmTokens(eq(3L), eq(null));

            // JWT 토큰 무효화
            verify(authTokenService).deleteTokens(eq(1L), eq(null));
            verify(authTokenService).deleteTokens(eq(2L), eq(null));
            verify(authTokenService).deleteTokens(eq(3L), eq(null));

            // 카카오 토큰 삭제
            verify(kakaoTokenService).deleteByMemberId(eq(1L));
            verify(kakaoTokenService).deleteByMemberId(eq(2L));
            verify(kakaoTokenService).deleteByMemberId(eq(3L));
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
            verify(sseUseCase).deleteEmitters(eq(1L), eq(null));
            verify(socialLogoutService).forceLogout(eq("kakaoUser"), eq(SocialProvider.KAKAO));
            verify(fcmUseCase).deleteFcmTokens(eq(1L), eq(null));
            verify(authTokenService).deleteTokens(eq(1L), eq(null));
            verify(kakaoTokenService).deleteByMemberId(eq(1L));
        });
    }
}
