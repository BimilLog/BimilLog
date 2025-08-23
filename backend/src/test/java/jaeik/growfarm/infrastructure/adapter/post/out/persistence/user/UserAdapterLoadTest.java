package jaeik.growfarm.infrastructure.adapter.post.out.persistence.user;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * <h2>UserAdapterLoad 테스트</h2>
 * <p>UserAdapterLoad의 사용자 정보 로드 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserAdapterLoadTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @InjectMocks
    private UserAdapterLoad userAdapterLoad;

    @Test
    @DisplayName("정상 케이스 - ID로 사용자 프록시 조회")
    void shouldReturnUserProxy_WhenValidUserIdProvided() {
        // Given: 유효한 사용자 ID와 mock User 객체
        Long userId = 1L;
        User mockUser = User.builder()
                .id(userId)
                .userName("testUser")
                .socialId("123456")
                .provider(SocialProvider.KAKAO)
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(Setting.builder()
                        .messageNotification(true)
                        .commentNotification(true)
                        .postFeaturedNotification(true)
                        .build())
                .build();

        when(userQueryUseCase.getReferenceById(userId)).thenReturn(mockUser);

        // When: getReferenceById 호출
        User resultUser = userAdapterLoad.getReferenceById(userId);

        // Then: UserQueryUseCase의 결과와 동일한 User 객체 반환 확인
        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getId()).isEqualTo(userId);
        assertThat(resultUser.getUserName()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 사용자 ID로 프록시 조회")
    void shouldReturnNullOrThrowException_WhenNonExistentUserIdProvided() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentUserId = 999L;
        when(userQueryUseCase.getReferenceById(nonExistentUserId)).thenReturn(null);

        // When: getReferenceById 호출
        User resultUser = userAdapterLoad.getReferenceById(nonExistentUserId);

        // Then: null 반환 확인 (혹은 UserQueryUseCase의 구현에 따라 예외 발생 여부 확인)
        assertThat(resultUser).isNull();
    }

    @Test
    @DisplayName("경계값 - null 사용자 ID로 프록시 조회")
    void shouldReturnNullOrThrowException_WhenNullUserIdProvided() {
        // Given: null 사용자 ID
        Long nullUserId = null;
        when(userQueryUseCase.getReferenceById(nullUserId)).thenReturn(null);

        // When: getReferenceById 호출
        User resultUser = userAdapterLoad.getReferenceById(nullUserId);

        // Then: null 반환 확인
        assertThat(resultUser).isNull();
    }
}
