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
import org.mockito.ArgumentCaptor;
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
        PostReqDTO postReqDTO = PostReqDTO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

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
    @DisplayName("게시글 작성 - null DTO 예외")
    void shouldThrowException_WhenNullDTO() {
        // Given
        Long userId = 1L;
        PostReqDTO postReqDTO = null;

        // When & Then
        assertThatThrownBy(() -> postCommandService.writePost(userId, postReqDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        // null 검증이 먼저 실행되므로 userLoadPort는 호출되지 않음
        verify(userLoadPort, never()).getReferenceById(any());
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 수정 - null DTO 예외")
    void shouldThrowException_WhenNullDTOForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqDTO postReqDTO = null;

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, postReqDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(post, never()).updatePost(any());
        verify(postCommandPort, never()).save(any());
        verify(postCacheCommandPort, never()).deleteFullPostCache(any());
    }

    @Test
    @DisplayName("게시글 삭제 - 이벤트 발행 검증")
    void shouldPublishCorrectEvent_WhenDeletePost() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        String postTitle = "테스트 제목";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);
        given(post.getTitle()).willReturn(postTitle);

        ArgumentCaptor<PostDeletedEvent> eventCaptor = ArgumentCaptor.forClass(PostDeletedEvent.class);

        // When
        postCommandService.deletePost(userId, postId);

        // Then
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        
        PostDeletedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.postId()).isEqualTo(postId);
        assertThat(capturedEvent.postTitle()).isEqualTo(postTitle);
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

    @Test
    @DisplayName("게시글 작성 - null 사용자 ID")
    void shouldThrowException_WhenNullUserId() {
        // Given
        Long userId = null;
        PostReqDTO postReqDTO = PostReqDTO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        given(userLoadPort.getReferenceById(userId)).willThrow(new IllegalArgumentException("User ID cannot be null"));

        // When & Then
        assertThatThrownBy(() -> postCommandService.writePost(userId, postReqDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID cannot be null");

        verify(userLoadPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 작성 - 사용자 없음 예외")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        Long userId = 999L;
        PostReqDTO postReqDTO = PostReqDTO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        given(userLoadPort.getReferenceById(userId)).willThrow(new RuntimeException("User not found"));

        // When & Then
        assertThatThrownBy(() -> postCommandService.writePost(userId, postReqDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userLoadPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 작성 - 긴 제목과 내용")
    void shouldWritePost_WhenLongTitleAndContent() {
        // Given
        Long userId = 1L;
        Long expectedPostId = 123L;
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("A".repeat(255)); // 긴 제목
        postReqDTO.setContent("B".repeat(5000)); // 긴 내용

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
    @DisplayName("게시글 수정 - null 사용자 ID")
    void shouldThrowException_WhenNullUserIdForUpdate() {
        // Given
        Long userId = null;
        Long postId = 123L;
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("수정된 제목");
        postReqDTO.setContent("수정된 내용");

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
    }

    @Test
    @DisplayName("게시글 수정 - null 게시글 ID")
    void shouldThrowException_WhenNullPostIdForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = null;
        PostReqDTO postReqDTO = new PostReqDTO();

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, postReqDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryPort, times(1)).findById(postId);
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 삭제 - null 사용자 ID")
    void shouldThrowException_WhenNullUserIdForDelete() {
        // Given
        Long userId = null;
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
    }

    @Test
    @DisplayName("게시글 삭제 - null 게시글 ID")
    void shouldThrowException_WhenNullPostIdForDelete() {
        // Given
        Long userId = 1L;
        Long postId = null;

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(userId, postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryPort, times(1)).findById(postId);
        verify(postCommandPort, never()).delete(any());
    }

    @Test
    @DisplayName("게시글 작성 - 음수 사용자 ID")
    void shouldThrowException_WhenNegativeUserId() {
        // Given
        Long userId = -1L;
        PostReqDTO postReqDTO = PostReqDTO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        given(userLoadPort.getReferenceById(userId)).willThrow(new RuntimeException("Invalid user ID"));

        // When & Then
        assertThatThrownBy(() -> postCommandService.writePost(userId, postReqDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid user ID");

        verify(userLoadPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 수정 - 음수 게시글 ID")
    void shouldThrowException_WhenNegativePostIdForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = -1L;
        PostReqDTO postReqDTO = new PostReqDTO();

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, postReqDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryPort, times(1)).findById(postId);
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 삭제 - 음수 게시글 ID")
    void shouldThrowException_WhenNegativePostIdForDelete() {
        // Given
        Long userId = 1L;
        Long postId = -1L;

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(userId, postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryPort, times(1)).findById(postId);
        verify(postCommandPort, never()).delete(any());
    }

    @Test
    @DisplayName("게시글 작성 - 매우 큰 사용자 ID")
    void shouldWritePost_WhenVeryLargeUserId() {
        // Given
        Long userId = Long.MAX_VALUE;
        Long expectedPostId = 123L;
        PostReqDTO postReqDTO = PostReqDTO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

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
    @DisplayName("게시글 수정 - 권한 검증 순서 확인")
    void shouldVerifyAuthorshipBeforeUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("수정된 제목");
        postReqDTO.setContent("수정된 내용");

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, postReqDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

        // isAuthor가 호출되었는지 확인 (권한 검증이 먼저 실행됨)
        verify(post, times(1)).isAuthor(userId);
        // 권한 검증에 실패했으므로 업데이트가 호출되지 않음
        verify(post, never()).updatePost(any());
        verify(postCommandPort, never()).save(any());
        verify(postCacheCommandPort, never()).deleteFullPostCache(any());
    }

    @Test
    @DisplayName("게시글 삭제 - 권한 검증 순서 확인")
    void shouldVerifyAuthorshipBeforeDelete() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(userId, postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

        // isAuthor가 호출되었는지 확인 (권한 검증이 먼저 실행됨)
        verify(post, times(1)).isAuthor(userId);
        // 권한 검증에 실패했으므로 삭제가 호출되지 않음
        verify(postCommandPort, never()).delete(any());
        verify(postCacheCommandPort, never()).deleteFullPostCache(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(post, never()).getTitle(); // 권한 실패시 제목을 가져오지 않음
    }

    @Test
    @DisplayName("게시글 삭제 - 이벤트 발행 실패시 롤백")
    void shouldHandleEventPublishFailure() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        String postTitle = "삭제될 게시글";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);
        given(post.getTitle()).willReturn(postTitle);
        doThrow(new RuntimeException("Event publish failed")).when(eventPublisher).publishEvent(any(PostDeletedEvent.class));

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(userId, postId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Event publish failed");

        // 이벤트 발행은 실패하지만 삭제와 캐시 정리는 완료됨
        verify(postCommandPort, times(1)).delete(post);
        verify(postCacheCommandPort, times(1)).deleteFullPostCache(postId);
        verify(eventPublisher, times(1)).publishEvent(any(PostDeletedEvent.class));
    }

    @Test
    @DisplayName("여러 게시글 연속 작성")
    void shouldWriteMultiplePosts() {
        // Given
        Long userId = 1L;
        Long[] expectedPostIds = {100L, 200L, 300L};
        
        given(userLoadPort.getReferenceById(userId)).willReturn(user);
        
        for (int i = 0; i < expectedPostIds.length; i++) {
            PostReqDTO postReqDTO = new PostReqDTO();
            postReqDTO.setTitle("제목 " + (i + 1));
            postReqDTO.setContent("내용 " + (i + 1));
            
            Post mockPost = mock(Post.class);
            given(postCommandPort.save(any(Post.class))).willReturn(mockPost);
            given(mockPost.getId()).willReturn(expectedPostIds[i]);
            
            // When
            Long result = postCommandService.writePost(userId, postReqDTO);
            
            // Then
            assertThat(result).isEqualTo(expectedPostIds[i]);
        }
        
        verify(userLoadPort, times(expectedPostIds.length)).getReferenceById(userId);
        verify(postCommandPort, times(expectedPostIds.length)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 수정 - 캐시 삭제 실패")
    void shouldUpdatePostEvenWhenCacheDeleteFails() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("수정된 제목");
        postReqDTO.setContent("수정된 내용");

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);
        doThrow(new RuntimeException("Cache delete failed")).when(postCacheCommandPort).deleteFullPostCache(postId);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, postReqDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cache delete failed");

        // 게시글 수정은 완료되지만 캐시 삭제에서 실패
        verify(post, times(1)).updatePost(postReqDTO);
        verify(postCommandPort, times(1)).save(post);
        verify(postCacheCommandPort, times(1)).deleteFullPostCache(postId);
    }
}