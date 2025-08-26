package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.out.CommentLikeCommandPort;
import jaeik.growfarm.domain.comment.application.port.out.CommentLikeQueryPort;
import jaeik.growfarm.domain.comment.application.port.out.CommentQueryPort;
import jaeik.growfarm.domain.comment.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.comment.entity.CommentRequest;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>CommentLikeService 단위 테스트</h2>
 * <p>댓글 좋아요 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentLikeService 단위 테스트")
class CommentLikeServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private CommentLikeCommandPort commentLikeCommandPort;

    @Mock
    private CommentLikeQueryPort commentLikeQueryPort;

    @Mock
    private CommentQueryPort commentQueryPort;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private CommentLikeService commentLikeService;

    private User testUser;
    private Comment testComment;
    private CommentRequest commentRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(100L)
                .userName("testUser")
                .socialId("kakao123")
                .build();

        testComment = Comment.builder()
                .id(200L)
                .content("테스트 댓글")
                .user(testUser)
                .deleted(false)
                .build();

        commentRequest = CommentRequest.builder()
                .id(200L)
                .content("테스트 댓글")
                .build();

        given(userDetails.getUserId()).willReturn(100L);
    }

    @Test
    @DisplayName("댓글 좋아요 추가 성공")
    void shouldAddLike_WhenUserHasNotLikedComment() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentLikeQueryPort.isLikedByUser(200L, 100L)).willReturn(false);

        // When
        commentLikeService.likeComment(commentRequest, userDetails);

        // Then
        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikeCommandPort).save(likeCaptor.capture());
        
        CommentLike capturedLike = likeCaptor.getValue();
        assertThat(capturedLike.getComment()).isEqualTo(testComment);
        assertThat(capturedLike.getUser()).isEqualTo(testUser);
        
        verify(commentLikeCommandPort, never()).deleteLike(any(), any());
    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void shouldRemoveLike_WhenUserHasAlreadyLikedComment() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentLikeQueryPort.isLikedByUser(200L, 100L)).willReturn(true);

        // When
        commentLikeService.likeComment(commentRequest, userDetails);

        // Then
        verify(commentLikeCommandPort).deleteLike(testComment, testUser);
        verify(commentLikeCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 댓글에 좋아요 시 COMMENT_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenCommentNotFound() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentLikeService.likeComment(commentRequest, userDetails))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);

        verify(commentQueryPort).findById(200L);
        verify(loadUserPort, never()).findById(any());
        verify(commentLikeQueryPort, never()).isLikedByUser(any(), any());
        verify(commentLikeCommandPort, never()).save(any());
        verify(commentLikeCommandPort, never()).deleteLike(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 좋아요 시 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));
        given(loadUserPort.findById(100L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentLikeService.likeComment(commentRequest, userDetails))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(commentQueryPort).findById(200L);
        verify(loadUserPort).findById(100L);
        verify(commentLikeQueryPort, never()).isLikedByUser(any(), any());
        verify(commentLikeCommandPort, never()).save(any());
        verify(commentLikeCommandPort, never()).deleteLike(any(), any());
    }

    @Test
    @DisplayName("삭제된 댓글에 좋아요 시도")
    void shouldHandleDeletedComment() {
        // Given
        Comment deletedComment = Comment.builder()
                .id(200L)
                .content("삭제된 댓글")
                .user(testUser)
                .deleted(true)
                .build();

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(deletedComment));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentLikeQueryPort.isLikedByUser(200L, 100L)).willReturn(false);

        // When
        commentLikeService.likeComment(commentRequest, userDetails);

        // Then
        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikeCommandPort).save(likeCaptor.capture());
        
        CommentLike capturedLike = likeCaptor.getValue();
        assertThat(capturedLike.getComment()).isEqualTo(deletedComment);
        assertThat(capturedLike.getUser()).isEqualTo(testUser);
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

        given(commentQueryPort.findById(200L)).willReturn(Optional.of(ownComment));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentLikeQueryPort.isLikedByUser(200L, 100L)).willReturn(false);

        // When
        commentLikeService.likeComment(commentRequest, userDetails);

        // Then
        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikeCommandPort).save(likeCaptor.capture());
        
        CommentLike capturedLike = likeCaptor.getValue();
        assertThat(capturedLike.getComment()).isEqualTo(ownComment);
        assertThat(capturedLike.getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("여러 번 연속으로 좋아요 토글")
    void shouldToggleLikeMultipleTimes() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));

        // 첫 번째: 좋아요 추가
        given(commentLikeQueryPort.isLikedByUser(200L, 100L)).willReturn(false);
        commentLikeService.likeComment(commentRequest, userDetails);
        verify(commentLikeCommandPort).save(any());

        // 두 번째: 좋아요 취소
        given(commentLikeQueryPort.isLikedByUser(200L, 100L)).willReturn(true);
        commentLikeService.likeComment(commentRequest, userDetails);
        verify(commentLikeCommandPort).deleteLike(testComment, testUser);

        // 세 번째: 다시 좋아요 추가
        given(commentLikeQueryPort.isLikedByUser(200L, 100L)).willReturn(false);
        commentLikeService.likeComment(commentRequest, userDetails);
        
        // Then
        verify(commentLikeCommandPort, times(2)).save(any()); // 총 2번 호출
        verify(commentLikeCommandPort, times(1)).deleteLike(testComment, testUser); // 1번 호출
    }

    @Test
    @DisplayName("다른 사용자들의 댓글에 좋아요")
    void shouldLikeDifferentUsersComments() {
        // Given
        User anotherUser = User.builder()
                .id(101L)
                .userName("anotherUser")
                .socialId("kakao456")
                .build();

        Comment anotherComment = Comment.builder()
                .id(201L)
                .content("다른 사용자 댓글")
                .user(anotherUser)
                .deleted(false)
                .build();

        CommentRequest anotherCommentRequest = CommentRequest.builder()
                .id(201L)
                .content("다른 사용자 댓글")
                .build();

        given(commentQueryPort.findById(201L)).willReturn(Optional.of(anotherComment));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentLikeQueryPort.isLikedByUser(201L, 100L)).willReturn(false);

        // When
        commentLikeService.likeComment(anotherCommentRequest, userDetails);

        // Then
        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikeCommandPort).save(likeCaptor.capture());
        
        CommentLike capturedLike = likeCaptor.getValue();
        assertThat(capturedLike.getComment()).isEqualTo(anotherComment);
        assertThat(capturedLike.getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("익명 댓글에 좋아요")
    void shouldLikeAnonymousComment() {
        // Given
        Comment anonymousComment = Comment.builder()
                .id(202L)
                .content("익명 댓글")
                .user(null)
                .password(1234)
                .deleted(false)
                .build();

        CommentRequest anonymousCommentRequest = CommentRequest.builder()
                .id(202L)
                .content("익명 댓글")
                .build();

        given(commentQueryPort.findById(202L)).willReturn(Optional.of(anonymousComment));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentLikeQueryPort.isLikedByUser(202L, 100L)).willReturn(false);

        // When
        commentLikeService.likeComment(anonymousCommentRequest, userDetails);

        // Then
        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikeCommandPort).save(likeCaptor.capture());
        
        CommentLike capturedLike = likeCaptor.getValue();
        assertThat(capturedLike.getComment()).isEqualTo(anonymousComment);
        assertThat(capturedLike.getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("좋아요 상태 확인 실패 시 예외 처리")
    void shouldHandleException_WhenCheckingLikeStatusFails() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(Optional.of(testComment));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentLikeQueryPort.isLikedByUser(200L, 100L))
                .willThrow(new RuntimeException("좋아요 상태 확인 실패"));

        // When & Then
        assertThatThrownBy(() -> commentLikeService.likeComment(commentRequest, userDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("좋아요 상태 확인 실패");

        verify(commentQueryPort).findById(200L);
        verify(loadUserPort).findById(100L);
        verify(commentLikeQueryPort).isLikedByUser(200L, 100L);
        verify(commentLikeCommandPort, never()).save(any());
        verify(commentLikeCommandPort, never()).deleteLike(any(), any());
    }
}