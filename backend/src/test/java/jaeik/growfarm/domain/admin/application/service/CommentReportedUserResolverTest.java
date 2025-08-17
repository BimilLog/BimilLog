package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.user.entity.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>CommentReportedUserResolver 단위 테스트</h2>
 * <p>댓글 신고 사용자 해결사의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentReportedUserResolver 단위 테스트")
class CommentReportedUserResolverTest {

    @Mock
    private CommentQueryUseCase commentQueryUseCase;

    @InjectMocks
    private CommentReportedUserResolver commentReportedUserResolver;

    private Comment testComment;
    private User testUser;

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
                .build();
    }

    @Test
    @DisplayName("유효한 댓글 ID로 사용자 해결 시 성공")
    void shouldResolveUser_WhenValidCommentId() {
        // Given
        Long commentId = 200L;
        given(commentQueryUseCase.findById(commentId)).willReturn(Optional.of(testComment));

        // When
        User result = commentReportedUserResolver.resolve(commentId);

        // Then
        assertThat(result).isEqualTo(testUser);
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUserName()).isEqualTo("testUser");
        verify(commentQueryUseCase).findById(commentId);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 ID로 해결 시 COMMENT_FAILED 예외 발생")
    void shouldThrowException_WhenCommentNotFound() {
        // Given
        Long nonExistentCommentId = 999L;
        given(commentQueryUseCase.findById(nonExistentCommentId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentReportedUserResolver.resolve(nonExistentCommentId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_FAILED);

        verify(commentQueryUseCase).findById(nonExistentCommentId);
    }

    @Test
    @DisplayName("지원하는 신고 유형이 COMMENT인지 확인")
    void shouldSupportCommentReportType() {
        // When
        ReportType supportedType = commentReportedUserResolver.supports();

        // Then
        assertThat(supportedType).isEqualTo(ReportType.COMMENT);
    }

    @Test
    @DisplayName("댓글이 존재하지만 사용자가 null인 경우 처리")
    void shouldHandleCommentWithNullUser() {
        // Given
        Long commentId = 200L;
        Comment commentWithNullUser = Comment.builder()
                .id(commentId)
                .content("사용자가 없는 댓글")
                .user(null)
                .build();
        
        given(commentQueryUseCase.findById(commentId)).willReturn(Optional.of(commentWithNullUser));

        // When
        User result = commentReportedUserResolver.resolve(commentId);

        // Then
        assertThat(result).isNull();
        verify(commentQueryUseCase).findById(commentId);
    }

    @Test
    @DisplayName("다양한 댓글 ID로 사용자 해결 테스트")
    void shouldResolveUsersForDifferentCommentIds() {
        // Given
        Long[] commentIds = {1L, 2L, 3L};
        User[] users = {
                User.builder().id(101L).userName("user1").build(),
                User.builder().id(102L).userName("user2").build(),
                User.builder().id(103L).userName("user3").build()
        };

        for (int i = 0; i < commentIds.length; i++) {
            Comment comment = Comment.builder()
                    .id(commentIds[i])
                    .content("댓글 " + (i + 1))
                    .user(users[i])
                    .build();
            given(commentQueryUseCase.findById(commentIds[i])).willReturn(Optional.of(comment));
        }

        // When & Then
        for (int i = 0; i < commentIds.length; i++) {
            User result = commentReportedUserResolver.resolve(commentIds[i]);
            assertThat(result).isEqualTo(users[i]);
            assertThat(result.getId()).isEqualTo(users[i].getId());
            assertThat(result.getUserName()).isEqualTo(users[i].getUserName());
        }

        // 모든 호출이 이루어졌는지 검증
        for (Long commentId : commentIds) {
            verify(commentQueryUseCase).findById(commentId);
        }
    }

    @Test
    @DisplayName("대댓글 사용자 해결 테스트")
    void shouldResolveUserForReplyComment() {
        // Given
        Long replyCommentId = 500L;
        User replyUser = User.builder()
                .id(200L)
                .userName("replyUser")
                .socialId("kakao456")
                .build();

        Comment replyComment = Comment.builder()
                .id(replyCommentId)
                .content("대댓글입니다")
                .user(replyUser)
                .build();

        given(commentQueryUseCase.findById(replyCommentId)).willReturn(Optional.of(replyComment));

        // When
        User result = commentReportedUserResolver.resolve(replyCommentId);

        // Then
        assertThat(result).isEqualTo(replyUser);
        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getUserName()).isEqualTo("replyUser");
        verify(commentQueryUseCase).findById(replyCommentId);
    }

    @Test
    @DisplayName("익명 댓글 사용자 해결 테스트")
    void shouldResolveAnonymousCommentUser() {
        // Given
        Long anonymousCommentId = 600L;
        User anonymousUser = User.builder()
                .id(null)
                .userName("익명")
                .socialId("anonymous")
                .build();

        Comment anonymousComment = Comment.builder()
                .id(anonymousCommentId)
                .content("익명 댓글")
                .user(anonymousUser)
                .build();

        given(commentQueryUseCase.findById(anonymousCommentId)).willReturn(Optional.of(anonymousComment));

        // When
        User result = commentReportedUserResolver.resolve(anonymousCommentId);

        // Then
        assertThat(result).isEqualTo(anonymousUser);
        assertThat(result.getUserName()).isEqualTo("익명");
        assertThat(result.getSocialId()).isEqualTo("anonymous");
        verify(commentQueryUseCase).findById(anonymousCommentId);
    }

    @Test
    @DisplayName("0이나 음수 댓글 ID로 해결 시도시 처리")
    void shouldHandleInvalidCommentIds() {
        // Given
        Long[] invalidIds = {0L, -1L, -100L};

        for (Long invalidId : invalidIds) {
            given(commentQueryUseCase.findById(invalidId)).willReturn(Optional.empty());
        }

        // When & Then
        for (Long invalidId : invalidIds) {
            assertThatThrownBy(() -> commentReportedUserResolver.resolve(invalidId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_FAILED);
            verify(commentQueryUseCase).findById(invalidId);
        }
    }
}