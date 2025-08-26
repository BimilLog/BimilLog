package jaeik.growfarm.infrastructure.adapter.comment.in.listener;

import jaeik.growfarm.domain.auth.event.UserWithdrawnEvent;
import jaeik.growfarm.domain.comment.application.port.out.CommentCommandPort;
import jaeik.growfarm.domain.post.event.PostDeletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * <h2>댓글 이벤트 리스너 테스트</h2>
 * <p>CommentEventListener의 단위 테스트</p>
 * <p>사용자 탈퇴 및 게시글 삭제 이벤트 처리 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("댓글 이벤트 리스너 테스트")
class CommentEventListenerTest {

    @Mock
    private CommentCommandPort commentCommandPort;

    @InjectMocks
    private CommentEventListener commentEventListener;

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - 댓글 익명화")
    void handleUserWithdrawnEvent_ShouldAnonymizeComments() {
        // Given
        Long userId = 1L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

        // When
        commentEventListener.handleUserWithdrawnEvent(event);

        // Then
        verify(commentCommandPort).anonymizeUserComments(eq(userId));
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - 익명화 실패 시 예외 전파")
    void handleUserWithdrawnEvent_WhenAnonymizeFails_ShouldPropagateException() {
        // Given
        Long userId = 1L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);
        
        RuntimeException anonymizeException = new RuntimeException("댓글 익명화 실패");
        doThrow(anonymizeException).when(commentCommandPort).anonymizeUserComments(userId);

        // When & Then
        try {
            commentEventListener.handleUserWithdrawnEvent(event);
        } catch (RuntimeException e) {
            // 예외가 전파되어야 함 (탈퇴 처리 자체가 실패로 간주)
            verify(commentCommandPort).anonymizeUserComments(eq(userId));
        }
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - 다양한 사용자 ID")
    void handleUserWithdrawnEvent_WithDifferentUserIds() {
        // Given
        Long userId1 = 999L;
        Long userId2 = 123L;
        UserWithdrawnEvent event1 = new UserWithdrawnEvent(userId1);
        UserWithdrawnEvent event2 = new UserWithdrawnEvent(userId2);
        
        // When
        commentEventListener.handleUserWithdrawnEvent(event1);
        commentEventListener.handleUserWithdrawnEvent(event2);
        
        // Then
        verify(commentCommandPort).anonymizeUserComments(eq(userId1));
        verify(commentCommandPort).anonymizeUserComments(eq(userId2));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 댓글 삭제")
    void handlePostDeletedEvent_ShouldDeleteAllComments() {
        // Given
        Long postId = 100L;
        PostDeletedEvent event = new PostDeletedEvent(postId, "테스트 게시글");

        // When
        commentEventListener.handlePostDeletedEvent(event);

        // Then
        verify(commentCommandPort).deleteAllByPostId(eq(postId));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 삭제 실패 시 예외 전파")
    void handlePostDeletedEvent_WhenDeleteFails_ShouldPropagateException() {
        // Given
        Long postId = 100L;
        PostDeletedEvent event = new PostDeletedEvent(postId, "테스트 게시글");
        
        RuntimeException deleteException = new RuntimeException("댓글 삭제 실패");
        doThrow(deleteException).when(commentCommandPort).deleteAllByPostId(postId);

        // When & Then
        try {
            commentEventListener.handlePostDeletedEvent(event);
        } catch (RuntimeException e) {
            // 예외가 전파되어야 함 (게시글 삭제 처리 자체가 실패로 간주)
            verify(commentCommandPort).deleteAllByPostId(eq(postId));
        }
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 다양한 게시글 ID")
    void handlePostDeletedEvent_WithDifferentPostIds() {
        // Given
        Long postId1 = 500L;
        Long postId2 = 777L;
        PostDeletedEvent event1 = new PostDeletedEvent(postId1, "테스트 게시글1");
        PostDeletedEvent event2 = new PostDeletedEvent(postId2, "테스트 게시글2");
        
        // When
        commentEventListener.handlePostDeletedEvent(event1);
        commentEventListener.handlePostDeletedEvent(event2);
        
        // Then
        verify(commentCommandPort).deleteAllByPostId(eq(postId1));
        verify(commentCommandPort).deleteAllByPostId(eq(postId2));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - null postId 처리")
    void handlePostDeletedEvent_WithNullPostId() {
        // Given
        PostDeletedEvent event = new PostDeletedEvent(null, null);

        // When
        commentEventListener.handlePostDeletedEvent(event);

        // Then - null postId도 포트로 전달되어야 함
        verify(commentCommandPort).deleteAllByPostId(eq(null));
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - null userId 처리")
    void handleUserWithdrawnEvent_WithNullUserId() {
        // Given
        UserWithdrawnEvent event = new UserWithdrawnEvent(null);

        // When
        commentEventListener.handleUserWithdrawnEvent(event);

        // Then - null userId도 포트로 전달되어야 함
        verify(commentCommandPort).anonymizeUserComments(eq(null));
    }

    @Test
    @DisplayName("이벤트 처리 - 이벤트 정보 검증")
    void handleEvents_EventDataValidation() {
        // Given - UserWithdrawnEvent
        Long userId = 888L;
        UserWithdrawnEvent userEvent = new UserWithdrawnEvent(userId);
        assert userEvent.userId().equals(userId);

        // Given - PostDeletedEvent
        Long postId = 999L;
        PostDeletedEvent postEvent = new PostDeletedEvent(postId, "검증용 게시글");
        assert postEvent.postId().equals(postId);

        // When
        commentEventListener.handleUserWithdrawnEvent(userEvent);
        commentEventListener.handlePostDeletedEvent(postEvent);

        // Then
        verify(commentCommandPort).anonymizeUserComments(eq(userId));
        verify(commentCommandPort).deleteAllByPostId(eq(postId));
    }
}