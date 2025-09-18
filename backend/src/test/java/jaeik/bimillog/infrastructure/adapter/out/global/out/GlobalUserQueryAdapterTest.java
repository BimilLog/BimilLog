package jaeik.bimillog.infrastructure.adapter.out.global.out;

import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.global.GlobalUserQueryAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>GlobalUserQueryAdapter 단위 테스트</h2>
 * <p>사용자 조회 공용 어댑터의 동작을 검증하는 단위 테스트입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalUserQueryAdapter 단위 테스트")
class GlobalUserQueryAdapterTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @Mock
    private User user;

    @InjectMocks
    private GlobalUserQueryAdapter globalUserQueryAdapter;

    @Test
    @DisplayName("사용자 ID로 조회 - 사용자 존재")
    void shouldReturnUser_WhenUserExists() {
        // Given
        Long userId = 1L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(user));

        // When
        Optional<User> result = globalUserQueryAdapter.findById(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
        verify(userQueryUseCase, times(1)).findById(userId);
    }

    @Test
    @DisplayName("사용자 ID로 조회 - 사용자 없음")
    void shouldReturnEmpty_WhenUserNotExists() {
        // Given
        Long userId = 999L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.empty());

        // When
        Optional<User> result = globalUserQueryAdapter.findById(userId);

        // Then
        assertThat(result).isEmpty();
        verify(userQueryUseCase, times(1)).findById(userId);
    }

    @Test
    @DisplayName("사용자명으로 조회 - 사용자 존재")
    void shouldReturnUser_WhenUserNameExists() {
        // Given
        String userName = "testUser";
        given(userQueryUseCase.findByUserName(userName)).willReturn(Optional.of(user));

        // When
        Optional<User> result = globalUserQueryAdapter.findByUserName(userName);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
        verify(userQueryUseCase, times(1)).findByUserName(userName);
    }

    @Test
    @DisplayName("사용자명으로 조회 - 사용자 없음")
    void shouldReturnEmpty_WhenUserNameNotExists() {
        // Given
        String userName = "nonExistentUser";
        given(userQueryUseCase.findByUserName(userName)).willReturn(Optional.empty());

        // When
        Optional<User> result = globalUserQueryAdapter.findByUserName(userName);

        // Then
        assertThat(result).isEmpty();
        verify(userQueryUseCase, times(1)).findByUserName(userName);
    }

    @Test
    @DisplayName("사용자명 존재 여부 확인 - 존재함")
    void shouldReturnTrue_WhenUserNameExists() {
        // Given
        String userName = "existingUser";
        given(userQueryUseCase.existsByUserName(userName)).willReturn(true);

        // When
        boolean result = globalUserQueryAdapter.existsByUserName(userName);

        // Then
        assertThat(result).isTrue();
        verify(userQueryUseCase, times(1)).existsByUserName(userName);
    }

    @Test
    @DisplayName("사용자명 존재 여부 확인 - 존재하지 않음")
    void shouldReturnFalse_WhenUserNameNotExists() {
        // Given
        String userName = "nonExistentUser";
        given(userQueryUseCase.existsByUserName(userName)).willReturn(false);

        // When
        boolean result = globalUserQueryAdapter.existsByUserName(userName);

        // Then
        assertThat(result).isFalse();
        verify(userQueryUseCase, times(1)).existsByUserName(userName);
    }

    @Test
    @DisplayName("JPA 프록시 참조 조회 - 성공")
    void shouldReturnProxyReference_WhenUserIdProvided() {
        // Given
        Long userId = 1L;
        given(userQueryUseCase.getReferenceById(userId)).willReturn(user);

        // When
        User result = globalUserQueryAdapter.getReferenceById(userId);

        // Then
        assertThat(result).isEqualTo(user);
        verify(userQueryUseCase, times(1)).getReferenceById(userId);
    }
}