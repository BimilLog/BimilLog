package jaeik.bimillog.domain.comment.service;

import jaeik.bimillog.domain.comment.application.port.out.CommentLikePort;
import jaeik.bimillog.domain.comment.application.port.out.CommentQueryPort;
import jaeik.bimillog.domain.comment.application.port.out.CommentToUserPort;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
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
    private CommentToUserPort commentToUserPort;

    @Mock
    private CommentLikePort commentLikePort;

    @Mock
    private CommentQueryPort commentQueryPort;

    @InjectMocks
    private CommentLikeService commentLikeService;

    private User testUser;
    private Comment testComment;
    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_COMMENT_ID = 200L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .userName("testUser")
                .socialId("kakao123")
                .build();

        testComment = Comment.builder()
                .id(TEST_COMMENT_ID)
                .content("테스트 댓글")
                .user(testUser)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("댓글 좋아요 추가 성공")
    void shouldAddLike_WhenUserHasNotLikedComment() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(Optional.of(testComment));
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(false);

        // When
        commentLikeService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);

        // Then
        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikePort).save(likeCaptor.capture());
        
        CommentLike capturedLike = likeCaptor.getValue();
        assertThat(capturedLike.getComment()).isEqualTo(testComment);
        assertThat(capturedLike.getUser()).isEqualTo(testUser);
        
        verify(commentLikePort, never()).deleteLike(any(), any());
    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void shouldRemoveLike_WhenUserHasAlreadyLikedComment() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(Optional.of(testComment));
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(true);

        // When
        commentLikeService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);

        // Then
        verify(commentLikePort).deleteLikeByIds(TEST_COMMENT_ID, TEST_USER_ID);
        verify(commentLikePort, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 댓글에 좋아요 시 COMMENT_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenCommentNotFound() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentLikeService.likeComment(TEST_USER_ID, TEST_COMMENT_ID))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_NOT_FOUND);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(commentToUserPort, never()).findById(any());
        verify(commentLikePort, never()).isLikedByUser(any(), any());
        verify(commentLikePort, never()).save(any());
        verify(commentLikePort, never()).deleteLike(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 좋아요 시 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(Optional.of(testComment));
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentLikeService.likeComment(TEST_USER_ID, TEST_COMMENT_ID))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.USER_NOT_FOUND);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(commentToUserPort).findById(TEST_USER_ID);
        verify(commentLikePort, never()).isLikedByUser(any(), any());
        verify(commentLikePort, never()).save(any());
        verify(commentLikePort, never()).deleteLike(any(), any());
    }

    @Test
    @DisplayName("익명 사용자(null userId)가 좋아요 시 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenUserIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> commentLikeService.likeComment(null, TEST_COMMENT_ID))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.USER_NOT_FOUND);

        verify(commentQueryPort, never()).findById(any());
        verify(commentToUserPort, never()).findById(any());
        verify(commentLikePort, never()).isLikedByUser(any(), any());
        verify(commentLikePort, never()).save(any());
        verify(commentLikePort, never()).deleteLike(any(), any());
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

        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(Optional.of(ownComment));
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(false);

        // When
        commentLikeService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);

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
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(Optional.of(testComment));
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));

        // 첫 번째: 좋아요 추가
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(false);
        commentLikeService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);
        verify(commentLikePort).save(any());

        // 두 번째: 좋아요 취소
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(true);
        commentLikeService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);
        verify(commentLikePort).deleteLikeByIds(TEST_COMMENT_ID, TEST_USER_ID);

        // 세 번째: 다시 좋아요 추가
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID)).willReturn(false);
        commentLikeService.likeComment(TEST_USER_ID, TEST_COMMENT_ID);
        
        // Then
        verify(commentLikePort, times(2)).save(any()); // 총 2번 호출
        verify(commentLikePort, times(1)).deleteLikeByIds(TEST_COMMENT_ID, TEST_USER_ID); // 1번 호출
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

        given(commentQueryPort.findById(201L)).willReturn(Optional.of(anotherComment));
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentLikePort.isLikedByUser(201L, TEST_USER_ID)).willReturn(false);

        // When
        commentLikeService.likeComment(TEST_USER_ID, 201L);

        // Then
        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikePort).save(likeCaptor.capture());
        
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

        given(commentQueryPort.findById(202L)).willReturn(Optional.of(anonymousComment));
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentLikePort.isLikedByUser(202L, TEST_USER_ID)).willReturn(false);

        // When
        commentLikeService.likeComment(TEST_USER_ID, 202L);

        // Then
        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikePort).save(likeCaptor.capture());
        
        CommentLike capturedLike = likeCaptor.getValue();
        assertThat(capturedLike.getComment()).isEqualTo(anonymousComment);
        assertThat(capturedLike.getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("좋아요 상태 확인 실패 시 예외 처리")
    void shouldHandleException_WhenCheckingLikeStatusFails() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(Optional.of(testComment));
        given(commentToUserPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentLikePort.isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID))
                .willThrow(new RuntimeException("좋아요 상태 확인 실패"));

        // When & Then
        assertThatThrownBy(() -> commentLikeService.likeComment(TEST_USER_ID, TEST_COMMENT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("좋아요 상태 확인 실패");

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(commentToUserPort).findById(TEST_USER_ID);
        verify(commentLikePort).isLikedByUser(TEST_COMMENT_ID, TEST_USER_ID);
        verify(commentLikePort, never()).save(any());
        verify(commentLikePort, never()).deleteLike(any(), any());
    }
}