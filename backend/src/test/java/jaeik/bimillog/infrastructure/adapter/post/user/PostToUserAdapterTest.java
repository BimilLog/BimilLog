package jaeik.bimillog.infrastructure.adapter.post.user;

import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.adapter.post.out.user.PostToUserAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostToUserAdapter 단위 테스트</h2>
 * <p>Post 도메인에서 User 도메인으로의 어댑터 기능을 테스트합니다.</p>
 * <p>사용자 프록시 조회 기능 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class PostToUserAdapterTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    private PostToUserAdapter postToUserAdapter;

    private User testUser;

    @BeforeEach
    void setUp() {
        postToUserAdapter = new PostToUserAdapter(userQueryUseCase);
        
        testUser = User.builder()
                .id(1L)
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
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 ID로 프록시 조회")
    void shouldReturnUserProxy_WhenValidUserIdProvided() {
        // given
        Long userId = 1L;
        given(userQueryUseCase.getReferenceById(userId))
                .willReturn(testUser);

        // when
        User result = postToUserAdapter.getReferenceById(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUserName()).isEqualTo("testUser");
        
        verify(userQueryUseCase).getReferenceById(userId);
    }

    @Test
    @DisplayName("예외 상황 - UserQueryUseCase에서 예외 발생")
    void shouldPropagateException_WhenUserQueryUseCaseThrowsException() {
        // given
        Long userId = 999L;
        given(userQueryUseCase.getReferenceById(userId))
                .willThrow(new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> postToUserAdapter.getReferenceById(userId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
        
        verify(userQueryUseCase).getReferenceById(userId);
    }

}