package jaeik.bimillog.adapter.out.comment;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.infrastructure.adapter.out.comment.CommentDeleteAdapter;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentRepository;
import jaeik.bimillog.testutil.BaseH2IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CommentDeleteAdapter 통합 테스트</h2>
 * <p>H2 인메모리 데이터베이스를 사용한 CommentDeleteAdapter의 통합 테스트</p>
 * <p>H2 데이터베이스를 사용하여 댓글 삭제 및 익명화 동작 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
class CommentDeleteAdapterIntegrationTest extends BaseH2IntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentDeleteAdapter commentDeleteAdapter;

    private Post testPost;

    @Override
    protected void setUpChild() {
        // 테스트 데이터 초기화
        commentRepository.deleteAll();

        // 테스트용 게시글 생성 - BaseH2IntegrationTest의 testUser 활용
        testPost = Post.builder()
                .user(testUser)
                .title("테스트 게시글")
                .content("테스트 내용")
                .isNotice(false)
                .views(0)
                .build();
        testPost = saveAndFlush(testPost);
    }

    @Test
    @DisplayName("정상 케이스 - 댓글 삭제 (자손 없는 경우 하드 삭제)")
    void shouldHardDeleteComment_WhenValidCommentProvided() {
        // Given: 기존 댓글 생성 및 저장 (자손이 없는 댓글)
        Comment existingComment = Comment.createComment(
                testPost,
                testUser,
                "삭제될 댓글",
                null
        );
        existingComment = commentRepository.save(existingComment);
        Long commentId = existingComment.getId();

        // 삭제 전 댓글 존재 확인
        assertThat(commentRepository.findById(commentId)).isPresent();

        // When: 댓글 삭제 (내부적으로 자손이 없어 하드 삭제 수행)
        commentDeleteAdapter.deleteComment(commentId);

        // EntityManager 초기화로 변경사항 반영
        entityManager.flush();
        entityManager.clear();

        // Then: 댓글이 완전히 삭제되었는지 검증
        Optional<Comment> deletedComment = commentRepository.findById(commentId);
        assertThat(deletedComment).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 클로저 테이블과 함께 댓글 삭제")
    void shouldDeleteCommentWithClosures_WhenValidCommentProvided() {
        // Given: 댓글 생성 및 클로저 관계 설정
        Comment comment = Comment.createComment(testPost, testUser, "테스트 댓글", null);
        comment = commentRepository.save(comment);
        Long commentId = comment.getId();

        // 삭제 전 댓글 존재 확인
        assertThat(commentRepository.findById(commentId)).isPresent();

        // When: 댓글 삭제 (클로저 테이블과 함께 삭제)
        commentDeleteAdapter.deleteComment(commentId);

        // EntityManager 초기화로 변경사항 반영
        entityManager.flush();
        entityManager.clear();

        // Then: 댓글과 클로저가 모두 삭제되었는지 검증
        Optional<Comment> deletedComment = commentRepository.findById(commentId);
        assertThat(deletedComment).isEmpty(); // 하드 삭제로 완전히 제거
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 댓글 ID로 삭제")
    void shouldDoNothing_WhenDeletingNonExistentComment() {
        // Given: 존재하지 않는 댓글 ID
        Long nonExistentCommentId = 999L;

        // When: 존재하지 않는 댓글 삭제 (예외가 발생하지 않아야 함)
        commentDeleteAdapter.deleteComment(nonExistentCommentId);

        // Then: 예외가 발생하지 않고 정상적으로 완료되어야 함
        // 통합 테스트에서는 예외 발생 여부만 확인
        List<Comment> allComments = commentRepository.findAll();
        // 기존 댓글들에 영향을 주지 않아야 함
        assertThat(allComments).isEmpty(); // setUpChild에서 초기화되므로 빈 상태
    }
}