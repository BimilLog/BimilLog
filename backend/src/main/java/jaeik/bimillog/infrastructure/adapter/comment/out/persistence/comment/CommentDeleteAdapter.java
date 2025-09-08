package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentDeletePort;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure.CommentClosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 삭제 어댑터</h2>
 * <p>댓글 및 댓글 클로저 엔티티의 삭제 및 익명화를 처리하는 아웃바운드 어댑터 구현체</p>
 * <p>관심사별로 분리된 삭제 전용 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentDeleteAdapter implements CommentDeletePort {

    private final CommentRepository commentRepository;
    private final CommentClosureRepository commentClosureRepository;

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
     * <p>일종의 소프트 삭제로 간주하여 삭제 관련 포트에 포함</p>
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
     * <h3>사용자 댓글 ID 목록 조회</h3>
     * <p>특정 사용자가 작성한 모든 댓글 ID 목록을 조회합니다.</p>
     * <p>삭제 로직에서 활용하기 위해 삭제 포트에 포함</p>
     *
     * @param userId 사용자 ID
     * @return List<Long> 사용자가 작성한 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Long> findCommentIdsByUserId(Long userId) {
        return commentRepository.findCommentIdsByUserId(userId);
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

    /**
     * <h3>댓글 클로저 삭제</h3>
     * <p>주어진 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentClosure 삭제할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void delete(CommentClosure commentClosure) {
        commentClosureRepository.delete(commentClosure);
    }

    /**
     * <h3>자손 ID로 댓글 클로저 삭제</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentId 자손 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteByDescendantId(Long commentId) {
        commentClosureRepository.deleteByDescendantId(commentId);
    }

    /**
     * <h3>자손 ID로 댓글 클로저 목록 조회</h3>
     * <p>주어진 자손 댓글 ID에 해당하는 모든 댓글 클로저 엔티티 목록을 조회합니다.</p>
     * <p>삭제 로직에서 활용하기 위해 삭제 포트에 포함</p>
     *
     * @param descendantId 자손 댓글 ID
     * @return Optional<List<CommentClosure>> 조회된 댓글 클로저 엔티티 목록. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<List<CommentClosure>> findByDescendantId(Long descendantId) {
        return commentClosureRepository.findByDescendantId(descendantId);
    }
}