package jaeik.bimillog.domain.comment.service;

import jaeik.bimillog.domain.comment.application.port.out.*;
import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * <h2>CommentCommandService 단위 테스트</h2>
 * <p>댓글 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentCommandService 단위 테스트")
class CommentCommandServiceTest {

    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_COMMENT_ID = 200L;
    private static final String TEST_USER_NAME = "testUser";
    private static final String TEST_SOCIAL_ID = "kakao123";
    private static final String TEST_ORIGINAL_CONTENT = "원본 댓글";
    private static final String TEST_UPDATED_CONTENT = "수정된 댓글";
    private static final Integer TEST_PASSWORD = 1234;

    @Mock private CommentSavePort commentSavePort;
    @Mock private CommentDeletePort commentDeletePort;
    @Mock private CommentQueryPort commentQueryPort;
    @Mock private CommentLikePort commentLikePort;
    @Mock private CommentToUserPort commentToUserPort;

    @InjectMocks
    private CommentCommandService commentCommandService;

    private User testUser;
    private Comment testComment;
    private Comment.Request commentRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .userName(TEST_USER_NAME)
                .socialId(TEST_SOCIAL_ID)
                .build();

        testComment = Comment.builder()
                .id(TEST_COMMENT_ID)
                .content(TEST_ORIGINAL_CONTENT)
                .user(testUser)
                .password(null)
                .deleted(false)
                .build();

