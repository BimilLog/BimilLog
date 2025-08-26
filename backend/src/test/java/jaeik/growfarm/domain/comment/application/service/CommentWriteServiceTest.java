package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentClosure;
import jaeik.growfarm.domain.comment.event.CommentCreatedEvent;
import jaeik.growfarm.domain.post.entity.Post;
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
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>CommentWriteService 단위 테스트</h2>
 * <p>댓글 작성 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentWriteService 단위 테스트")
class CommentWriteServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LoadPostPort loadPostPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private CommentCommandPort commentCommandPort;

    @Mock
    private CommentQueryPort commentQueryPort;

    @Mock
    private CommentClosureCommandPort commentClosureCommandPort;

    @Mock
    private CommentClosureQueryPort commentClosureQueryPort;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private CommentWriteService commentWriteService;

    private User testUser;
    private Post testPost;
    private Comment testComment;
    private Comment parentComment;
    private CommentRequest commentRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(100L)
                .userName("testUser")
                .socialId("kakao123")
                .build();

        User postOwner = User.builder()
                .id(200L)
                .userName("postOwner")
                .socialId("kakao456")
                .build();

        testPost = Post.builder()
                .id(300L)
                .title("테스트 게시글")
                .content("테스트 내용")
                .user(postOwner)
                .build();

        testComment = Comment.builder()
                .id(400L)
                .content("테스트 댓글")
                .user(testUser)
                .post(testPost)
                .deleted(false)
                .build();

        parentComment = Comment.builder()
                .id(500L)
                .content("부모 댓글")
                .user(testUser)
                .post(testPost)
                .deleted(false)
                .build();

        commentRequest = CommentRequest.builder()
                .postId(300L)
                .content("테스트 댓글 내용")
                .password(1234)
                .build();
    }

    @Test
    @DisplayName("인증된 사용자의 댓글 작성 성공")
    void shouldWriteComment_WhenAuthenticatedUser() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentCommandPort.save(any(Comment.class))).willReturn(testComment);
        willDoNothing().given(commentClosureCommandPort).save(any(CommentClosure.class));

        // When
        commentWriteService.writeComment(userDetails, commentRequest);

        // Then
        verify(loadPostPort).findById(300L);
        verify(loadUserPort).findById(100L);
        verify(commentCommandPort).save(any(Comment.class));
        verify(commentClosureCommandPort).save(any(CommentClosure.class));

        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        CommentCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getPostUserId()).isEqualTo(200L); // 게시글 작성자 ID
        assertThat(capturedEvent.getPostId()).isEqualTo(300L);
        assertThat(capturedEvent.getCommenterName()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("익명 사용자의 댓글 작성 성공")
    void shouldWriteComment_WhenAnonymousUser() {
        // Given
        CommentRequest anonymousRequest = CommentRequest.builder()
                .postId(300L)
                .content("테스트 댓글 내용")
                .password(1234)
                .build();
        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(commentCommandPort.save(any(Comment.class))).willReturn(testComment);
        willDoNothing().given(commentClosureCommandPort).save(any(CommentClosure.class));

        // When
        commentWriteService.writeComment(null, anonymousRequest);

        // Then
        verify(loadPostPort).findById(300L);
        verify(loadUserPort, never()).findById(any());
        verify(commentCommandPort).save(any(Comment.class));
        verify(commentClosureCommandPort).save(any(CommentClosure.class));

        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        CommentCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getPostUserId()).isEqualTo(200L);
        assertThat(capturedEvent.getPostId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("대댓글 작성 성공 - 클로저 테이블 업데이트")
    void shouldWriteReplyComment_WithClosureTableUpdate() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        CommentRequest replyRequest = CommentRequest.builder()
                .postId(300L)
                .content("테스트 댓글 내용")
                .password(1234)
                .parentId(500L)
                .build();

        List<CommentClosure> parentClosures = Arrays.asList(
                CommentClosure.createCommentClosure(parentComment, parentComment, 0), // 자기 자신
                CommentClosure.createCommentClosure(parentComment, parentComment, 1)  // 부모와의 관계
        );

        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentCommandPort.save(any(Comment.class))).willReturn(testComment);
        given(commentQueryPort.findById(500L)).willReturn(Optional.of(parentComment));
        given(commentClosureQueryPort.findByDescendantId(500L)).willReturn(Optional.of(parentClosures));
        willDoNothing().given(commentClosureCommandPort).save(any(CommentClosure.class));

        // When
        commentWriteService.writeComment(userDetails, replyRequest);

        // Then
        verify(commentQueryPort).findById(500L);
        verify(commentClosureQueryPort).findByDescendantId(500L);
        // 자기 자신 클로저 + 부모 클로저들의 수만큼 save 호출
        verify(commentClosureCommandPort, times(3)).save(any(CommentClosure.class));

        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 POST_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenPostNotFound() {
        // Given
        given(loadPostPort.findById(300L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentWriteService.writeComment(userDetails, commentRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(loadPostPort).findById(300L);
        verify(loadUserPort, never()).findById(any());
        verify(commentCommandPort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 댓글 작성 시 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(loadUserPort.findById(100L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentWriteService.writeComment(userDetails, commentRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(loadPostPort).findById(300L);
        verify(loadUserPort).findById(100L);
        verify(commentCommandPort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 부모 댓글에 대댓글 작성 시 PARENT_COMMENT_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenParentCommentNotFound() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        CommentRequest requestWithInvalidParent = CommentRequest.builder()
                .postId(300L)
                .content("테스트 댓글 내용")
                .password(1234)
                .parentId(999L) // 존재하지 않는 부모 댓글 ID
                .build();

        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentCommandPort.save(any(Comment.class))).willReturn(testComment);
        willDoNothing().given(commentClosureCommandPort).save(any(CommentClosure.class));
        given(commentQueryPort.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentWriteService.writeComment(userDetails, requestWithInvalidParent))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARENT_COMMENT_NOT_FOUND);

        verify(commentQueryPort).findById(999L);
        verify(commentClosureQueryPort, never()).findByDescendantId(any());
    }

    @Test
    @DisplayName("부모 댓글의 클로저 테이블 조회 실패 시 PARENT_COMMENT_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenParentCommentClosureNotFound() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        CommentRequest requestWithParent = CommentRequest.builder()
                .postId(300L)
                .content("테스트 댓글 내용")
                .password(1234)
                .parentId(500L)
                .build();

        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentCommandPort.save(any(Comment.class))).willReturn(testComment);
        willDoNothing().given(commentClosureCommandPort).save(any(CommentClosure.class));
        given(commentQueryPort.findById(500L)).willReturn(Optional.of(parentComment));
        given(commentClosureQueryPort.findByDescendantId(500L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentWriteService.writeComment(userDetails, requestWithParent))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARENT_COMMENT_NOT_FOUND);

        verify(commentQueryPort).findById(500L);
        verify(commentClosureQueryPort).findByDescendantId(500L);
    }

    @Test
    @DisplayName("게시글 작성자가 없는 경우 이벤트 발행하지 않음")
    void shouldNotPublishEvent_WhenPostOwnerIsNull() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        Post postWithoutOwner = Post.builder()
                .id(300L)
                .title("익명 게시글")
                .content("익명 내용")
                .user(null) // 게시글 작성자 없음
                .build();

        given(loadPostPort.findById(300L)).willReturn(Optional.of(postWithoutOwner));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentCommandPort.save(any(Comment.class))).willReturn(testComment);
        willDoNothing().given(commentClosureCommandPort).save(any(CommentClosure.class));

        // When
        commentWriteService.writeComment(userDetails, commentRequest);

        // Then
        verify(commentCommandPort).save(any(Comment.class));
        verify(commentClosureCommandPort).save(any(CommentClosure.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("댓글 저장 중 예외 발생 시 COMMENT_WRITE_FAILED 예외 발생")
    void shouldThrowException_WhenCommentSaveFails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentCommandPort.save(any(Comment.class))).willThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> commentWriteService.writeComment(userDetails, commentRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_WRITE_FAILED);

        verify(commentCommandPort).save(any(Comment.class));
        verify(commentClosureCommandPort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("클로저 테이블 저장 중 예외 발생 시 COMMENT_WRITE_FAILED 예외 발생")
    void shouldThrowException_WhenClosureSaveFails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentCommandPort.save(any(Comment.class))).willReturn(testComment);
        willThrow(new RuntimeException("Closure error")).given(commentClosureCommandPort).save(any(CommentClosure.class));

        // When & Then
        assertThatThrownBy(() -> commentWriteService.writeComment(userDetails, commentRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_WRITE_FAILED);

        verify(commentCommandPort).save(any(Comment.class));
        verify(commentClosureCommandPort).save(any(CommentClosure.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("다중 대댓글 계층 구조 처리")
    void shouldHandleMultiLevelReplyStructure() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        CommentRequest replyRequest = CommentRequest.builder()
                .postId(300L)
                .content("테스트 댓글 내용")
                .password(1234)
                .parentId(500L)
                .build();

        // 부모 댓글의 클로저 관계: 깊이 0(자기 자신), 깊이 1(부모와의 관계)
        List<CommentClosure> parentClosures = Arrays.asList(
                CommentClosure.createCommentClosure(parentComment, parentComment, 0),
                CommentClosure.createCommentClosure(parentComment, parentComment, 1),
                CommentClosure.createCommentClosure(parentComment, parentComment, 2)
        );

        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentCommandPort.save(any(Comment.class))).willReturn(testComment);
        given(commentQueryPort.findById(500L)).willReturn(Optional.of(parentComment));
        given(commentClosureQueryPort.findByDescendantId(500L)).willReturn(Optional.of(parentClosures));
        willDoNothing().given(commentClosureCommandPort).save(any(CommentClosure.class));

        // When
        commentWriteService.writeComment(userDetails, replyRequest);

        // Then
        // 자기 자신 클로저(1개) + 부모 클로저들(3개) = 총 4개 클로저 저장
        verify(commentClosureCommandPort, times(4)).save(any(CommentClosure.class));

        ArgumentCaptor<CommentClosure> closureCaptor = ArgumentCaptor.forClass(CommentClosure.class);
        verify(commentClosureCommandPort, times(4)).save(closureCaptor.capture());

        List<CommentClosure> savedClosures = closureCaptor.getAllValues();
        // 첫 번째는 자기 자신과의 클로저 (depth = 0)
        assertThat(savedClosures.getFirst().getDepth()).isEqualTo(0);
        assertThat(savedClosures.getFirst().getAncestor()).isEqualTo(testComment);
        assertThat(savedClosures.getFirst().getDescendant()).isEqualTo(testComment);
    }

    @Test
    @DisplayName("긴 댓글 내용 처리")
    void shouldHandleLongCommentContent() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        String longContent = "이것은 매우 긴 댓글 내용입니다. ".repeat(20); // 약 600자
        CommentRequest longContentRequest = CommentRequest.builder()
                .postId(300L)
                .content(longContent)
                .password(1234)
                .build();

        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentCommandPort.save(any(Comment.class))).willReturn(testComment);
        willDoNothing().given(commentClosureCommandPort).save(any(CommentClosure.class));

        // When
        commentWriteService.writeComment(userDetails, longContentRequest);

        // Then
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentCommandPort).save(commentCaptor.capture());

        Comment savedComment = commentCaptor.getValue();
        assertThat(savedComment.getContent()).isEqualTo(longContent);
        verify(commentClosureCommandPort).save(any(CommentClosure.class));
        verify(eventPublisher).publishEvent(any(CommentCreatedEvent.class));
    }

    @Test
    @DisplayName("특수 문자가 포함된 댓글 내용 처리")
    void shouldHandleSpecialCharactersInContent() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        String specialContent = "특수문자 테스트: !@#$%^&*()_+{}|:\"<>?[];',./`~";
        CommentRequest specialContentRequest = CommentRequest.builder()
                .postId(300L)
                .content(specialContent)
                .password(1234)
                .build();

        given(loadPostPort.findById(300L)).willReturn(Optional.of(testPost));
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(commentCommandPort.save(any(Comment.class))).willReturn(testComment);
        willDoNothing().given(commentClosureCommandPort).save(any(CommentClosure.class));

        // When
        commentWriteService.writeComment(userDetails, specialContentRequest);

        // Then
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentCommandPort).save(commentCaptor.capture());

        Comment savedComment = commentCaptor.getValue();
        assertThat(savedComment.getContent()).isEqualTo(specialContent);
        verify(eventPublisher).publishEvent(any(CommentCreatedEvent.class));
    }
}