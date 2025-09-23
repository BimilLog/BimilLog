package jaeik.bimillog.domain.comment.service;

import jaeik.bimillog.domain.comment.application.port.out.*;
import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.global.application.port.out.GlobalUserQueryPort;
import jaeik.bimillog.testutil.TestUsers;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.CommentTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;
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
    @Mock private GlobalUserQueryPort globalUserQueryPort;
    @Mock private CommentToPostPort commentToPostPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentCommandService commentCommandService;

    private User testUser;
    private Comment testComment;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = TestUsers.copyWithId(TestUsers.USER1, TEST_USER_ID);
        testPost = TestFixtures.createPostWithId(300L, testUser, "테스트 게시글", "게시글 내용");
        testComment = CommentTestDataBuilder.createTestComment(testUser, testPost, TEST_ORIGINAL_CONTENT);
        // 리플렉션으로 ID 설정
        TestFixtures.setFieldValue(testComment, "id", TEST_COMMENT_ID);
    }

    @Test
    @DisplayName("댓글 좋아요 추가 성공")
    void shouldAddLike_WhenUserHasNotLikedComment() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);
        given(globalUserQueryPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
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
        given(globalUserQueryPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
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
        verify(globalUserQueryPort, never()).findById(any());
        verify(commentLikePort, never()).isLikedByUser(any(), any());
        verify(commentLikePort, never()).save(any());
        verify(commentLikePort, never()).deleteLikeByIds(anyLong(), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 좋아요 시 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);
        given(globalUserQueryPort.findById(TEST_USER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentCommandService.likeComment(TEST_USER_ID, TEST_COMMENT_ID))
                .isInstanceOf(UserCustomException.class)
                .hasFieldOrPropertyWithValue("userErrorCode", UserErrorCode.USER_NOT_FOUND);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(globalUserQueryPort).findById(TEST_USER_ID);
        verify(commentLikePort, never()).isLikedByUser(any(), any());
        verify(commentLikePort, never()).save(any());
        verify(commentLikePort, never()).deleteLikeByIds(anyLong(), anyLong());
    }

    @Test
    @DisplayName("익명 사용자(null userId)가 좋아요 시 예외 발생")
    void shouldThrowException_WhenUserIdIsNull() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);
        given(globalUserQueryPort.findById(null)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentCommandService.likeComment(null, TEST_COMMENT_ID))
                .isInstanceOf(UserCustomException.class)
                .hasFieldOrPropertyWithValue("userErrorCode", UserErrorCode.USER_NOT_FOUND);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
        verify(globalUserQueryPort).findById(null);
        verify(commentLikePort, never()).isLikedByUser(any(), any());
        verify(commentLikePort, never()).save(any());
        verify(commentLikePort, never()).deleteLikeByIds(anyLong(), anyLong());
    }

    @Test
    @DisplayName("자신의 댓글에 좋아요")
    void shouldAllowSelfLike() {
        // Given
        Comment ownComment = CommentTestDataBuilder.createTestComment(testUser, testPost, "내가 작성한 댓글");
        TestFixtures.setFieldValue(ownComment, "id", 200L);

        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(ownComment);
        given(globalUserQueryPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
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

    // 과도한 테스트 제거 - "여러 번 연속으로 좋아요 토글" 테스트는 단일 토글 테스트로 충분

    @Test
    @DisplayName("인증된 사용자의 댓글 수정 성공")
    void shouldUpdateComment_WhenAuthenticatedUserOwnsComment() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);

        // When
        commentCommandService.updateComment(TEST_COMMENT_ID, TEST_USER_ID, TEST_UPDATED_CONTENT, null);

        // Then
        verify(commentQueryPort).findById(TEST_COMMENT_ID);
    }

    @Test
    @DisplayName("익명 사용자의 패스워드 일치로 댓글 수정 성공")
    void shouldUpdateComment_WhenAnonymousUserWithCorrectPassword() {
        // Given
        Comment anonymousComment = Comment.createComment(testPost, null, "익명 댓글", TEST_PASSWORD);
        TestFixtures.setFieldValue(anonymousComment, "id", TEST_COMMENT_ID);

        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(anonymousComment);

        // When
        commentCommandService.updateComment(TEST_COMMENT_ID, null, "수정된 익명 댓글", TEST_PASSWORD);

        // Then
        verify(commentQueryPort).findById(TEST_COMMENT_ID);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 시 COMMENT_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenCommentNotFoundForUpdate() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willThrow(new CommentCustomException(CommentErrorCode.COMMENT_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(TEST_COMMENT_ID, TEST_USER_ID, TEST_UPDATED_CONTENT, null))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_NOT_FOUND);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
    }

    @Test
    @DisplayName("잘못된 패스워드로 댓글 수정 시 COMMENT_UNAUTHORIZED 예외 발생")
    void shouldThrowException_WhenPasswordNotMatch() {
        // Given
        Comment passwordComment = Comment.createComment(testPost, null, "패스워드 댓글", TEST_PASSWORD);
        TestFixtures.setFieldValue(passwordComment, "id", TEST_COMMENT_ID);

        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(passwordComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(TEST_COMMENT_ID, null, TEST_UPDATED_CONTENT, 9999))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_UNAUTHORIZED);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
    }

    @Test
    @DisplayName("다른 사용자가 댓글 수정 시 ONLY_COMMENT_OWNER_UPDATE 예외 발생")
    void shouldThrowException_WhenNotCommentOwner() {
        // Given
        Long userId = 100L;
        User anotherUser = TestUsers.copyWithId(TestUsers.USER3, 999L);

        Comment anotherUserComment = CommentTestDataBuilder.createTestComment(anotherUser, testPost, "다른 사용자 댓글");
        TestFixtures.setFieldValue(anotherUserComment, "id", 200L);

        given(commentQueryPort.findById(200L)).willReturn(anotherUserComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(200L, userId, TEST_UPDATED_CONTENT, null))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_UNAUTHORIZED);

        verify(commentQueryPort).findById(200L);
    }

    @Test
    @DisplayName("후손이 없는 댓글 삭제 시 완전 삭제")
    void shouldDeleteComment_WhenNoDescendants() {
        // Given
        Long userId = 100L;
        given(commentQueryPort.findById(200L)).willReturn(testComment);

        // When
        commentCommandService.deleteComment(200L, userId, null);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentDeletePort).deleteComment(200L);
    }

    @Test
    @DisplayName("후손이 있는 댓글 삭제 시 소프트 삭제")
    void shouldSoftDeleteComment_WhenHasDescendants() {
        // Given
        Long userId = 100L;
        given(commentQueryPort.findById(200L)).willReturn(testComment);

        // When
        commentCommandService.deleteComment(200L, userId, null);

        // Then
        verify(commentQueryPort).findById(200L);
        verify(commentDeletePort).deleteComment(200L);
    }


    @Test
    @DisplayName("인증 정보가 null인 상태에서 패스워드 없는 댓글 수정 시 예외 발생")
    void shouldThrowException_WhenNullUserDetailsWithoutPassword() {
        // Given
        given(commentQueryPort.findById(200L)).willReturn(testComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(200L, null, TEST_UPDATED_CONTENT, null))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_UNAUTHORIZED);

        verify(commentQueryPort).findById(200L);
    }

    @Test
    @DisplayName("이미 삭제된 댓글에 대한 작업")
    void shouldHandleDeletedComment() {
        // Given
        Long userId = 100L;
        Comment deletedComment = CommentTestDataBuilder.createTestComment(testUser, testPost, "삭제된 댓글");
        TestFixtures.setFieldValue(deletedComment, "id", 200L);
        TestFixtures.setFieldValue(deletedComment, "deleted", true);

        given(commentQueryPort.findById(200L)).willReturn(deletedComment);

        // When
        commentCommandService.updateComment(200L, userId, TEST_UPDATED_CONTENT, null);

        // Then
        verify(commentQueryPort).findById(200L);
    }

    @Test
    @DisplayName("빈 문자열 패스워드로 댓글 수정 시 ONLY_COMMENT_OWNER_UPDATE 예외 발생")
    void shouldThrowException_WhenEmptyPasswordWithoutUser() {
        // Given
        Comment emptyPasswordComment = Comment.createComment(testPost, null, "빈 패스워드 댓글", null);
        TestFixtures.setFieldValue(emptyPasswordComment, "id", 200L);

        given(commentQueryPort.findById(200L)).willReturn(emptyPasswordComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.updateComment(200L, null, "수정된 댓글", null))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_UNAUTHORIZED);

        verify(commentQueryPort).findById(200L);
    }

    // === 누락된 댓글 삭제 테스트 케이스들 ===

    @Test
    @DisplayName("익명 댓글 삭제 - 패스워드 일치로 삭제 성공")
    void shouldDeleteAnonymousComment_WhenCorrectPasswordProvided() {
        // Given
        Comment anonymousComment = Comment.createComment(testPost, null, "익명 댓글", 1234);
        TestFixtures.setFieldValue(anonymousComment, "id", 300L);

        given(commentQueryPort.findById(300L)).willReturn(anonymousComment);

        // When
        commentCommandService.deleteComment(300L, null, 1234);

        // Then
        verify(commentQueryPort).findById(300L);
        verify(commentDeletePort).deleteComment(300L);
    }

    @Test
    @DisplayName("익명 댓글 삭제 - 잘못된 패스워드로 삭제 실패")
    void shouldThrowException_WhenAnonymousCommentWithWrongPassword() {
        // Given
        Comment anonymousComment = Comment.createComment(testPost, null, "익명 댓글", 1234);
        TestFixtures.setFieldValue(anonymousComment, "id", 300L);

        given(commentQueryPort.findById(300L)).willReturn(anonymousComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.deleteComment(300L, null, 9999))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_UNAUTHORIZED);

        verify(commentQueryPort).findById(300L);
        verify(commentDeletePort, never()).deleteComment(any());
    }

    @Test
    @DisplayName("계층 댓글 삭제 - 자손이 있는 인증 사용자 댓글 소프트 삭제")
    void shouldSoftDeleteUserComment_WhenHasDescendants() {
        // Given
        Long userId = 100L;
        Comment parentComment = CommentTestDataBuilder.createTestComment(testUser, testPost, "부모 댓글");
        TestFixtures.setFieldValue(parentComment, "id", 400L);

        given(commentQueryPort.findById(400L)).willReturn(parentComment);

        // When
        commentCommandService.deleteComment(400L, userId, null);

        // Then
        verify(commentQueryPort).findById(400L);
        verify(commentDeletePort).deleteComment(400L);
    }

    @Test
    @DisplayName("계층 댓글 삭제 - 자손이 있는 익명 댓글 소프트 삭제")
    void shouldSoftDeleteAnonymousComment_WhenHasDescendants() {
        // Given
        Comment anonymousParentComment = Comment.createComment(testPost, null, "익명 부모 댓글", 5678);
        TestFixtures.setFieldValue(anonymousParentComment, "id", 500L);

        given(commentQueryPort.findById(500L)).willReturn(anonymousParentComment);

        // When
        commentCommandService.deleteComment(500L, null, 5678);

        // Then
        verify(commentQueryPort).findById(500L);
        verify(commentDeletePort).deleteComment(500L);
    }

    @Test
    @DisplayName("다른 사용자가 댓글 삭제 시도 - ONLY_COMMENT_OWNER_UPDATE 예외 발생")
    void shouldThrowException_WhenNotOwnerTriesToDelete() {
        // Given
        Long requestUserId = 100L;
        User anotherUser = TestUsers.copyWithId(TestUsers.USER3, 999L);

        Comment anotherUserComment = CommentTestDataBuilder.createTestComment(anotherUser, testPost, "다른 사용자 댓글");
        TestFixtures.setFieldValue(anotherUserComment, "id", 600L);

        given(commentQueryPort.findById(600L)).willReturn(anotherUserComment);

        // When & Then
        assertThatThrownBy(() -> commentCommandService.deleteComment(600L, requestUserId, null))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_UNAUTHORIZED);

        verify(commentQueryPort).findById(600L);
        verify(commentDeletePort, never()).deleteComment(any());
    }

    // === 누락된 댓글 작성 테스트 케이스들 ===

    @Test
    @DisplayName("인증 사용자의 일반 댓글 작성 성공")
    void shouldWriteComment_WhenAuthenticatedUser() {
        // Given
        Long postId = 300L;
        String content = "인증 사용자 댓글";
        Comment savedComment = CommentTestDataBuilder.createTestComment(testUser, testPost, content);
        TestFixtures.setFieldValue(savedComment, "id", TEST_COMMENT_ID);

        given(commentToPostPort.findById(postId)).willReturn(testPost);
        given(globalUserQueryPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentSavePort.save(any(Comment.class))).willReturn(savedComment);

        // When
        commentCommandService.writeComment(TEST_USER_ID, postId, null, content, null);

        // Then
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentSavePort).save(commentCaptor.capture());

        Comment capturedComment = commentCaptor.getValue();
        assertThat(capturedComment.getContent()).isEqualTo(content);
        assertThat(capturedComment.getUser()).isEqualTo(testUser);
        assertThat(capturedComment.getPost()).isEqualTo(testPost);
        assertThat(capturedComment.isDeleted()).isFalse();

        verify(commentSavePort).saveAll(any());
        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        CommentCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.postUserId()).isEqualTo(100L);
        assertThat(capturedEvent.commenterName()).isEqualTo("testUser");
        assertThat(capturedEvent.postId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("익명 사용자의 비밀번호 댓글 작성 성공")
    void shouldWriteComment_WhenAnonymousUserWithPassword() {
        // Given
        Long postId = 300L;
        String content = "익명 댓글";
        Integer password = 1234;
        Comment savedComment = Comment.createComment(testPost, null, content, password);
        TestFixtures.setFieldValue(savedComment, "id", TEST_COMMENT_ID);

        given(commentToPostPort.findById(postId)).willReturn(testPost);
        given(commentSavePort.save(any(Comment.class))).willReturn(savedComment);

        // When
        commentCommandService.writeComment(null, postId, null, content, password);

        // Then
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentSavePort).save(commentCaptor.capture());

        Comment capturedComment = commentCaptor.getValue();
        assertThat(capturedComment.getContent()).isEqualTo(content);
        assertThat(capturedComment.getPassword()).isEqualTo(password);
        assertThat(capturedComment.getUser()).isNull();
        assertThat(capturedComment.getPost()).isEqualTo(testPost);
        assertThat(capturedComment.isDeleted()).isFalse();

        verify(commentSavePort).saveAll(any());
        // testPost에 user가 있으므로 이벤트가 발행되어야 함
        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        CommentCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.postUserId()).isEqualTo(100L);
        assertThat(capturedEvent.commenterName()).isEqualTo("익명");
        assertThat(capturedEvent.postId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("대댓글 작성 성공 - 계층 구조 생성")
    void shouldWriteReplyComment_WhenParentCommentExists() {
        // Given
        Long postId = 300L;
        Long parentId = 100L;
        String content = "대댓글";
        Comment savedComment = CommentTestDataBuilder.createTestComment(testUser, testPost, content);
        TestFixtures.setFieldValue(savedComment, "id", TEST_COMMENT_ID);

        Comment parentComment = CommentTestDataBuilder.createTestComment(testUser, testPost, "부모 댓글");
        TestFixtures.setFieldValue(parentComment, "id", parentId);

        CommentClosure parentClosure = CommentClosure.createCommentClosure(parentComment, parentComment, 0);
        List<CommentClosure> parentClosures = Collections.singletonList(parentClosure);

        given(commentToPostPort.findById(postId)).willReturn(testPost);
        given(globalUserQueryPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentSavePort.save(any(Comment.class))).willReturn(savedComment);
        given(commentSavePort.getParentClosures(parentId)).willReturn(Optional.of(parentClosures));

        // When
        commentCommandService.writeComment(TEST_USER_ID, postId, parentId, content, null);

        // Then
        ArgumentCaptor<List<CommentClosure>> closureCaptor = ArgumentCaptor.forClass(List.class);
        verify(commentSavePort).saveAll(closureCaptor.capture());

        List<CommentClosure> capturedClosures = closureCaptor.getValue();
        assertThat(capturedClosures).hasSize(2); // 자기 자신 + 부모와의 관계

        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        CommentCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.postUserId()).isEqualTo(100L);
        assertThat(capturedEvent.commenterName()).isEqualTo("testUser");
        assertThat(capturedEvent.postId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 예외 발생")
    void shouldThrowException_WhenPostNotFound() {
        // Given
        Long postId = 999L;
        given(commentToPostPort.findById(postId)).willThrow(new RuntimeException("게시글을 찾을 수 없습니다."));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.writeComment(TEST_USER_ID, postId, null, "댓글", null))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_WRITE_FAILED);

        verify(commentToPostPort).findById(postId);
        verify(commentSavePort, never()).save(any(Comment.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 부모 댓글에 대댓글 작성 시 예외 발생")
    void shouldThrowException_WhenParentCommentNotFound() {
        // Given
        Long postId = 300L;
        Long parentId = 999L;
        Comment savedComment = CommentTestDataBuilder.createTestComment(testUser, testPost, "대댓글");
        TestFixtures.setFieldValue(savedComment, "id", TEST_COMMENT_ID);

        given(commentToPostPort.findById(postId)).willReturn(testPost);
        given(globalUserQueryPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentSavePort.save(any(Comment.class))).willReturn(savedComment);
        given(commentSavePort.getParentClosures(parentId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentCommandService.writeComment(TEST_USER_ID, postId, parentId, "대댓글", null))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_WRITE_FAILED);

        verify(commentToPostPort).findById(postId);
        verify(commentSavePort).save(any(Comment.class));
        verify(commentSavePort).getParentClosures(parentId);
        verify(eventPublisher, never()).publishEvent(any());
    }

    // === 누락된 테스트 추가 ===

    @Test
    @DisplayName("댓글 작성 시 null content 검증")
    void shouldThrowException_WhenWriteCommentWithNullContent() {
        // Given
        Long postId = 300L;
        given(commentToPostPort.findById(postId)).willReturn(testPost);
        given(globalUserQueryPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> commentCommandService.writeComment(TEST_USER_ID, postId, null, null, null))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_WRITE_FAILED);

        verify(commentSavePort, never()).save(any(Comment.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("댓글 수정 시 빈 문자열 content 검증")
    void shouldThrowException_WhenUpdateCommentWithEmptyContent() {
        // Given
        given(commentQueryPort.findById(TEST_COMMENT_ID)).willReturn(testComment);

        // When & Then  
        assertThatThrownBy(() -> commentCommandService.updateComment(TEST_COMMENT_ID, TEST_USER_ID, "", null))
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", CommentErrorCode.COMMENT_UPDATE_FAILED);

        verify(commentQueryPort).findById(TEST_COMMENT_ID);
    }

    @Test
    @DisplayName("대댓글의 대댓글 작성 (depth > 1) 테스트")
    void shouldWriteNestedReplyComment_WhenDepthGreaterThanOne() {
        // Given
        Long postId = 300L;
        Long parentId = 100L;
        Long grandParentId = 50L;
        String content = "대댓글의 대댓글";
        
        Comment savedComment = CommentTestDataBuilder.createTestComment(testUser, testPost, content);
        TestFixtures.setFieldValue(savedComment, "id", TEST_COMMENT_ID);

        Comment parentComment = CommentTestDataBuilder.createTestComment(testUser, testPost, "부모 댓글");
        TestFixtures.setFieldValue(parentComment, "id", parentId);
        
        Comment grandParentComment = CommentTestDataBuilder.createTestComment(testUser, testPost, "조부모 댓글");
        TestFixtures.setFieldValue(grandParentComment, "id", grandParentId);

        // 클로저 테이블 관계: grandParent -> parent -> child
        CommentClosure grandParentClosure = CommentClosure.createCommentClosure(grandParentComment, grandParentComment, 0);
        CommentClosure parentToGrandParent = CommentClosure.createCommentClosure(grandParentComment, parentComment, 1);
        CommentClosure parentClosure = CommentClosure.createCommentClosure(parentComment, parentComment, 0);
        
        List<CommentClosure> parentClosures = List.of(parentToGrandParent, parentClosure);

        given(commentToPostPort.findById(postId)).willReturn(testPost);
        given(globalUserQueryPort.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(commentSavePort.save(any(Comment.class))).willReturn(savedComment);
        given(commentSavePort.getParentClosures(parentId)).willReturn(Optional.of(parentClosures));

        // When
        commentCommandService.writeComment(TEST_USER_ID, postId, parentId, content, null);

        // Then
        ArgumentCaptor<List<CommentClosure>> closureCaptor = ArgumentCaptor.forClass(List.class);
        verify(commentSavePort).saveAll(closureCaptor.capture());

        List<CommentClosure> capturedClosures = closureCaptor.getValue();
        // depth가 2인 관계가 포함되어야 함 (조부모와의 관계)
        assertThat(capturedClosures).hasSize(3); // 자기 자신 + 부모와의 관계 + 조부모와의 관계
        
        verify(eventPublisher).publishEvent(any(CommentCreatedEvent.class));
    }
}