        commentRequest = Comment.Request.builder()
                .id(TEST_COMMENT_ID)
                .content(TEST_UPDATED_CONTENT)
                .build();
    }

    @Test
    @DisplayName("댓글 좋아요 추가 성공")
    void shouldAddLike_WhenUserHasNotLikedComment() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(false);

        // When
        commentCommandService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);

        // Then
        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikePort).save(likeCaptor.capture());

        CommentLike capturedLike = likeCaptor.getValue();
        assertThat(capturedLike.getComment()).isEqualTo(testComment);
        assertThat(capturedLike.getUser()).isEqualTo(testUser);

        verify(commentLikePort, never()).deleteLikeByIds(anyLong(), anyLong());
    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void shouldRemoveLike_WhenUserHasAlreadyLikedComment() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(true);

        // When
        commentCommandService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);

        // Then
        verify(commentLikePort).deleteLikeByIds(TEST_COMMENT_ID, TEST_USER_ID);
        verify(commentLikePort, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 댓글에 좋아요 시 COMMENT_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenCommentNotFound() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willThrow(new CommentCustomException(CommentErrorCode.COMMENT_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.likeComment(TEST_USER_ID, TEST_COMMENT_ID))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_NOT_FOUND);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(commentToUserPort, never()).findById(any());
        verify(commentLikePort, never()).isLikedByUser(any(), any());
        verify(commentLikePort, never()).save(any());
        verify(commentLikePort, never()).deleteLikeByIds(anyLong(), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 좋아요 시 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentCommandService.likeComment(TEST_USER_ID, TEST_COMMENT_ID))
                .isInstanceOf(UserCustomException.class)
                .hasFieldOrPropertyWithValue("userErrorCode", UserErrorCode.USER_NOT_FOUND);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(commentToUserPort).findById(TEST_USER_ID);
        verify(commentLikePort, never()).isLikedByUser(any(), any());
        verify(commentLikePort, never()).save(any());
        verify(commentLikePort, never()).deleteLikeByIds(anyLong(), anyLong());
    }

    @Test
    @DisplayName("익명 사용자(null userId)가 좋아요 시 예외 발생")
    void shouldThrowException_WhenUserIdIsNull() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);
        given(commentToUserPort.findById(null)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentCommandService.likeComment(null, TEST_COMMENT_ID))
                .isInstanceOf(UserCustomException.class)
                .hasFieldOrPropertyWithValue("userErrorCode", UserErrorCode.USER_NOT_FOUND);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(commentToUserPort).findById(null);
        verify(commentLikePort, never()).isLikedByUser(any(), any());
        verify(commentLikePort, never()).save(any());
        verify(commentLikePort, never()).deleteLikeByIds(anyLong(), anyLong());
    }

    @Test
    @DisplayName("자신의 댓글에 좋아요")
    void shouldAllowSelfLike() {
        // Given
        Comment ownComment = Comment.builder()
                .id(200L)
                .content("내가 작성한 댓글")
                .user(testUser)
                .deleted(false)
                .build();

        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(ownComment);
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(false);

        // When
        commentCommandService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);

        // Then
        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikePort).save(likeCaptor.capture());

        CommentLike capturedLike = likeCaptor.getValue();
        assertThat(capturedLike.getComment()).isEqualTo(ownComment);
        assertThat(capturedLike.getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("여러 번 연속으로 좋아요 토글")
    void shouldToggleLikeMultipleTimes() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));

        // 첫 번째: 좋아요 추가
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(false);
        commentCommandService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);
        verify(commentLikePort).save(any());

        // 두 번째: 좋아요 취소
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(true);
        commentCommandService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);
        verify(commentLikePort).deleteLikeByIds(TEST_COMMENT_ID, TEST_USER_ID);

        // 세 번째: 다시 좋아요 추가
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(false);
        commentCommandService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);

        // Then
        verify(commentLikePort, times(2)).save(any()); // 총 2번 호출
        verify(commentLikePort, times(1)).deleteLikeByIds(TEST_COMMENT_ID, TEST_USER_ID); // 1번 호출
    }

    @Test
    @DisplayName("인증된 사용자의 댓글 수정 성공")
    void shouldUpdateComment_WhenAuthenticatedUserOwnsComment() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);

        // When
        commentCommandService.updateComment(TEST_USER_ID, commentRequest);

        // Then
        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(commentSavePort).save(testComment);
    }

    @Test
    @DisplayName("익명 사용자의 패스워드 일치로 댓글 수정 성공")
    void shouldUpdateComment_WhenAnonymousUserWithCorrectPassword() {
        // Given
        Comment anonymousComment = Comment.builder()
                .id(TEST_COMMENT_ID)
                .content("익명 댓글")
                .user(null)
                .password(TEST_PASSWORD)
                .deleted(false)
                .build();

        Comment.Request anonymousCommentRequest = Comment.Request.builder()
                .id(TEST_COMMENT_ID)
                .content("수정된 익명 댓글")
                .password(TEST_PASSWORD)
                .build();

        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(anonymousComment);

        // When
        commentCommandService.updateComment(null, anonymousCommentRequest);

        // Then
        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(commentSavePort).save(anonymousComment);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 시 COMMENT_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenCommentNotFoundForUpdate() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willThrow(new CommentCustomException(CommentErrorCode.COMMENT_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(TEST_USER_ID, commentRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_NOT_FOUND);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(commentSavePort, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("잘못된 패스워드로 댓글 수정 시 COMMENT_PASSWORD_NOT_MATCH 예외 발생")
    void shouldThrowException_WhenPasswordNotMatch() {
        // Given
        Comment passwordComment = Comment.builder()
                .id(TEST_COMMENT_ID)
                .content("패스워드 댓글")
                .user(null)
                .password(TEST_PASSWORD)
                .deleted(false)
                .build();

        Comment.Request wrongPasswordRequest = Comment.Request.builder()
                .id(TEST_COMMENT_ID)
                .content(TEST_UPDATED_CONTENT)
                .password(9999)
                .build();

        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(passwordComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(null, wrongPasswordRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_PASSWORD_NOT_MATCH);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(commentSavePort, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("다른 사용자가 댓글 수정 시 ONLY_COMMENT_OWNER_UPDATE 예외 발생")
    void shouldThrowException_WhenNotCommentOwner() {
        // Given
        Long userId = 100L;
        User anotherUser = User.builder()
                .id(999L)
                .userName("anotherUser")
                .build();

        Comment anotherUserComment = Comment.builder()
                .id(200L)
                .content("다른 사용자 댓글")
                .user(anotherUser)
                .password(null)
                .deleted(false)
                .build();

        given(commentQueryPort.findById(200L)).willReturn(anotherUserComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(userId, commentRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(200L);
        verify(commentSavePort, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("후손이 없는 댓글 삭제 시 완전 삭제")
    void shouldDeleteComment_WhenNoDescendants() {
        // Given
        Long userId = 100L;
        given(commentQueryPort.findById(200L)).willReturn(testComment);
        given(commentDeletePort.conditionalSoftDelete(200L)).willReturn(0); // 자손이 없어서 소프트 삭제 안됨
        given(commentDeletePort.deleteClosuresByDescendantId(200L)).willReturn(1);
        given(commentDeletePort.hardDeleteComment(200L)).willReturn(1);

        // When
        commentCommandService.deleteComment(userId, commentRequest);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentDeletePort).conditionalSoftDelete(200L);
        verify(commentDeletePort).deleteClosuresByDescendantId(200L);
        verify(commentDeletePort).hardDeleteComment(200L);
        verify(commentSavePort, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("후손이 있는 댓글 삭제 시 소프트 삭제")
    void shouldSoftDeleteComment_WhenHasDescendants() {
        // Given
        Long userId = 100L;
        given(commentQueryPort.findById(200L)).willReturn(testComment);
        given(commentDeletePort.conditionalSoftDelete(200L)).willReturn(1); // 자손이 있어서 소프트 삭제됨

        // When
        commentCommandService.deleteComment(userId, commentRequest);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentDeletePort).conditionalSoftDelete(200L);
        // 소프트 삭제되면 하드 삭제 관련 메서드들은 호출되지 않음
        verify(commentDeletePort, never()).deleteClosuresByDescendantId(any());
        verify(commentDeletePort, never()).hardDeleteComment(any());
        verify(commentSavePort, never()).save(any(Comment.class));
    }


    @Test
    @DisplayName("인증 정보가 null인 상태에서 패스워드 없는 댓글 수정 시 예외 발생")
    void shouldThrowException_WhenNullUserDetailsWithoutPassword() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(testComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(null, commentRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(200L);
        verify(commentSavePort, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("이미 삭제된 댓글에 대한 작업")
    void shouldHandleDeletedComment() {
        // Given
        Long userId = 100L;
        Comment deletedComment = Comment.builder()
                .id(200L)
                .content("삭제된 댓글")
                .user(testUser)
                .password(null)
                .deleted(true)
                .build();

        given(commentQueryPort.findById(200L)).willReturn(deletedComment);

        // When
        commentCommandService.updateComment(userId, commentRequest);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentSavePort).save(deletedComment);
    }

    @Test
    @DisplayName("빈 문자열 패스워드로 댓글 수정 시 ONLY_COMMENT_OWNER_UPDATE 예외 발생")
    void shouldThrowException_WhenEmptyPasswordWithoutUser() {
        // Given
        Comment emptyPasswordComment = Comment.builder()
                .id(200L)
                .content("빈 패스워드 댓글")
                .user(null)
                .password(null)
                .deleted(false)
                .build();

        Comment.Request emptyPasswordRequest = Comment.Request.builder()
                .id(200L)
                .content("수정된 댓글")
                .password(null)
                .build();

        given(commentQueryPort.findById(200L)).willReturn(emptyPasswordComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(null, emptyPasswordRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(200L);
        verify(commentSavePort, never()).save(any(Comment.class));
    }

    // === 누락된 댓글 삭제 테스트 케이스들 ===

    @Test
    @DisplayName("익명 댓글 삭제 - 패스워드 일치로 삭제 성공")
    void shouldDeleteAnonymousComment_WhenCorrectPasswordProvided() {
        // Given
        Comment anonymousComment = Comment.builder()
                .id(300L)
                .content("익명 댓글")
                .user(null)
                .password(1234)
                .deleted(false)
                .build();

        Comment.Request deleteRequest = Comment.Request.builder()
                .id(300L)
                .password(1234)
                .build();

        given(commentQueryPort.findById(300L)).willReturn(anonymousComment);
        given(commentDeletePort.conditionalSoftDelete(300L)).willReturn(0); // 자손이 없어서 소프트 삭제 안됨
        given(commentDeletePort.deleteClosuresByDescendantId(300L)).willReturn(1);
        given(commentDeletePort.hardDeleteComment(300L)).willReturn(1);

        // When
        commentCommandService.deleteComment(null, deleteRequest);

        // Then
        verify(commentQueryPort).findById(300L);
        verify(commentDeletePort).conditionalSoftDelete(300L);
        verify(commentDeletePort).deleteClosuresByDescendantId(300L);
        verify(commentDeletePort).hardDeleteComment(300L);
    }

    @Test
    @DisplayName("익명 댓글 삭제 - 잘못된 패스워드로 삭제 실패")
    void shouldThrowException_WhenAnonymousCommentWithWrongPassword() {
        // Given
        Comment anonymousComment = Comment.builder()
                .id(300L)
                .content("익명 댓글")
                .user(null)
                .password(1234)
                .deleted(false)
                .build();

        Comment.Request deleteRequest = Comment.Request.builder()
                .id(300L)
                .password(9999)
                .build();

        given(commentQueryPort.findById(300L)).willReturn(anonymousComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.deleteComment(null, deleteRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_PASSWORD_NOT_MATCH);

        verify(commentQueryPort).findById(300L);
        verify(commentDeletePort, never()).hardDeleteComment(any());
    }

    @Test
    @DisplayName("계층 댓글 삭제 - 자손이 있는 인증 사용자 댓글 소프트 삭제")
    void shouldSoftDeleteUserComment_WhenHasDescendants() {
        // Given
        Long userId = 100L;
        Comment parentComment = Comment.builder()
                .id(400L)
                .content("부모 댓글")
                .user(testUser)
                .password(null)
                .deleted(false)
                .build();

        Comment.Request deleteRequest = Comment.Request.builder()
                .id(400L)
                .build();

        given(commentQueryPort.findById(400L)).willReturn(parentComment);
        given(commentDeletePort.conditionalSoftDelete(400L)).willReturn(1); // 자손이 있어서 소프트 삭제됨

        // When
        commentCommandService.deleteComment(userId, deleteRequest);

        // Then
        verify(commentQueryPort).findById(400L);
        verify(commentDeletePort).conditionalSoftDelete(400L);
        // 소프트 삭제되면 하드 삭제 관련 메서드들은 호출되지 않음
        verify(commentDeletePort, never()).deleteClosuresByDescendantId(any());
        verify(commentDeletePort, never()).hardDeleteComment(any());
        verify(commentSavePort, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("계층 댓글 삭제 - 자손이 있는 익명 댓글 소프트 삭제")
    void shouldSoftDeleteAnonymousComment_WhenHasDescendants() {
        // Given
        Comment anonymousParentComment = Comment.builder()
                .id(500L)
                .content("익명 부모 댓글")
                .user(null)
                .password(5678)
                .deleted(false)
                .build();

        Comment.Request deleteRequest = Comment.Request.builder()
                .id(500L)
                .password(5678)
                .build();

        given(commentQueryPort.findById(500L)).willReturn(anonymousParentComment);
        given(commentDeletePort.conditionalSoftDelete(500L)).willReturn(1); // 자손이 있어서 소프트 삭제됨

        // When
        commentCommandService.deleteComment(null, deleteRequest);

        // Then
        verify(commentQueryPort).findById(500L);
        verify(commentDeletePort).conditionalSoftDelete(500L);
        // 소프트 삭제되면 하드 삭제 관련 메서드들은 호출되지 않음
        verify(commentDeletePort, never()).deleteClosuresByDescendantId(any());
        verify(commentDeletePort, never()).hardDeleteComment(any());
        verify(commentSavePort, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("다른 사용자가 댓글 삭제 시도 - ONLY_COMMENT_OWNER_UPDATE 예외 발생")
    void shouldThrowException_WhenNotOwnerTriesToDelete() {
        // Given
        Long requestUserId = 100L;
        User anotherUser = User.builder()
                .id(999L)
                .userName("anotherUser")
                .build();

        Comment anotherUserComment = Comment.builder()
                .id(600L)
                .content("다른 사용자 댓글")
                .user(anotherUser)
                .password(null)
                .deleted(false)
                .build();

        Comment.Request deleteRequest = Comment.Request.builder()
                .id(600L)
                .build();

        given(commentQueryPort.findById(600L)).willReturn(anotherUserComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.deleteComment(requestUserId, deleteRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(600L);
        verify(commentDeletePort, never()).hardDeleteComment(any());
    }

}