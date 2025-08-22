package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.out.UserCommandPort;
import jaeik.growfarm.domain.user.entity.BlackList;
import jaeik.growfarm.domain.admin.event.UserBannedEvent;
import jaeik.growfarm.domain.common.entity.SocialProvider;
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
 * <h2>UserService 테스트</h2>
 * <p>사용자 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>사용자 차단 이벤트 처리 및 블랙리스트 생성 시나리오를 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
//TODO 비즈니스 로직의 변경으로 테스트코드와 비즈니스 로직의 흐름이 맞지 않을 시 테스트 코드의 변경이 적으면 테스트 수정 필요 변경이 많으면 Deprecated 처리 후 새로운 단위 테스트 작성 필요
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserCommandPort userCommandPort;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 카카오 사용자")
    void shouldHandleUserBannedEvent_WhenKakaoUser() {
        // Given
        String socialId = "123456789";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, 1L, socialId, provider);

        // When
        userService.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getSocialId()).isEqualTo(socialId);
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 구글 사용자")
    void shouldHandleUserBannedEvent_WhenGoogleUser() {
        // Given
        String socialId = "google_123456";
        SocialProvider provider = SocialProvider.GOOGLE;
        UserBannedEvent event = new UserBannedEvent(this, 2L, socialId, provider);

        // When
        userService.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getSocialId()).isEqualTo(socialId);
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - 빈 소셜 ID")
    void shouldHandleUserBannedEvent_WhenEmptySocialId() {
        // Given
        String socialId = "";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, 2L, socialId, provider);

        // When
        userService.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getSocialId()).isEqualTo(socialId);
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 - null 소셜 ID")
    void shouldHandleUserBannedEvent_WhenNullSocialId() {
        // Given
        String socialId = null;
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, 2L, socialId, provider);

        // When
        userService.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getSocialId()).isNull();
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("대량 사용자 차단 이벤트 처리")
    void shouldHandleBulkUserBannedEvents() {
        // Given
        int eventCount = 100;
        SocialProvider provider = SocialProvider.KAKAO;

        // When - 100개의 사용자 차단 이벤트 처리
        for (int i = 0; i < eventCount; i++) {
            UserBannedEvent event = new UserBannedEvent(this, (long) i, "user_" + i, provider);
            userService.handleUserBannedEvent(event);
        }

        // Then
        verify(userCommandPort, times(eventCount)).save(any(BlackList.class));
    }

    @Test
    @DisplayName("특수 문자 포함 소셜 ID 처리")
    void shouldHandleUserBannedEvent_WhenSpecialCharactersSocialId() {
        // Given
        String socialId = "user@#$%^&*()_+{}|:<>?[];',./";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, 2L, socialId, provider);

        // When
        userService.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getSocialId()).isEqualTo(socialId);
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("매우 긴 소셜 ID 처리")
    void shouldHandleUserBannedEvent_WhenVeryLongSocialId() {
        // Given
        String socialId = "a".repeat(1000); // 1000자 길이의 소셜 ID
        SocialProvider provider = SocialProvider.GOOGLE;
        UserBannedEvent event = new UserBannedEvent(this, 2L, socialId, provider);

        // When
        userService.handleUserBannedEvent(event);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getSocialId()).isEqualTo(socialId);
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("동일한 사용자 중복 차단 이벤트 처리")
    void shouldHandleDuplicateUserBannedEvents() {
        // Given
        String socialId = "duplicate_user";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, 2L, socialId, provider);

        // When - 동일한 사용자에 대해 여러 번 차단 이벤트 발생
        userService.handleUserBannedEvent(event);
        userService.handleUserBannedEvent(event);
        userService.handleUserBannedEvent(event);

        // Then - 각각의 이벤트에 대해 블랙리스트 생성됨 (중복 검사는 비즈니스 로직에서 처리)
        verify(userCommandPort, times(3)).save(any(BlackList.class));
    }

    @Test
    @DisplayName("모든 Provider 타입에 대한 차단 이벤트 처리")
    void shouldHandleUserBannedEvent_ForAllProviders() {
        // Given & When & Then
        SocialProvider[] providers = SocialProvider.values();
        
        for (int i = 0; i < providers.length; i++) {
            SocialProvider provider = providers[i];
            String socialId = "user_" + provider.name().toLowerCase();
            
            UserBannedEvent event = new UserBannedEvent(this, (long) i, socialId, provider);

            userService.handleUserBannedEvent(event);

            // 각 provider에 대해 저장이 호출되었는지 확인
            verify(userCommandPort, times(i + 1)).save(any(BlackList.class));
        }
    }

    @Test
    @DisplayName("BlackList 생성 메서드 호출 검증")
    void shouldVerifyBlackListCreation() {
        // Given
        String socialId = "test_user";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, 2L, socialId, provider);

        // When
        userService.handleUserBannedEvent(event);

        // Then - BlackList.createBlackList가 올바른 파라미터로 호출되는지 검증
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(userCommandPort).save(blackListCaptor.capture());

        BlackList savedBlackList = blackListCaptor.getValue();
        // BlackList 객체가 정상적으로 생성되었는지 확인
        assertThat(savedBlackList).isNotNull();
        assertThat(savedBlackList.getSocialId()).isEqualTo(socialId);
        assertThat(savedBlackList.getProvider()).isEqualTo(provider);
    }
}