package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.growfarm.domain.post.application.port.out.PostCommandPort;
import jaeik.growfarm.domain.post.application.port.out.PostQueryPort;
import jaeik.growfarm.domain.post.application.port.out.UserLoadPort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.event.PostDeletedEvent;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.PostReqDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostCommandService 테스트</h2>
 * <p>게시글 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCommandService 테스트")
class PostCommandServiceTest {

    @Mock
    private PostCommandPort postCommandPort;

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private UserLoadPort userLoadPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("테스트 제목");
        postReqDTO.setContent("테스트 내용");

        given(userLoadPort.getReferenceById(userId)).willReturn(user);
        given(postCommandPort.save(any(Post.class))).willReturn(post);
        given(post.getId()).willReturn(expectedPostId);

        // When
        Long result = postCommandService.writePost(userId, postReqDTO);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(userLoadPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, times(1)).save(any(Post.class));
        verifyNoMoreInteractions(userLoadPort, postCommandPort);
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void shouldUpdatePost_WhenValidAuthor() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("수정된 제목");
        postReqDTO.setContent("수정된 내용");

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);

        // When
        postCommandService.updatePost(userId, postId, postReqDTO);

        // Then
        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(post, times(1)).updatePost(postReqDTO);
        verify(postCommandPort, times(1)).save(post);
        verify(postCacheCommandPort, times(1)).deleteFullPostCache(postId);
        verifyNoMoreInteractions(postQueryPort, postCommandPort, postCacheCommandPort);
    }

    @Test
    @DisplayName("게시글 수정 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 999L;
        PostReqDTO postReqDTO = new PostReqDTO();

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, postReqDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryPort, times(1)).findById(postId);
        verify(postCommandPort, never()).save(any());
        verify(postCacheCommandPort, never()).deleteFullPostCache(any());
    }

    @Test
    @DisplayName("게시글 수정 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqDTO postReqDTO = new PostReqDTO();

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, postReqDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(post, never()).updatePost(any());
        verify(postCommandPort, never()).save(any());
        verify(postCacheCommandPort, never()).deleteFullPostCache(any());
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void shouldDeletePost_WhenValidAuthor() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        String postTitle = "삭제될 게시글";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);
        given(post.getTitle()).willReturn(postTitle);

        // When
        postCommandService.deletePost(userId, postId);

        // Then
        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(postCommandPort, times(1)).delete(post);
        verify(postCacheCommandPort, times(1)).deleteFullPostCache(postId);
        verify(eventPublisher, times(1)).publishEvent(any(PostDeletedEvent.class));
        verifyNoMoreInteractions(postQueryPort, postCommandPort, postCacheCommandPort, eventPublisher);
    }

    @Test
    @DisplayName("게시글 삭제 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForDelete() {
        // Given
        Long userId = 1L;
        Long postId = 999L;

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(userId, postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryPort, times(1)).findById(postId);
        verify(postCommandPort, never()).delete(any());
        verify(postCacheCommandPort, never()).deleteFullPostCache(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("게시글 삭제 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForDelete() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(userId, postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(postCommandPort, never()).delete(any());
        verify(postCacheCommandPort, never()).deleteFullPostCache(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("게시글 작성 - null DTO")
    void shouldWritePost_WhenNullDTO() {
        // Given
        Long userId = 1L;
        Long expectedPostId = 123L;
        PostReqDTO postReqDTO = null;

        given(userLoadPort.getReferenceById(userId)).willReturn(user);
        given(postCommandPort.save(any(Post.class))).willReturn(post);
        given(post.getId()).willReturn(expectedPostId);

        // When
        Long result = postCommandService.writePost(userId, postReqDTO);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(userLoadPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 수정 - null DTO")
    void shouldUpdatePost_WhenNullDTO() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqDTO postReqDTO = null;

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);

        // When
        postCommandService.updatePost(userId, postId, postReqDTO);

        // Then
        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(post, times(1)).updatePost(postReqDTO);
        verify(postCommandPort, times(1)).save(post);
        verify(postCacheCommandPort, times(1)).deleteFullPostCache(postId);
    }

    @Test
    @DisplayName("게시글 삭제 - 이벤트 발행 검증")
    void shouldPublishCorrectEvent_WhenDeletePost() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);
        given(post.getTitle()).willReturn("테스트 제목");

        // When
        postCommandService.deletePost(userId, postId);

        // Then
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> 
            event instanceof PostDeletedEvent && 
            ((PostDeletedEvent) event).getPostId().equals(postId)
        ));
    }

    @Test
    @DisplayName("게시글 작성 - 빈 제목과 내용")
    void shouldWritePost_WhenEmptyTitleAndContent() {
        // Given
        Long userId = 1L;
        Long expectedPostId = 123L;
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("");
        postReqDTO.setContent("");

        given(userLoadPort.getReferenceById(userId)).willReturn(user);
        given(postCommandPort.save(any(Post.class))).willReturn(post);
        given(post.getId()).willReturn(expectedPostId);

        // When
        Long result = postCommandService.writePost(userId, postReqDTO);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(userLoadPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, times(1)).save(any(Post.class));
    }
}