package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.out.CommentClosureCommandPort;
import jaeik.growfarm.domain.comment.application.port.out.CommentClosureQueryPort;
import jaeik.growfarm.domain.comment.application.port.out.CommentCommandPort;
import jaeik.growfarm.domain.comment.application.port.out.CommentQueryPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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
import static org.mockito.Mockito.doThrow;
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
    private CustomUserDetails userDetails;

    @InjectMocks
    private CommentCommandService commentCommandService;

    private User testUser;
    private Comment testComment;
    private CommentDTO commentDTO;

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

        commentDTO = new CommentDTO();
        commentDTO.setId(200L);
        commentDTO.setContent("수정된 댓글");
    }

    @Test
    @DisplayName("인증된 사용자의 댓글 수정 성공")
    void shouldUpdateComment_WhenAuthenticatedUserOwnsComment() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));

        // When
        commentCommandService.updateComment(commentDTO, userDetails);

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

        CommentDTO anonymousCommentDTO = new CommentDTO();
        anonymousCommentDTO.setId(200L);
        anonymousCommentDTO.setContent("수정된 익명 댓글");
        anonymousCommentDTO.setPassword(1234);

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(anonymousComment));

        // When
        commentCommandService.updateComment(anonymousCommentDTO, null);

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
        assertThatThrownBy(() -> commentCommandService.updateComment(commentDTO, userDetails))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);

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

        CommentDTO wrongPasswordDTO = new CommentDTO();
        wrongPasswordDTO.setId(200L);
        wrongPasswordDTO.setContent("수정된 댓글");
        wrongPasswordDTO.setPassword(9999);

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(passwordComment));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(wrongPasswordDTO, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_PASSWORD_NOT_MATCH);

        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("다른 사용자가 댓글 수정 시 ONLY_COMMENT_OWNER_UPDATE 예외 발생")
    void shouldThrowException_WhenNotCommentOwner() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
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
        assertThatThrownBy(() -> commentCommandService.updateComment(commentDTO, userDetails))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("후손이 없는 댓글 삭제 시 완전 삭제")
    void shouldDeleteComment_WhenNoDescendants() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));
        given(commentClosureQueryPort.hasDescendants(200L)).willReturn(false);

        // When
        commentCommandService.deleteComment(commentDTO, userDetails);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentClosureQueryPort).hasDescendants(200L);
        verify(commentClosureCommandPort).deleteByDescendantId(200L);
        verify(commentCommandPort).delete(testComment);
        verify(commentCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("후손이 있는 댓글 삭제 시 소프트 삭제")
    void shouldSoftDeleteComment_WhenHasDescendants() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));
        given(commentClosureQueryPort.hasDescendants(200L)).willReturn(true);

        // When
        commentCommandService.deleteComment(commentDTO, userDetails);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentClosureQueryPort).hasDescendants(200L);
        verify(commentCommandPort).save(testComment);
        verify(commentClosureCommandPort, never()).deleteByDescendantId(any());
        verify(commentCommandPort, never()).delete(any());
    }

    @Test
    @DisplayName("댓글 삭제 과정에서 예외 발생 시 COMMENT_DELETE_FAILED 예외 발생")
    void shouldThrowException_WhenDeleteProcessFails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));
        doThrow(new RuntimeException("삭제 실패")).when(commentClosureQueryPort).hasDescendants(200L);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.deleteComment(commentDTO, userDetails))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_DELETE_FAILED);

        verify(commentQueryPort).findById(200L);
        verify(commentClosureQueryPort).hasDescendants(200L);
    }

    @Test
    @DisplayName("인증 정보가 null인 상태에서 패스워드 없는 댓글 수정 시 예외 발생")
    void shouldThrowException_WhenNullUserDetailsWithoutPassword() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(commentDTO, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("이미 삭제된 댓글에 대한 작업")
    void shouldHandleDeletedComment() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        Comment deletedComment = Comment.builder()
                .id(200L)
                .content("삭제된 댓글")
                .user(testUser)
                .password(null)
                .deleted(true)
                .build();

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(deletedComment));

        // When
        commentCommandService.updateComment(commentDTO, userDetails);

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

        CommentDTO emptyPasswordDTO = new CommentDTO();
        emptyPasswordDTO.setId(200L);
        emptyPasswordDTO.setContent("수정된 댓글");
        emptyPasswordDTO.setPassword(null);

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(emptyPasswordComment));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(emptyPasswordDTO, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ONLY_COMMENT_OWNER_UPDATE);

        verify(commentQueryPort).findById(200L);
        verify(commentCommandPort, never()).save(any());
    }
}