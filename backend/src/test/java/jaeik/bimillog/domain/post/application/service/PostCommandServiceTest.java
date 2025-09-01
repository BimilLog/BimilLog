package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.LoadUserInfoPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostReqVO;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private LoadUserInfoPort loadUserInfoPort;

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
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postCommandPort.save(any(Post.class))).willReturn(post);
        given(post.getId()).willReturn(expectedPostId);

        // When
        Long result = postCommandService.writePost(userId, postReqDTO);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(loadUserInfoPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, times(1)).save(any(Post.class));
        verifyNoMoreInteractions(loadUserInfoPort, postCommandPort);
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void shouldUpdatePost_WhenValidAuthor() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .build();

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);

        // When
        postCommandService.updatePost(userId, postId, postReqDTO);

        // Then
        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(post, times(1)).updatePost(postReqDTO);
        verify(postCommandPort, times(1)).save(post);
        verify(postCacheCommandPort, times(1)).deleteCache(null, postId, new PostCacheFlag[0]);
        verifyNoMoreInteractions(postQueryPort, postCommandPort, postCacheCommandPort);
    }

    @Test
    @DisplayName("게시글 수정 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 999L;
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, postReqDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryPort, times(1)).findById(postId);
        verify(postCommandPort, never()).save(any());
        verify(postCacheCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 수정 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

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
        verify(postCacheCommandPort, never()).deleteCache(any(), any());
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
        verify(postCacheCommandPort, times(1)).deleteCache(null, postId, new PostCacheFlag[0]);
        verifyNoMoreInteractions(postQueryPort, postCommandPort, postCacheCommandPort);
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
        verify(postCacheCommandPort, never()).deleteCache(any(), any());
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
        verify(postCacheCommandPort, never()).deleteCache(any(), any());
    }

    @Test
    @DisplayName("게시글 작성 - null DTO 예외 (Post.createPost에서 발생)")
    void shouldThrowException_WhenNullDTO() {
        // Given
        Long userId = 1L;
        PostReqVO postReqDTO = null;
        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);

        // When & Then
        assertThatThrownBy(() -> postCommandService.writePost(userId, postReqDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PostReqVO cannot be null");

        // loadUserInfoPort는 호출되지만 Post.createPost에서 예외 발생
        verify(loadUserInfoPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 수정 - null DTO 예외 (Post.updatePost에서 발생)")
    void shouldThrowException_WhenNullDTOForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqVO postReqDTO = null;

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);
        
        // Mock이 실제 예외를 발생시키도록 설정
        doThrow(new IllegalArgumentException("PostReqVO cannot be null"))
            .when(post).updatePost(postReqDTO);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, postReqDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PostReqVO cannot be null");

        verify(postQueryPort, times(1)).findById(postId);
        verify(post, times(1)).isAuthor(userId);
        verify(post, times(1)).updatePost(postReqDTO); // updatePost에서 예외 발생
        verify(postCommandPort, never()).save(any()); // 예외로 인해 호출되지 않음
        verify(postCacheCommandPort, never()).deleteCache(any(), any()); // 예외로 인해 호출되지 않음
    }


    @Test
    @DisplayName("게시글 작성 - null 사용자 ID")
    void shouldThrowException_WhenNullUserId() {
        // Given
        Long userId = null;
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        // userId가 null이면 loadUserInfoPort 호출되지 않고 user가 null로 설정됨
        // postCommandPort.save() 기본값이 null이므로 NPE 발생
        given(postCommandPort.save(any())).willReturn(null);

        // When & Then
        assertThatThrownBy(() -> postCommandService.writePost(userId, postReqDTO))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Cannot invoke")
                .hasMessageContaining("getId()");

        verify(loadUserInfoPort, never()).getReferenceById(any()); // 호출되지 않음
        verify(postCommandPort, times(1)).save(any());
    }

    @Test
    @DisplayName("게시글 작성 - 사용자 없음 예외")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        Long userId = 999L;
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        given(loadUserInfoPort.getReferenceById(userId)).willThrow(new RuntimeException("User not found"));

        // When & Then
        assertThatThrownBy(() -> postCommandService.writePost(userId, postReqDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(loadUserInfoPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 작성 - 긴 제목과 내용")
    void shouldWritePost_WhenLongTitleAndContent() {
        // Given
        Long userId = 1L;
        Long expectedPostId = 123L;
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("A".repeat(255)) // 긴 제목
                .content("B".repeat(5000)) // 긴 내용
                .build();

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postCommandPort.save(any(Post.class))).willReturn(post);
        given(post.getId()).willReturn(expectedPostId);

        // When
        Long result = postCommandService.writePost(userId, postReqDTO);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(loadUserInfoPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 수정 - null 사용자 ID")
    void shouldThrowException_WhenNullUserIdForUpdate() {
        // Given
        Long userId = null;
        Long postId = 123L;
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .build();

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
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

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
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        given(loadUserInfoPort.getReferenceById(userId)).willThrow(new RuntimeException("Invalid user ID"));

        // When & Then
        assertThatThrownBy(() -> postCommandService.writePost(userId, postReqDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid user ID");

        verify(loadUserInfoPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 수정 - 음수 게시글 ID")
    void shouldThrowException_WhenNegativePostIdForUpdate() {
        // Given
        Long userId = 1L;
        Long postId = -1L;
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

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
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postCommandPort.save(any(Post.class))).willReturn(post);
        given(post.getId()).willReturn(expectedPostId);

        // When
        Long result = postCommandService.writePost(userId, postReqDTO);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(loadUserInfoPort, times(1)).getReferenceById(userId);
        verify(postCommandPort, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 수정 - 권한 검증 순서 확인")
    void shouldVerifyAuthorshipBeforeUpdate() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .build();

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
        verify(postCacheCommandPort, never()).deleteCache(any(), any());
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
        verify(postCacheCommandPort, never()).deleteCache(any(), any());
        verify(post, never()).getTitle(); // 권한 실패시 제목을 가져오지 않음
    }


    @Test
    @DisplayName("여러 게시글 연속 작성")
    void shouldWriteMultiplePosts() {
        // Given
        Long userId = 1L;
        Long[] expectedPostIds = {100L, 200L, 300L};
        
        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        
        for (int i = 0; i < expectedPostIds.length; i++) {
            PostReqVO postReqDTO = PostReqVO.builder()
                    .title("제목 " + (i + 1))
                    .content("내용 " + (i + 1))
                    .build();
            
            Post mockPost = mock(Post.class);
            given(postCommandPort.save(any(Post.class))).willReturn(mockPost);
            given(mockPost.getId()).willReturn(expectedPostIds[i]);
            
            // When
            Long result = postCommandService.writePost(userId, postReqDTO);
            
            // Then
            assertThat(result).isEqualTo(expectedPostIds[i]);
        }
        
        verify(loadUserInfoPort, times(expectedPostIds.length)).getReferenceById(userId);
        verify(postCommandPort, times(expectedPostIds.length)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 수정 - 캐시 삭제 실패")
    void shouldUpdatePostEvenWhenCacheDeleteFails() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        PostReqVO postReqDTO = PostReqVO.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .build();

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isAuthor(userId)).willReturn(true);
        doThrow(new RuntimeException("Cache delete failed")).when(postCacheCommandPort).deleteCache(null, postId, new PostCacheFlag[0]);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(userId, postId, postReqDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cache delete failed");

        // 게시글 수정은 완료되지만 캐시 삭제에서 실패
        verify(post, times(1)).updatePost(postReqDTO);
        verify(postCommandPort, times(1)).save(post);
        verify(postCacheCommandPort, times(1)).deleteCache(null, postId, new PostCacheFlag[0]);
    }
}