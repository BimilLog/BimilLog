package jaeik.growfarm.infrastructure.adapter.user.in.listener;

import jaeik.growfarm.domain.admin.event.UserBannedEvent;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.application.port.out.UserCommandPort;
import jaeik.growfarm.domain.user.entity.BlackList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * <h2>블랙리스트 이벤트 리스너 테스트</h2>
 * <p>BlacklistEventListener의 단위 테스트</p>
 * <p>사용자 차단 시 블랙리스트 생성 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("블랙리스트 이벤트 리스너 테스트")
class BlacklistEventListenerTest {

    @Mock
    private UserCommandPort userCommandPort;

    @InjectMocks
    private BlacklistEventListener blacklistEventListener;

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 카카오 사용자")
    void handleUserBannedEvent_WithKakaoProvider() {
        // Given
        Long userId = 1L;
        String socialId = "12345";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);

        // When
        blacklistEventListener.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getSocialId()).isEqualTo(socialId);
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 구글 사용자")
    void handleUserBannedEvent_WithGoogleProvider() {
        // Given
        Long userId = 2L;
        String socialId = "google123";
        SocialProvider provider = SocialProvider.GOOGLE;
        UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);

        // When
        blacklistEventListener.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getSocialId()).isEqualTo(socialId);
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - null 소셜 ID")
    void handleUserBannedEvent_WithNullSocialId() {
        // Given
        Long userId = 3L;
        String socialId = null;
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);

        // When
        blacklistEventListener.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getSocialId()).isNull();
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 빈 소셜 ID")
    void handleUserBannedEvent_WithEmptySocialId() {
        // Given
        Long userId = 4L;
        String socialId = "";
        SocialProvider provider = SocialProvider.GOOGLE;
        UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);

        // When
        blacklistEventListener.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getSocialId()).isEmpty();
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 모든 Provider 타입")
    void handleUserBannedEvent_WithAllProviders() {
        // Given & When & Then
        SocialProvider[] providers = SocialProvider.values();
        
        for (int i = 0; i < providers.length; i++) {
            SocialProvider provider = providers[i];
            String socialId = "user_" + provider.name().toLowerCase();
            Long userId = (long) (i + 1);
            
            UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);
            blacklistEventListener.handleUserBannedEvent(event);
        }

        // Then
        verify(userCommandPort, times(providers.length)).save(any(BlackList.class));
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - BlackList 객체 생성 검증")
    void handleUserBannedEvent_VerifyBlackListCreation() {
        // Given
        Long userId = 5L;
        String socialId = "test_user_123";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);

        // When
        blacklistEventListener.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList savedBlackList = blackListCaptor.getValue();
        assertThat(savedBlackList).isNotNull();
        assertThat(savedBlackList.getSocialId()).isEqualTo(socialId);
        assertThat(savedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 동일 사용자 중복 차단")
    void handleUserBannedEvent_DuplicateBanEvents() {
        // Given
        Long userId = 6L;
        String socialId = "duplicate_user";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);

        // When
        blacklistEventListener.handleUserBannedEvent(event);
        blacklistEventListener.handleUserBannedEvent(event);
        blacklistEventListener.handleUserBannedEvent(event);

        // Then - 각각의 이벤트에 대해 블랙리스트 생성 (중복 검사는 비즈니스 로직에서 처리)
        verify(userCommandPort, times(3)).save(any(BlackList.class));
    }
}