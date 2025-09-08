package jaeik.bimillog.infrastructure.outadapter.auth.persistence.user;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>LoadUserAdapter 단위 테스트</h2>
 * <p>사용자 조회 어댑터의 비즈니스 로직 위주로 테스트</p>
 * <p>헥사고날 아키텍처의 도메인 간 의존성 어댑터 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class LoadNotificationToPaperToUserAdapterTest {

    @Mock private UserQueryUseCase userQueryUseCase;

    @InjectMocks private LoadUserAdapter loadUserAdapter;

    @Test
    @DisplayName("사용자 조회 - 존재하는 사용자 ID로 조회")
    void shouldReturnUser_WhenUserExists() {
        // Given: 존재하는 사용자 ID와 사용자 데이터
        Long existingUserId = 1L;
        User expectedUser = User.builder()
                .id(existingUserId)
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .socialNickname("testUser")
                .thumbnailImage("https://example.com/profile.jpg")
                .role(UserRole.USER)
                .build();
        
        given(userQueryUseCase.findById(existingUserId)).willReturn(Optional.of(expectedUser));

        // When: 사용자 조회
        User result = loadUserAdapter.findById(existingUserId);

        // Then: 올바른 사용자 반환 검증
        assertThat(result).isEqualTo(expectedUser);
        assertThat(result.getId()).isEqualTo(existingUserId);
        assertThat(result.getSocialNickname()).isEqualTo("testUser");
        assertThat(result.getProvider()).isEqualTo(SocialProvider.KAKAO);
        
        verify(userQueryUseCase).findById(existingUserId);
    }

    @Test
    @DisplayName("사용자 조회 - 존재하지 않는 사용자 ID로 조회")
    void shouldThrowException_WhenUserNotExists() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentUserId = 999L;
        given(userQueryUseCase.findById(nonExistentUserId)).willReturn(Optional.empty());

        // When & Then: 예외 발생 검증
        assertThatThrownBy(() -> loadUserAdapter.findById(nonExistentUserId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
        
        verify(userQueryUseCase).findById(nonExistentUserId);
    }

    @Test
    @DisplayName("사용자 조회 - null 사용자 ID로 조회")
    void shouldThrowException_WhenUserIdIsNull() {
        // Given: null 사용자 ID
        Long nullUserId = null;
        given(userQueryUseCase.findById(nullUserId)).willReturn(Optional.empty());

        // When & Then: 예외 발생 검증
        assertThatThrownBy(() -> loadUserAdapter.findById(nullUserId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
        
        verify(userQueryUseCase).findById(nullUserId);
    }

    @Test
    @DisplayName("사용자 조회 - 음수 사용자 ID로 조회")
    void shouldThrowException_WhenUserIdIsNegative() {
        // Given: 음수 사용자 ID
        Long negativeUserId = -1L;
        given(userQueryUseCase.findById(negativeUserId)).willReturn(Optional.empty());

        // When & Then: 예외 발생 검증
        assertThatThrownBy(() -> loadUserAdapter.findById(negativeUserId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
        
        verify(userQueryUseCase).findById(negativeUserId);
    }

    @Test
    @DisplayName("사용자 조회 - 0 사용자 ID로 조회")
    void shouldThrowException_WhenUserIdIsZero() {
        // Given: 0 사용자 ID
        Long zeroUserId = 0L;
        given(userQueryUseCase.findById(zeroUserId)).willReturn(Optional.empty());

        // When & Then: 예외 발생 검증
        assertThatThrownBy(() -> loadUserAdapter.findById(zeroUserId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
        
        verify(userQueryUseCase).findById(zeroUserId);
    }

    @Test
    @DisplayName("사용자 조회 - 매우 큰 사용자 ID로 조회")
    void shouldThrowException_WhenUserIdIsVeryLarge() {
        // Given: 매우 큰 사용자 ID
        Long largeUserId = Long.MAX_VALUE;
        given(userQueryUseCase.findById(largeUserId)).willReturn(Optional.empty());

        // When & Then: 예외 발생 검증
        assertThatThrownBy(() -> loadUserAdapter.findById(largeUserId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
        
        verify(userQueryUseCase).findById(largeUserId);
    }
}