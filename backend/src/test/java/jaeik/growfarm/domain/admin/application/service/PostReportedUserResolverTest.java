package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
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
 * <h2>PostReportedUserResolver 단위 테스트</h2>
 * <p>게시글 신고 사용자 해결사의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostReportedUserResolver 단위 테스트")
class PostReportedUserResolverTest {

    @Mock
    private PostQueryUseCase postQueryUseCase;

    @InjectMocks
    private PostReportedUserResolver postReportedUserResolver;

    private Post testPost;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(100L)
                .userName("testUser")
                .socialId("kakao123")
                .build();

        testPost = Post.builder()
                .id(200L)
                .title("테스트 게시글")
                .content("테스트 내용")
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("유효한 게시글 ID로 사용자 해결 시 성공")
    void shouldResolveUser_WhenValidPostId() {
        // Given
        Long postId = 200L;
        given(postQueryUseCase.findById(postId)).willReturn(Optional.of(testPost));

        // When
        User result = postReportedUserResolver.resolve(postId);

        // Then
        assertThat(result).isEqualTo(testUser);
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUserName()).isEqualTo("testUser");
        verify(postQueryUseCase).findById(postId);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 ID로 해결 시 POST_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenPostNotFound() {
        // Given
        Long nonExistentPostId = 999L;
        given(postQueryUseCase.findById(nonExistentPostId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postReportedUserResolver.resolve(nonExistentPostId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryUseCase).findById(nonExistentPostId);
    }

    @Test
    @DisplayName("지원하는 신고 유형이 POST인지 확인")
    void shouldSupportPostReportType() {
        // When
        ReportType supportedType = postReportedUserResolver.supports();

        // Then
        assertThat(supportedType).isEqualTo(ReportType.POST);
    }

    @Test
    @DisplayName("게시글이 존재하지만 사용자가 null인 경우 처리")
    void shouldHandlePostWithNullUser() {
        // Given
        Long postId = 200L;
        Post postWithNullUser = Post.builder()
                .id(postId)
                .title("사용자가 없는 게시글")
                .content("내용")
                .user(null)
                .build();
        
        given(postQueryUseCase.findById(postId)).willReturn(Optional.of(postWithNullUser));

        // When
        User result = postReportedUserResolver.resolve(postId);

        // Then
        assertThat(result).isNull();
        verify(postQueryUseCase).findById(postId);
    }

    @Test
    @DisplayName("다양한 게시글 ID로 사용자 해결 테스트")
    void shouldResolveUsersForDifferentPostIds() {
        // Given
        Long[] postIds = {1L, 2L, 3L};
        User[] users = {
                User.builder().id(101L).userName("user1").build(),
                User.builder().id(102L).userName("user2").build(),
                User.builder().id(103L).userName("user3").build()
        };

        for (int i = 0; i < postIds.length; i++) {
            Post post = Post.builder()
                    .id(postIds[i])
                    .title("게시글 " + (i + 1))
                    .content("내용 " + (i + 1))
                    .user(users[i])
                    .build();
            given(postQueryUseCase.findById(postIds[i])).willReturn(Optional.of(post));
        }

        // When & Then
        for (int i = 0; i < postIds.length; i++) {
            User result = postReportedUserResolver.resolve(postIds[i]);
            assertThat(result).isEqualTo(users[i]);
            assertThat(result.getId()).isEqualTo(users[i].getId());
            assertThat(result.getUserName()).isEqualTo(users[i].getUserName());
        }

        // 모든 호출이 이루어졌는지 검증
        for (Long postId : postIds) {
            verify(postQueryUseCase).findById(postId);
        }
    }

    @Test
    @DisplayName("공지사항 게시글 사용자 해결 테스트")
    void shouldResolveUserForNoticePost() {
        // Given
        Long noticePostId = 500L;
        User adminUser = User.builder()
                .id(1L)
                .userName("관리자")
                .socialId("admin")
                .build();

        Post noticePost = Post.builder()
                .id(noticePostId)
                .title("[공지] 중요한 공지사항")
                .content("공지사항 내용")
                .user(adminUser)
                .build();

        given(postQueryUseCase.findById(noticePostId)).willReturn(Optional.of(noticePost));

        // When
        User result = postReportedUserResolver.resolve(noticePostId);

        // Then
        assertThat(result).isEqualTo(adminUser);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserName()).isEqualTo("관리자");
        verify(postQueryUseCase).findById(noticePostId);
    }

    @Test
    @DisplayName("삭제된 게시글 사용자 해결 실패 테스트")
    void shouldFailToResolveDeletedPost() {
        // Given
        Long deletedPostId = 600L;
        given(postQueryUseCase.findById(deletedPostId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postReportedUserResolver.resolve(deletedPostId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryUseCase).findById(deletedPostId);
    }
}