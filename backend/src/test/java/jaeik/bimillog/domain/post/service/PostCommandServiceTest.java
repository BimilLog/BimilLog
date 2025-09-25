package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.application.service.PostCommandService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.global.application.port.out.GlobalUserQueryPort;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostCommandService 테스트</h2>
 * <p>게시글 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 *
 */
@DisplayName("PostCommandService 테스트")
class PostCommandServiceTest extends BaseUnitTest {

    @Mock
    private PostCommandPort postCommandPort;

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private GlobalUserQueryPort globalUserQueryPort;

    @Mock
    private RedisPostCommandPort redisPostCommandPort;

    @InjectMocks
    private PostCommandService postCommandService;

    @Test
    @DisplayName("게시글 작성 - 성공")
    void shouldWritePost_WhenValidInput() {
        // Given
        Long userId = 1L;
        Long expectedPostId = 123L;
        String title = "테스트 제목";
        String content = "테스트 내용";
        Integer password = 1234;

        Post createdPost = PostTestDataBuilder.withId(expectedPostId, PostTestDataBuilder.createPost(getTestUser(), title, content));

        given(globalUserQueryPort.getReferenceById(userId)).willReturn(getTestUser());
        given(postCommandPort.create(any(Post.class))).willReturn(createdPost);

        // When
        Long result = postCommandService.writePost(userId, title, content, password);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(globalUserQueryPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, times(1)).create(any(Post.class));
        verifyNoMoreInteractions(globalUserQueryPort, postCommandPort);
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void shouldUpdatePost_WhenValidAuthor() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        Post existingPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestUser(), "기존 제목", "기존 내용")));

        given(postQueryPort.findById(postId)).willReturn(existingPost);
        given(existingPost.isAuthor(userId)).willReturn(true);

        // When
        postCommandService.updatePost(userId, postId, "수정된 제목", "수정된 내용");

        // Then
        verify(postQueryPort, times(1)).findById(postId);
        verify(existingPost, times(1)).isAuthor(userId);
        verify(existingPost, times(1)).updatePost("수정된 제목", "수정된 내용");
        verify(redisPostCommandPort, times(1)).deleteCache(null, postId);
        verifyNoMoreInteractions(postQueryPort, postCommandPort, redisPostCommandPort);
    }

    @Test
    @DisplayName("게시글 수정 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 999L;

        given(postQueryPort.findById(postId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, "title", "content"))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postQueryPort, times(1)).findById(postId);
        verify(redisPostCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 수정 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        Post otherUserPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getOtherUser(), "다른 사용자 게시글", "내용")));

        given(postQueryPort.findById(postId)).willReturn(otherUserPost);
        given(otherUserPost.isAuthor(userId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, "title", "content"))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.FORBIDDEN);

        verify(postQueryPort, times(1)).findById(postId);
        verify(otherUserPost, times(1)).isAuthor(userId);
        verify(otherUserPost, never()).updatePost(anyString(), anyString());
        verify(redisPostCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void shouldDeletePost_WhenValidAuthor() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        String postTitle = "삭제될 게시글";

        Post postToDelete = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestUser(), postTitle, "내용")));

        given(postQueryPort.findById(postId)).willReturn(postToDelete);
        given(postToDelete.isAuthor(userId)).willReturn(true);
        given(postToDelete.getTitle()).willReturn(postTitle);

        // When
        postCommandService.deletePost(userId, postId);

        // Then
        verify(postQueryPort, times(1)).findById(postId);
        verify(postToDelete, times(1)).isAuthor(userId);
        verify(postCommandPort, times(1)).delete(postToDelete);
        verify(redisPostCommandPort, times(1)).deleteCache(null, postId);
        verifyNoMoreInteractions(postQueryPort, postCommandPort, redisPostCommandPort);
    }

    @Test
    @DisplayName("게시글 삭제 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForDelete() {
        // Given
        Long userId = 1L;
        Long postId = 999L;

        given(postQueryPort.findById(postId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(userId, postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postQueryPort, times(1)).findById(postId);
        verify(postCommandPort, never()).delete(any());
        verify(redisPostCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 삭제 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForDelete() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        Post otherUserPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getOtherUser(), "다른 사용자 게시글", "내용")));

        given(postQueryPort.findById(postId)).willReturn(otherUserPost);
        given(otherUserPost.isAuthor(userId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(userId, postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.FORBIDDEN);

        verify(postQueryPort, times(1)).findById(postId);
        verify(otherUserPost, times(1)).isAuthor(userId);
        verify(postCommandPort, never()).delete(any());
        verify(redisPostCommandPort, never()).deleteCache(any(), any());
    }


    @Test
    @DisplayName("게시글 수정 - 캐시 삭제 실패")
    void shouldUpdatePostEvenWhenCacheDeleteFails() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        Post existingPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestUser(), "제목", "내용")));

        given(postQueryPort.findById(postId)).willReturn(existingPost);
        given(existingPost.isAuthor(userId)).willReturn(true);
        doThrow(new RuntimeException("Cache delete failed")).when(redisPostCommandPort).deleteCache(null, postId);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, "title", "content"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cache delete failed");

        // 게시글 수정은 완료되지만 캐시 삭제에서 실패
        verify(existingPost, times(1)).updatePost("title", "content");
        verify(redisPostCommandPort, times(1)).deleteCache(null, postId);
    }
}