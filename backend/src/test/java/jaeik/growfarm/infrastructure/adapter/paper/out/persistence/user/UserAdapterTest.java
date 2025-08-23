package jaeik.growfarm.infrastructure.adapter.paper.out.persistence.user;

import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>Paper 도메인의 UserAdapter 테스트</h2>
 * <p>Paper 도메인에서 User 도메인 UserQueryUseCase를 통해 사용자를 조회하는 어댑터의 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserAdapterTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @InjectMocks
    private UserAdapter userAdapter;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userName("testUser")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - 존재하는 사용자 이름으로 사용자 조회")
    void shouldFindUserByUserName_WhenUserExists() {
        // Given
        String userName = "testUser";
        given(userQueryUseCase.findByUserName(userName)).willReturn(Optional.of(testUser));

        // When
        Optional<User> result = userAdapter.findByUserName(userName);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserName()).isEqualTo(userName);
        verify(userQueryUseCase, times(1)).findByUserName(userName);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 사용자 이름으로 사용자 조회")
    void shouldReturnEmpty_WhenUserNotExists() {
        // Given
        String userName = "nonExistentUser";
        given(userQueryUseCase.findByUserName(userName)).willReturn(Optional.empty());

        // When
        Optional<User> result = userAdapter.findByUserName(userName);

        // Then
        assertThat(result).isEmpty();
        verify(userQueryUseCase, times(1)).findByUserName(userName);
    }

    @Test
    @DisplayName("경계값 - null 사용자 이름으로 사용자 조회")
    void shouldReturnEmpty_WhenNullUserNameProvided() {
        // Given
        String userName = null;
        given(userQueryUseCase.findByUserName(null)).willReturn(Optional.empty());

        // When
        Optional<User> result = userAdapter.findByUserName(userName);

        // Then
        assertThat(result).isEmpty();
        verify(userQueryUseCase, times(1)).findByUserName(null);
    }

    @Test
    @DisplayName("정상 케이스 - 존재하는 사용자 이름으로 존재 여부 확인")
    void shouldReturnTrue_WhenUserExistsForExistsByUserName() {
        // Given
        String userName = "testUser";
        given(userQueryUseCase.existsByUserName(userName)).willReturn(true);

        // When
        boolean result = userAdapter.existsByUserName(userName);

        // Then
        assertThat(result).isTrue();
        verify(userQueryUseCase, times(1)).existsByUserName(userName);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 사용자 이름으로 존재 여부 확인")
    void shouldReturnFalse_WhenUserNotExistsForExistsByUserName() {
        // Given
        String userName = "nonExistentUser";
        given(userQueryUseCase.existsByUserName(userName)).willReturn(false);

        // When
        boolean result = userAdapter.existsByUserName(userName);

        // Then
        assertThat(result).isFalse();
        verify(userQueryUseCase, times(1)).existsByUserName(userName);
    }

    @Test
    @DisplayName("경계값 - null 사용자 이름으로 존재 여부 확인")
    void shouldReturnFalse_WhenNullUserNameProvidedForExistsByUserName() {
        // Given
        String userName = null;
        given(userQueryUseCase.existsByUserName(null)).willReturn(false);

        // When
        boolean result = userAdapter.existsByUserName(userName);

        // Then
        assertThat(result).isFalse();
        verify(userQueryUseCase, times(1)).existsByUserName(null);
    }

    @Test
    @DisplayName("통합 테스트 - findByUserName과 existsByUserName의 일관성 확인")
    void shouldBeConsistent_BetweenFindAndExistsByUserName() {
        // Given
        String existingUserName = "existingUser";
        User existingUser = User.builder().id(2L).userName(existingUserName).role(UserRole.USER).build();

        given(userQueryUseCase.findByUserName(existingUserName)).willReturn(Optional.of(existingUser));
        given(userQueryUseCase.existsByUserName(existingUserName)).willReturn(true);

        String nonExistentUserName = "nonExistentUser";
        given(userQueryUseCase.findByUserName(nonExistentUserName)).willReturn(Optional.empty());
        given(userQueryUseCase.existsByUserName(nonExistentUserName)).willReturn(false);

        // When & Then - Existing User
        Optional<User> foundUser = userAdapter.findByUserName(existingUserName);
        boolean exists = userAdapter.existsByUserName(existingUserName);

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserName()).isEqualTo(existingUserName);
        assertThat(exists).isTrue();

        // When & Then - Non-Existent User
        Optional<User> notFoundUser = userAdapter.findByUserName(nonExistentUserName);
        boolean notExists = userAdapter.existsByUserName(nonExistentUserName);

        assertThat(notFoundUser).isEmpty();
        assertThat(notExists).isFalse();

        verify(userQueryUseCase, times(2)).findByUserName(anyString()); // 2 calls for existingUser and nonExistentUser
        verify(userQueryUseCase, times(2)).existsByUserName(anyString()); // 2 calls for existingUser and nonExistentUser
    }
}
