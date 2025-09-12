package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.global.application.port.out.GlobalUserQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostCommandService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCommandService 테스트")
class PostCommandServiceTest {

    @Mock
    private PostCommandPort postCommandPort;

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private GlobalUserQueryPort globalUserQueryPort;

    @Mock
    private PostCacheCommandPort postCacheCommandPort;

    @Mock
    private User user;

    @Mock
    private Post post;

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

        given(globalUserQueryPort.getReferenceById(userId)).willReturn(user);
        given(postCommandPort.create(any(Post.class))).willReturn(post);
        given(post.getId()).willReturn(expectedPostId);

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

        given(postQueryPort.findById(postId)).willReturn(post);
        given(post.isAuthor(userId)).willReturn(true);

        // When
        postCommandService.updatePost(userId, postId, "수정된 제목", "수정된 내용");

        // Then
        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(post, times(1)).updatePost("수정된 제목", "수정된 내용");
        verify(postCacheCommandPort, times(1)).deleteCache(null, postId);
        verifyNoMoreInteractions(postQueryPort, postCommandPort, postCacheCommandPort);
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
        verify(postCacheCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 수정 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(postQueryPort.findById(postId)).willReturn(post);
        given(post.isAuthor(userId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, "title", "content"))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.FORBIDDEN);

        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(post, never()).updatePost(anyString(), anyString());
        verify(postCacheCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void shouldDeletePost_WhenValidAuthor() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        String postTitle = "삭제될 게시글";

        given(postQueryPort.findById(postId)).willReturn(post);
        given(post.isAuthor(userId)).willReturn(true);
        given(post.getTitle()).willReturn(postTitle);

        // When
        postCommandService.deletePost(userId, postId);

        // Then
        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(postCommandPort, times(1)).delete(post);
        verify(postCacheCommandPort, times(1)).deleteCache(null, postId);
        verifyNoMoreInteractions(postQueryPort, postCommandPort, postCacheCommandPort);
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
        verify(postCacheCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 삭제 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForDelete() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(postQueryPort.findById(postId)).willReturn(post);
        given(post.isAuthor(userId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(userId, postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.FORBIDDEN);

        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(postCommandPort, never()).delete(any());
        verify(postCacheCommandPort, never()).deleteCache(any(), any());
    }


    @Test
    @DisplayName("게시글 수정 - 캐시 삭제 실패")
    void shouldUpdatePostEvenWhenCacheDeleteFails() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(postQueryPort.findById(postId)).willReturn(post);
        given(post.isAuthor(userId)).willReturn(true);
        doThrow(new RuntimeException("Cache delete failed")).when(postCacheCommandPort).deleteCache(null, postId);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, "title", "content"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cache delete failed");

        // 게시글 수정은 완료되지만 캐시 삭제에서 실패
        verify(post, times(1)).updatePost("title", "content");
        verify(postCacheCommandPort, times(1)).deleteCache(null, postId);
    }
}