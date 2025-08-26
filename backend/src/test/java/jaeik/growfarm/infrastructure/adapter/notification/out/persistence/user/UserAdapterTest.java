package jaeik.growfarm.infrastructure.adapter.notification.out.persistence.user;

import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.common.entity.SocialProvider;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * <h2>UserAdapter 테스트</h2>
 * <p>Notification 도메인의 User 어댑터 단위 테스트</p>
 * <p>헥사고날 아키텍처에서 도메인 간 통신을 위한 어댑터 동작 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAdapter 테스트")
class UserAdapterTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @InjectMocks
    private UserAdapter userAdapter;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Given: 테스트용 사용자 설정
        Setting testSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        testUser = User.builder()
                .id(1L)
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(testSetting)
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 ID로 조회 성공")
    void shouldFindUserById_WhenValidUserIdProvided() {
        // Given: UseCase에서 사용자를 반환하도록 설정
        Long userId = 1L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When: 사용자 조회
        Optional<User> result = userAdapter.findById(userId);

        // Then: 조회 결과 검증
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getUserName()).isEqualTo("테스트유저");
        assertThat(result.get().getSocialId()).isEqualTo("12345");
        assertThat(result.get().getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(result.get().getRole()).isEqualTo(UserRole.USER);

        // Verify: UseCase 호출 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 사용자 ID로 조회")
    void shouldReturnEmpty_WhenUserNotFound() {
        // Given: UseCase에서 빈 Optional 반환하도록 설정
        Long userId = 999L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.empty());

        // When: 존재하지 않는 사용자 조회
        Optional<User> result = userAdapter.findById(userId);

        // Then: 빈 Optional 반환 검증
        assertThat(result).isEmpty();

        // Verify: UseCase 호출 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("경계 케이스 - null 사용자 ID로 조회")
    void shouldReturnEmpty_WhenUserIdIsNull() {
        // Given: UseCase에서 null ID에 대해 빈 Optional 반환하도록 설정
        Long userId = null;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.empty());

        // When: null ID로 사용자 조회
        Optional<User> result = userAdapter.findById(userId);

        // Then: 빈 Optional 반환 검증
        assertThat(result).isEmpty();

        // Verify: UseCase 호출 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("경계 케이스 - 0 값 사용자 ID로 조회")
    void shouldReturnEmpty_WhenUserIdIsZero() {
        // Given: UseCase에서 0 ID에 대해 빈 Optional 반환하도록 설정
        Long userId = 0L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.empty());

        // When: 0 ID로 사용자 조회
        Optional<User> result = userAdapter.findById(userId);

        // Then: 빈 Optional 반환 검증
        assertThat(result).isEmpty();

        // Verify: UseCase 호출 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("경계 케이스 - 음수 사용자 ID로 조회")
    void shouldReturnEmpty_WhenUserIdIsNegative() {
        // Given: UseCase에서 음수 ID에 대해 빈 Optional 반환하도록 설정
        Long userId = -1L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.empty());

        // When: 음수 ID로 사용자 조회
        Optional<User> result = userAdapter.findById(userId);

        // Then: 빈 Optional 반환 검증
        assertThat(result).isEmpty();

        // Verify: UseCase 호출 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("경계 케이스 - 매우 큰 사용자 ID로 조회")
    void shouldReturnEmpty_WhenUserIdIsVeryLarge() {
        // Given: UseCase에서 큰 ID에 대해 빈 Optional 반환하도록 설정
        Long userId = Long.MAX_VALUE;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.empty());

        // When: 매우 큰 ID로 사용자 조회
        Optional<User> result = userAdapter.findById(userId);

        // Then: 빈 Optional 반환 검증
        assertThat(result).isEmpty();

        // Verify: UseCase 호출 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 설정이 포함된 사용자 조회")
    void shouldFindUserWithSettings_WhenValidUserIdProvided() {
        // Given: 설정이 포함된 사용자
        Setting customSetting = Setting.builder()
                .messageNotification(false)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();

        User userWithCustomSetting = User.builder()
                .id(2L)
                .socialId("67890")
                .provider(SocialProvider.KAKAO)
                .userName("설정테스트유저")
                .role(UserRole.USER)
                .setting(customSetting)
                .build();

        Long userId = 2L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(userWithCustomSetting));

        // When: 사용자 조회
        Optional<User> result = userAdapter.findById(userId);

        // Then: 사용자와 설정 정보 검증
        assertThat(result).isPresent();
        User foundUser = result.get();
        assertThat(foundUser.getId()).isEqualTo(2L);
        assertThat(foundUser.getUserName()).isEqualTo("설정테스트유저");
        assertThat(foundUser.getSetting()).isNotNull();
        assertThat(foundUser.getSetting().isMessageNotification()).isFalse();
        assertThat(foundUser.getSetting().isCommentNotification()).isTrue();
        assertThat(foundUser.getSetting().isPostFeaturedNotification()).isFalse();

        // Verify: UseCase 호출 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - 관리자 사용자 조회")
    void shouldFindAdminUser_WhenValidAdminUserIdProvided() {
        // Given: 관리자 사용자
        User adminUser = User.builder()
                .id(3L)
                .socialId("admin123")
                .provider(SocialProvider.KAKAO)
                .userName("관리자")
                .role(UserRole.ADMIN)
                .setting(testUser.getSetting())
                .build();

        Long userId = 3L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(adminUser));

        // When: 관리자 사용자 조회
        Optional<User> result = userAdapter.findById(userId);

        // Then: 관리자 사용자 검증
        assertThat(result).isPresent();
        User foundUser = result.get();
        assertThat(foundUser.getId()).isEqualTo(3L);
        assertThat(foundUser.getUserName()).isEqualTo("관리자");
        assertThat(foundUser.getRole()).isEqualTo(UserRole.ADMIN);

        // Verify: UseCase 호출 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }
}