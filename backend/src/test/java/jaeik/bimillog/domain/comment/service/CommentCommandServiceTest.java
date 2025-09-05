package jaeik.bimillog.domain.comment.service;

import jaeik.bimillog.domain.comment.application.port.out.*;
import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Mock
    private CommentCommandPort commentCommandPort;

    @Mock
    private CommentQueryPort commentQueryPort;

    @Mock
    private CommentClosureQueryPort commentClosureQueryPort;

    @Mock
    private CommentClosureCommandPort commentClosureCommandPort;

    @Mock
    private CommentToPostPort commentToPostPort;

    @Mock
    private CommentToUserPort commentToUserPort;

    @InjectMocks
    private CommentCommandService commentCommandService;

    private User testUser;
    private Comment testComment;
    private Comment.Request commentRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(100L)
                .userName("testUser")
                .socialId("kakao123")
                .build();

        testComment = Comment.builder()
                .id(200L)
                .content("원본 댓글")
                .user(testUser)
                .password(null)
                .deleted(false)
                .build();

        commentRequest = Comment.Request.builder()
                .id(200L)
                .content("수정된 댓글")
                .build();
    }

    @Test
    @DisplayName("인증된 사용자의 댓글 수정 성공")
    void shouldUpdateComment_WhenAuthenticatedUserOwnsComment() {
        // Given
        Long userId = 100L;
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));

        // When
        commentCommandService.updateComment(userId, commentRequest);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort).save(testComment);
        // testComment.getContent()가 "수정된 댓글"로 변경되었는지는 실제 구현에 따라 다름
    }

    @Test
    @DisplayName("익명 사용자의 패스워드 일치로 댓글 수정 성공")
    void shouldUpdateComment_WhenAnonymousUserWithCorrectPassword() {
        // Given
        Comment anonymousComment = Comment.builder()
                .id(200L)
                .content("익명 댓글")
                .user(null)
                .password(1234)
                .deleted(false)
                .build();

        Comment.Request anonymousCommentRequest = Comment.Request.builder()
                .id(200L)
                .content("수정된 익명 댓글")
                .password(1234)
                .build();

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(anonymousComment));

        // When
        commentCommandService.updateComment(null, anonymousCommentRequest);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort).save(anonymousComment);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 시 COMMENT_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenCommentNotFound() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(100L, commentRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_NOT_FOUND);

        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("잘못된 패스워드로 댓글 수정 시 COMMENT_PASSWORD_NOT_MATCH 예외 발생")
    void shouldThrowException_WhenPasswordNotMatch() {
        // Given
        Comment passwordComment = Comment.builder()
                .id(200L)
                .content("패스워드 댓글")
                .user(null)
                .password(1234)
                .deleted(false)
                .build();

        Comment.Request wrongPasswordRequest = Comment.Request.builder()
                .id(200L)
                .content("수정된 댓글")
                .password(9999)
                .build();

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(passwordComment));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(null, wrongPasswordRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_PASSWORD_NOT_MATCH);

        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort, never()).save(any());
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

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(anotherUserComment));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(userId, commentRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("후손이 없는 댓글 삭제 시 완전 삭제")
    void shouldDeleteComment_WhenNoDescendants() {
        // Given
        Long userId = 100L;
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));
        given(commentCommandPort.conditionalSoftDelete(200L)).willReturn(0); // 자손이 없어서 소프트 삭제 안됨
        given(commentCommandPort.deleteClosuresByDescendantId(200L)).willReturn(1);
        given(commentCommandPort.hardDeleteComment(200L)).willReturn(1);

        // When
        commentCommandService.deleteComment(userId, commentRequest);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort).conditionalSoftDelete(200L);
        verify(commentCommandPort).deleteClosuresByDescendantId(200L);
        verify(commentCommandPort).hardDeleteComment(200L);
        verify(commentCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("후손이 있는 댓글 삭제 시 소프트 삭제")
    void shouldSoftDeleteComment_WhenHasDescendants() {
        // Given
        Long userId = 100L;
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));
        given(commentCommandPort.conditionalSoftDelete(200L)).willReturn(1); // 자손이 있어서 소프트 삭제됨

        // When
        commentCommandService.deleteComment(userId, commentRequest);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort).conditionalSoftDelete(200L);
        // 소프트 삭제되면 하드 삭제 관련 메서드들은 호출되지 않음
        verify(commentCommandPort, never()).deleteClosuresByDescendantId(any());
        verify(commentCommandPort, never()).hardDeleteComment(any());
        verify(commentCommandPort, never()).save(any());
    }


    @Test
    @DisplayName("인증 정보가 null인 상태에서 패스워드 없는 댓글 수정 시 예외 발생")
    void shouldThrowException_WhenNullUserDetailsWithoutPassword() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(null, commentRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort, never()).save(any());
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

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(deletedComment));

        // When
        commentCommandService.updateComment(userId, commentRequest);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort).save(deletedComment);
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

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(emptyPasswordComment));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(null, emptyPasswordRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort, never()).save(any());
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

        given(commentQueryPort.findById(300L)).willReturn(Optional.of(anonymousComment));
        given(commentCommandPort.conditionalSoftDelete(300L)).willReturn(0); // 자손이 없어서 소프트 삭제 안됨
        given(commentCommandPort.deleteClosuresByDescendantId(300L)).willReturn(1);
        given(commentCommandPort.hardDeleteComment(300L)).willReturn(1);

        // When
        commentCommandService.deleteComment(null, deleteRequest);

        // Then
        verify(commentQueryPort).findById(300L);
        verify(commentCommandPort).conditionalSoftDelete(300L);
        verify(commentCommandPort).deleteClosuresByDescendantId(300L);
        verify(commentCommandPort).hardDeleteComment(300L);
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

        given(commentQueryPort.findById(300L)).willReturn(Optional.of(anonymousComment));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.deleteComment(null, deleteRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_PASSWORD_NOT_MATCH);

        verify(commentQueryPort).findById(300L);
        verify(commentClosureQueryPort, never()).hasDescendants(any());
        verify(commentCommandPort, never()).hardDeleteComment(any());
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

        given(commentQueryPort.findById(400L)).willReturn(Optional.of(parentComment));
        given(commentCommandPort.conditionalSoftDelete(400L)).willReturn(1); // 자손이 있어서 소프트 삭제됨

        // When
        commentCommandService.deleteComment(userId, deleteRequest);

        // Then
        verify(commentQueryPort).findById(400L);
        verify(commentCommandPort).conditionalSoftDelete(400L);
        // 소프트 삭제되면 하드 삭제 관련 메서드들은 호출되지 않음
        verify(commentCommandPort, never()).deleteClosuresByDescendantId(any());
        verify(commentCommandPort, never()).hardDeleteComment(any());
        verify(commentCommandPort, never()).save(any());
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

        given(commentQueryPort.findById(500L)).willReturn(Optional.of(anonymousParentComment));
        given(commentCommandPort.conditionalSoftDelete(500L)).willReturn(1); // 자손이 있어서 소프트 삭제됨

        // When
        commentCommandService.deleteComment(null, deleteRequest);

        // Then
        verify(commentQueryPort).findById(500L);
        verify(commentCommandPort).conditionalSoftDelete(500L);
        // 소프트 삭제되면 하드 삭제 관련 메서드들은 호출되지 않음
        verify(commentCommandPort, never()).deleteClosuresByDescendantId(any());
        verify(commentCommandPort, never()).hardDeleteComment(any());
        verify(commentCommandPort, never()).save(any());
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

        given(commentQueryPort.findById(600L)).willReturn(Optional.of(anotherUserComment));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.deleteComment(requestUserId, deleteRequest))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(600L);
        verify(commentClosureQueryPort, never()).hasDescendants(any());
        verify(commentCommandPort, never()).hardDeleteComment(any());
    }
}