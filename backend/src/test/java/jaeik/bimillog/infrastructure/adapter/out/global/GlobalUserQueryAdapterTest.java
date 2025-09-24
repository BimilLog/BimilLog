package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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
@DisplayName("GlobalUserQueryAdapter 단위 테스트")
class GlobalUserQueryAdapterTest extends BaseUnitTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @InjectMocks
    private GlobalUserQueryAdapter globalUserQueryAdapter;

    @Test
    @DisplayName("사용자 ID로 조회 - 사용자 존재")
    void shouldReturnUser_WhenUserExists() {
        // Given
        Long userId = 1L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When
        Optional<User> result = globalUserQueryAdapter.findById(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        assertThat(result.get().getUserName()).isEqualTo("testUser1");
        assertThat(result.get().getSocialNickname()).isEqualTo("테스트유저1");
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
        String userName = "testUser1";
        given(userQueryUseCase.findByUserName(userName)).willReturn(Optional.of(testUser));

        // When
        Optional<User> result = globalUserQueryAdapter.findByUserName(userName);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
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
        String userName = testUser.getUserName();
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
        given(userQueryUseCase.getReferenceById(userId)).willReturn(testUser);

        // When
        User result = globalUserQueryAdapter.getReferenceById(userId);

        // Then
        assertThat(result).isEqualTo(testUser);
        verify(userQueryUseCase, times(1)).getReferenceById(userId);
    }

    @Test
    @DisplayName("관리자 사용자 조회 - 성공")
    void shouldReturnAdminUser_WhenAdminUserExists() {
        // Given
        Long adminId = 2L;
        given(userQueryUseCase.findById(adminId)).willReturn(Optional.of(getAdminUser()));

        // When
        Optional<User> result = globalUserQueryAdapter.findById(adminId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(getAdminUser());
        assertThat(result.get().getRole()).isEqualTo(jaeik.bimillog.domain.user.entity.UserRole.ADMIN);
        verify(userQueryUseCase, times(1)).findById(adminId);
    }
}