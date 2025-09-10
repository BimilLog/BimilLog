package jaeik.bimillog.infrastructure.adapter.comment.comment;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.comment.out.comment.CommentSaveAdapter;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <h2>댓글 저장 어댑터 단위 테스트</h2>
 * <p>CommentSaveAdapter의 댓글 저장 동작을 검증하는 단위 테스트</p>
 * <p>Mock을 사용하여 Repository 의존성을 격리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("댓글 저장 어댑터 단위 테스트")
class CommentSaveAdapterUnitTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentSaveAdapter commentSaveAdapter;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        Setting setting = Setting.createSetting();
        
        testUser = User.builder()
                .socialId("kakao_test_123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(setting)
                .build();

        testPost = Post.builder()
                .user(testUser)
                .title("테스트 게시글")
                .content("테스트 내용")
                .isNotice(false)
                .views(0)
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - 새로운 댓글 저장")
    void shouldSaveNewComment_WhenValidCommentProvided() {
        // Given
        Comment newComment = Comment.createComment(testPost, testUser, "테스트 댓글 내용", null);
        Comment savedComment = Comment.createComment(testPost, testUser, "테스트 댓글 내용", null);
        
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        // When
        Comment result = commentSaveAdapter.save(newComment);

        // Then
        verify(commentRepository).save(newComment);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("테스트 댓글 내용");
        assertThat(result.getPost()).isEqualTo(testPost);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getPassword()).isNull();
    }

    @Test
    @DisplayName("정상 케이스 - 익명 댓글(비밀번호) 저장")
    void shouldSaveAnonymousComment_WhenPasswordProvided() {
        // Given
        Comment anonymousComment = Comment.createComment(testPost, null, "익명 댓글 내용", 1234);
        Comment savedComment = Comment.createComment(testPost, null, "익명 댓글 내용", 1234);
        
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        // When
        Comment result = commentSaveAdapter.save(anonymousComment);

        // Then
        verify(commentRepository).save(anonymousComment);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("익명 댓글 내용");
        assertThat(result.getUser()).isNull();
        assertThat(result.getPassword()).isEqualTo(1234);
    }

    @Test
    @DisplayName("정상 케이스 - 댓글 수정 저장")
    void shouldUpdateComment_WhenCommentModified() {
        // Given
        Comment existingComment = Comment.createComment(testPost, testUser, "원본 댓글", null);
        existingComment.updateComment("수정된 댓글");
        Comment updatedComment = Comment.createComment(testPost, testUser, "수정된 댓글", null);
        
        when(commentRepository.save(any(Comment.class))).thenReturn(updatedComment);

        // When
        Comment result = commentSaveAdapter.save(existingComment);

        // Then
        verify(commentRepository).save(existingComment);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("수정된 댓글");
    }
}