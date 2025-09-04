package jaeik.bimillog.infrastructure.adapter.user.in.listener;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * <h2>블랙리스트 이벤트 리스너 테스트</h2>
 * <p>BlacklistEventListener의 단위 테스트</p>
 * <p>사용자 제재 시 BAN 역할 변경 및 블랙리스트 추가 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("블랙리스트 이벤트 리스너 테스트")
class BlacklistEventListenerTest {

    @Mock
    private UserCommandUseCase userCommandUseCase;

    @InjectMocks
    private BlacklistEventListener blacklistEventListener;

    @Test
    @DisplayName("사용자 제재 이벤트 처리 - 정상 케이스")
    void handleUserBannedEvent_Success() {
        // Given
        Long userId = 1L;
        String socialId = "12345";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);

        // When
        blacklistEventListener.handleUserBannedEvent(event);

        // Then
        verify(userCommandUseCase).banUser(userId);
        verify(userCommandUseCase).addToBlacklist(userId);
    }

    @Test
    @DisplayName("사용자 제재 이벤트 처리 - banUser 실행 순서 확인")
    void handleUserBannedEvent_VerifyExecutionOrder() {
        // Given
        Long userId = 2L;
        String socialId = "google123";
        SocialProvider provider = SocialProvider.GOOGLE;
        UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);

        // When
        blacklistEventListener.handleUserBannedEvent(event);

        // Then - banUser가 먼저, addToBlacklist가 나중에 호출되는지 확인
        var inOrder = inOrder(userCommandUseCase);
        inOrder.verify(userCommandUseCase).banUser(userId);
        inOrder.verify(userCommandUseCase).addToBlacklist(userId);
    }

    @Test
    @DisplayName("사용자 제재 이벤트 처리 - banUser 예외 발생 시 처리")
    void handleUserBannedEvent_WhenBanUserThrowsException() {
        // Given
        Long userId = 3L;
        String socialId = "kakao123";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);
        
        RuntimeException expectedException = new RuntimeException("사용자 제재 실패");
        doThrow(expectedException).when(userCommandUseCase).banUser(userId);

        // When & Then
        try {
            blacklistEventListener.handleUserBannedEvent(event);
        } catch (RuntimeException e) {
            // 예외가 재발생되어야 함
        }

        // banUser만 호출되고 addToBlacklist는 호출되지 않아야 함
        verify(userCommandUseCase).banUser(userId);
        verify(userCommandUseCase, never()).addToBlacklist(userId);
    }

    @Test
    @DisplayName("사용자 제재 이벤트 처리 - addToBlacklist 예외 발생 시 처리")
    void handleUserBannedEvent_WhenAddToBlacklistThrowsException() {
        // Given
        Long userId = 4L;
        String socialId = "kakao456";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);
        
        RuntimeException expectedException = new RuntimeException("블랙리스트 추가 실패");
        doThrow(expectedException).when(userCommandUseCase).addToBlacklist(userId);

        // When & Then
        try {
            blacklistEventListener.handleUserBannedEvent(event);
        } catch (RuntimeException e) {
            // 예외가 재발생되어야 함
        }

        // 둘 다 호출되어야 함 (banUser 성공 후 addToBlacklist에서 실패)
        verify(userCommandUseCase).banUser(userId);
        verify(userCommandUseCase).addToBlacklist(userId);
    }

    @Test
    @DisplayName("사용자 제재 이벤트 처리 - 다양한 Provider 타입")
    void handleUserBannedEvent_WithDifferentProviders() {
        // Given & When & Then
        SocialProvider[] providers = SocialProvider.values();
        
        for (int i = 0; i < providers.length; i++) {
            SocialProvider provider = providers[i];
            String socialId = "user_" + provider.name().toLowerCase();
            Long userId = (long) (i + 1);
            
            UserBannedEvent event = new UserBannedEvent(this, userId, socialId, provider);
            blacklistEventListener.handleUserBannedEvent(event);
        }

        // Then - 각 provider별로 제재 및 블랙리스트 추가가 수행됨
        verify(userCommandUseCase, times(providers.length)).banUser(any());
        verify(userCommandUseCase, times(providers.length)).addToBlacklist(any());
    }
}