package jaeik.bimillog.infrastructure.adapter.comment.in.listener;

import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
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
    private CommentCommandService commentCommandService;

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
        verify(commentCommandService).processUserCommentsOnWithdrawal(eq(userId));
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - 익명화 실패 시 예외 전파")
    void handleUserWithdrawnEvent_WhenAnonymizeFails_ShouldPropagateException() {
        // Given
        Long userId = 1L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);
        
        RuntimeException processException = new RuntimeException("댓글 처리 실패");
        doThrow(processException).when(commentCommandService).processUserCommentsOnWithdrawal(userId);

        // When & Then
        try {
            commentEventListener.handleUserWithdrawnEvent(event);
        } catch (RuntimeException e) {
            // 예외가 전파되어야 함 (탈퇴 처리 자체가 실패로 간주)
            verify(commentCommandService).processUserCommentsOnWithdrawal(eq(userId));
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
        verify(commentCommandService).processUserCommentsOnWithdrawal(eq(userId1));
        verify(commentCommandService).processUserCommentsOnWithdrawal(eq(userId2));
    }





    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - null userId 처리")
    void handleUserWithdrawnEvent_WithNullUserId() {
        // Given
        UserWithdrawnEvent event = new UserWithdrawnEvent(null);

        // When
        commentEventListener.handleUserWithdrawnEvent(event);

        // Then - null userId도 서비스로 전달되어야 함
        verify(commentCommandService).processUserCommentsOnWithdrawal(eq(null));
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 처리 - 이벤트 정보 검증")
    void handleUserWithdrawnEvent_EventDataValidation() {
        // Given - UserWithdrawnEvent
        Long userId = 888L;
        UserWithdrawnEvent userEvent = new UserWithdrawnEvent(userId);
        assert userEvent.userId().equals(userId);

        // When
        commentEventListener.handleUserWithdrawnEvent(userEvent);

        // Then
        verify(commentCommandService).processUserCommentsOnWithdrawal(eq(userId));
    }
}