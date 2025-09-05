package jaeik.bimillog.infrastructure.adapter.auth.in.listener;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.SocialUnlinkUseCase;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * <h2>사용자 차단 이벤트 리스너 테스트</h2>
 * <p>UserBannedEventListener의 단위 테스트</p>
 * <p>사용자 차단 시 소셜 로그인 해제 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 차단 이벤트 리스너 테스트")
class UserBannedEventListenerTest {

    @Mock
    private SocialUnlinkUseCase socialUnlinkUseCase;

    @InjectMocks
    private SocialUnlinkListener socialUnlinkListener;

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 카카오 로그인 해제")
    void handleUserBannedEvent_WithKakaoProvider() {
        // Given
        Long userId = 1L;
        String socialId = "12345";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(userId, socialId, provider);

        // When
        socialUnlinkListener.handleUserBannedEvent(event);

        // Then
        verify(socialUnlinkUseCase).unlinkSocialAccount(eq(provider), eq(socialId));
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 소셜 로그인 해제 실패 시에도 예외 전파")
    void handleUserBannedEvent_WhenUnlinkFails_ShouldPropagateException() {
        // Given
        Long userId = 1L;
        String socialId = "12345";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(userId, socialId, provider);
        
        RuntimeException unlinkException = new RuntimeException("소셜 로그인 해제 실패");
        doThrow(unlinkException).when(socialUnlinkUseCase).unlinkSocialAccount(provider, socialId);

        // When & Then
        try {
            socialUnlinkListener.handleUserBannedEvent(event);
        } catch (RuntimeException e) {
            // 예외가 전파되어야 함 (차단 처리 자체가 실패로 간주)
            verify(socialUnlinkUseCase).unlinkSocialAccount(eq(provider), eq(socialId));
        }
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 다양한 소셜 제공자")
    void handleUserBannedEvent_WithDifferentProviders() {
        // Given - KAKAO
        UserBannedEvent kakaoEvent = new UserBannedEvent(1L, "kakao123", SocialProvider.KAKAO);
        
        // When - KAKAO
        socialUnlinkListener.handleUserBannedEvent(kakaoEvent);
        
        // Then - KAKAO
        verify(socialUnlinkUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq("kakao123"));
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 이벤트 정보 검증")
    void handleUserBannedEvent_EventDataValidation() {
        // Given
        Long userId = 999L;
        String socialId = "testSocialId";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(userId, socialId, provider);

        // 이벤트 데이터 검증
        assert event.userId().equals(userId);
        assert event.socialId().equals(socialId);
        assert event.provider().equals(provider);

        // When
        socialUnlinkListener.handleUserBannedEvent(event);

        // Then
        verify(socialUnlinkUseCase).unlinkSocialAccount(eq(provider), eq(socialId));
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - null socialId 처리")
    void handleUserBannedEvent_WithNullSocialId() {
        // Given
        UserBannedEvent event = new UserBannedEvent(1L, null, SocialProvider.KAKAO);

        // When
        socialUnlinkListener.handleUserBannedEvent(event);

        // Then - null socialId도 포트로 전달되어야 함
        verify(socialUnlinkUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq(null));
    }
}