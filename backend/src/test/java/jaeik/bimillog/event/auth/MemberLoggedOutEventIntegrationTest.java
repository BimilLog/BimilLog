package jaeik.bimillog.event.auth;

import jaeik.bimillog.domain.auth.application.port.in.AuthTokenUseCase;
import jaeik.bimillog.domain.auth.application.port.in.KakaoTokenUseCase;
import jaeik.bimillog.domain.auth.event.MemberLoggedOutEvent;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
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
    private jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase socialLogoutUseCase;

    @MockitoBean
    private AuthTokenUseCase authTokenUseCase;

    @MockitoBean
    private SseUseCase sseUseCase;

    @MockitoBean
    private FcmUseCase fcmUseCase;

    @MockitoBean
    private KakaoTokenUseCase kakaoTokenUseCase;

    @Test
    @DisplayName("사용자 로그아웃 이벤트 워크플로우 - 토큰 정리와 SSE 정리까지 완료")
    void userLoggedOutEventWorkflow_ShouldCompleteCleanupTasks() {
        // Given
        Long memberId = 1L;
        Long tokenId = 100L;
        Long fcmTokenId = 200L;
        MemberLoggedOutEvent event = new MemberLoggedOutEvent(memberId, tokenId, fcmTokenId, SocialProvider.KAKAO);

        // When & Then
        publishAndVerify(event, () -> {
            // SSE 연결 정리
            verify(sseUseCase).deleteEmitters(eq(memberId), eq(tokenId));
            // 소셜 플랫폼 로그아웃
            verify(socialLogoutUseCase).socialLogout(eq(memberId), eq(SocialProvider.KAKAO), eq(tokenId));
            // FCM 토큰 삭제
            verify(fcmUseCase).deleteFcmTokens(eq(memberId), eq(fcmTokenId));
            // JWT 토큰 무효화
            verify(authTokenUseCase).deleteTokens(eq(memberId), eq(tokenId));
            // 카카오 토큰 삭제
            verify(kakaoTokenUseCase).deleteByMemberId(eq(memberId));
        });
    }


    @Test
    @DisplayName("여러 사용자 로그아웃 이벤트 동시 처리")
    void multipleUserLoggedOutEvents_ShouldProcessConcurrently() {
        // Given
        MemberLoggedOutEvent event1 = new MemberLoggedOutEvent(1L, 101L, 201L, SocialProvider.KAKAO);
        MemberLoggedOutEvent event2 = new MemberLoggedOutEvent(2L, 102L, 202L, SocialProvider.KAKAO);
        MemberLoggedOutEvent event3 = new MemberLoggedOutEvent(3L, 103L, 203L, SocialProvider.KAKAO);

        // When & Then - 동시에 여러 로그아웃 이벤트 발행
        publishEventsAndVerify(new Object[]{event1, event2, event3}, () -> {
            // SSE 연결 정리
            verify(sseUseCase).deleteEmitters(eq(1L), eq(101L));
            verify(sseUseCase).deleteEmitters(eq(2L), eq(102L));
            verify(sseUseCase).deleteEmitters(eq(3L), eq(103L));

            // 소셜 플랫폼 로그아웃
            verify(socialLogoutUseCase).socialLogout(eq(1L), eq(SocialProvider.KAKAO), eq(101L));
            verify(socialLogoutUseCase).socialLogout(eq(2L), eq(SocialProvider.KAKAO), eq(102L));
            verify(socialLogoutUseCase).socialLogout(eq(3L), eq(SocialProvider.KAKAO), eq(103L));

            // FCM 토큰 삭제
            verify(fcmUseCase).deleteFcmTokens(eq(1L), eq(201L));
            verify(fcmUseCase).deleteFcmTokens(eq(2L), eq(202L));
            verify(fcmUseCase).deleteFcmTokens(eq(3L), eq(203L));

            // JWT 토큰 무효화
            verify(authTokenUseCase).deleteTokens(eq(1L), eq(101L));
            verify(authTokenUseCase).deleteTokens(eq(2L), eq(102L));
            verify(authTokenUseCase).deleteTokens(eq(3L), eq(103L));

            // 카카오 토큰 삭제
            verify(kakaoTokenUseCase).deleteByMemberId(eq(1L));
            verify(kakaoTokenUseCase).deleteByMemberId(eq(2L));
            verify(kakaoTokenUseCase).deleteByMemberId(eq(3L));
        });
    }

}
