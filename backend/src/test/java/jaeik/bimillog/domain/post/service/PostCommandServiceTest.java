package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostCommandService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.global.application.port.out.GlobalUserQueryPort;
import jaeik.bimillog.testutil.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static jaeik.bimillog.testutil.TestAssertionUtils.*;
import static jaeik.bimillog.testutil.TestMockUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock private PostCommandPort postCommandPort;
    @Mock private PostQueryPort postQueryPort;
    @Mock private GlobalUserQueryPort globalUserQueryPort;
    @Mock private RedisPostCommandPort redisPostCommandPort;
    @Mock private User user;
    @Mock private Post post;

    @InjectMocks
    private PostCommandService postCommandService;

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 123L;
    private static final String TITLE = "테스트 제목";
    private static final String CONTENT = "테스트 내용";
    private static final Integer PASSWORD = 1234;

    @Test
    @DisplayName("게시글 작성 - 성공")
    void shouldWritePost_WhenValidInput() {
        // Given
        given(globalUserQueryPort.getReferenceById(USER_ID)).willReturn(user);
        given(postCommandPort.create(any(Post.class))).willReturn(post);
        given(post.getId()).willReturn(POST_ID);

        // When
        Long result = postCommandService.writePost(USER_ID, TITLE, CONTENT, PASSWORD);

        // Then
        assertThat(result).isEqualTo(POST_ID);
        verifyCalledOnceAndNoMore(globalUserQueryPort, () -> verify(globalUserQueryPort).getReferenceById(USER_ID));
        verifyCalledOnceAndNoMore(postCommandPort, () -> verify(postCommandPort).create(any(Post.class)));
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void shouldUpdatePost_WhenValidAuthor() {
        // Given
        given(postQueryPort.findById(POST_ID)).willReturn(post);
        given(post.isAuthor(USER_ID)).willReturn(true);

        // When
        postCommandService.updatePost(USER_ID, POST_ID, "수정된 제목", "수정된 내용");

        // Then
        verify(postQueryPort).findById(POST_ID);
        verify(post).isAuthor(USER_ID);
        verify(post).updatePost("수정된 제목", "수정된 내용");
        verify(redisPostCommandPort).deleteCache(null, POST_ID);
        verifyPortInteractions(postQueryPort, redisPostCommandPort);
    }

    @Test
    @DisplayName("게시글 수정 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForUpdate() {
        // Given
        Long invalidPostId = 999L;
        given(postQueryPort.findById(invalidPostId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertPostException(() -> postCommandService.updatePost(USER_ID, invalidPostId, "title", "content"),
                           PostErrorCode.POST_NOT_FOUND);

        verify(postQueryPort).findById(invalidPostId);
        verify(redisPostCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 수정 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForUpdate() {
        // Given
        given(postQueryPort.findById(POST_ID)).willReturn(post);
        given(post.isAuthor(USER_ID)).willReturn(false);

        // When & Then
        assertPostException(() -> postCommandService.updatePost(USER_ID, POST_ID, "title", "content"),
                           PostErrorCode.FORBIDDEN);

        verify(postQueryPort).findById(POST_ID);
        verify(post).isAuthor(USER_ID);
        verify(post, never()).updatePost(any(), any());
        verify(redisPostCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void shouldDeletePost_WhenValidAuthor() {
        // Given
        given(postQueryPort.findById(POST_ID)).willReturn(post);
        given(post.isAuthor(USER_ID)).willReturn(true);
        given(post.getTitle()).willReturn("삭제될 게시글");

        // When
        postCommandService.deletePost(USER_ID, POST_ID);

        // Then
        verify(postQueryPort).findById(POST_ID);
        verify(post).isAuthor(USER_ID);
        verify(postCommandPort).delete(post);
        verify(redisPostCommandPort).deleteCache(null, POST_ID);
        verifyPortInteractions(postQueryPort, postCommandPort, redisPostCommandPort);
    }

    @Test
    @DisplayName("게시글 삭제 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForDelete() {
        // Given
        Long invalidPostId = 999L;
        given(postQueryPort.findById(invalidPostId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertPostException(() -> postCommandService.deletePost(USER_ID, invalidPostId),
                           PostErrorCode.POST_NOT_FOUND);

        verify(postQueryPort).findById(invalidPostId);
        verify(postCommandPort, never()).delete(any());
        verify(redisPostCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 삭제 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForDelete() {
        // Given
        given(postQueryPort.findById(POST_ID)).willReturn(post);
        given(post.isAuthor(USER_ID)).willReturn(false);

        // When & Then
        assertPostException(() -> postCommandService.deletePost(USER_ID, POST_ID),
                           PostErrorCode.FORBIDDEN);

        verify(postQueryPort).findById(POST_ID);
        verify(post).isAuthor(USER_ID);
        verify(postCommandPort, never()).delete(any());
        verify(redisPostCommandPort, never()).deleteCache(any(), any());
    }


    @Test
    @DisplayName("게시글 수정 - 캐시 삭제 실패")
    void shouldUpdatePostEvenWhenCacheDeleteFails() {
        // Given
        given(postQueryPort.findById(POST_ID)).willReturn(post);
        given(post.isAuthor(USER_ID)).willReturn(true);
        doThrow(new RuntimeException("Cache delete failed")).when(redisPostCommandPort).deleteCache(null, POST_ID);

        // When & Then
        assertRuntimeException(() -> postCommandService.updatePost(USER_ID, POST_ID, "title", "content"),
                              "Cache delete failed");

        // 게시글 수정은 완료되지만 캐시 삭제에서 실패
        verify(post).updatePost("title", "content");
        verify(redisPostCommandPort).deleteCache(null, POST_ID);
    }
}