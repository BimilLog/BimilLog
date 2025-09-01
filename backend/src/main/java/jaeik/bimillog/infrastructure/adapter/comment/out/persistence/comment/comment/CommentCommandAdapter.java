package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentCommandPort;
import jaeik.bimillog.domain.comment.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>댓글 커맨드 어댑터</h2>
 * <p>댓글 엔티티의 저장 및 삭제 작업을 처리하는 아웃바운드 어댑터 구현체</p>
 * <p>CQRS 패턴에 따른 커맨드 전용 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentCommandAdapter implements CommentCommandPort {

    private final CommentRepository commentRepository;

    /**
     * <h3>댓글 저장</h3>
     * <p>주어진 댓글 엔티티를 저장합니다.</p>
     *
     * @param comment 저장할 댓글 엔티티
     * @return Comment 저장된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    /**
     * <h3>게시글 ID로 모든 댓글 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 댓글을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllByPostId(Long postId) {
        commentRepository.deleteAllByPostId(postId);
    }

    /**
     * <h3>사용자 댓글 익명화</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 익명화 처리합니다. (사용자 탈퇴 시 호출)</p>
     *
     * @param userId 익명화할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void anonymizeUserComments(Long userId) {
        commentRepository.anonymizeUserComments(userId);
    }

    /**
     * <h3>조건부 소프트 삭제</h3>
     * <p>자손이 있는 댓글에 대해서만 소프트 삭제를 수행합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return int 소프트 삭제된 댓글 수 (자손이 있으면 1, 없으면 0)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public int conditionalSoftDelete(Long commentId) {
        return commentRepository.conditionalSoftDelete(commentId);
    }

    /**
     * <h3>클로저 테이블에서 자손 관계 삭제</h3>
     * <p>자손이 없는 댓글의 모든 클로저 관계를 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return int 삭제된 클로저 관계 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public int deleteClosuresByDescendantId(Long commentId) {
        return commentRepository.deleteClosuresByDescendantId(commentId);
    }

    /**
     * <h3>댓글 하드 삭제</h3>
     * <p>자손이 없는 댓글을 완전히 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return int 삭제된 댓글 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public int hardDeleteComment(Long commentId) {
        return commentRepository.hardDeleteComment(commentId);
    }

}
