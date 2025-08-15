package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence.comment;

import jaeik.growfarm.domain.comment.application.port.out.CommentCommandPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentCommendAdapter implements CommentCommandPort {

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
     * <h3>댓글 삭제</h3>
     * <p>주어진 댓글 엔티티를 삭제합니다.</p>
     *
     * @param comment 삭제할 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void delete(Comment comment) {
        commentRepository.delete(comment);
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
}
