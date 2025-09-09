package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentSavePort;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure.CommentClosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 저장 어댑터</h2>
 * <p>댓글 및 댓글 클로저 엔티티의 저장을 처리하는 아웃바운드 어댑터 구현체</p>
 * <p>관심사별로 분리된 저장 전용 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentSaveAdapter implements CommentSavePort {

    private final CommentRepository commentRepository;
    private final CommentClosureRepository commentClosureRepository;


    /**
     * <h3>댓글 클로저 저장</h3>
     * <p>주어진 댓글 클로저 엔티티를 저장합니다.</p>
     *
     * @param commentClosure 저장할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void save(CommentClosure commentClosure) {
        commentClosureRepository.save(commentClosure);
    }

    /**
     * <h3>댓글 클로저 배치 저장</h3>
     * <p>주어진 댓글 클로저 엔티티 목록을 배치로 저장합니다.</p>
     * <p>성능 최적화를 위해 한 번의 트랜잭션으로 여러 엔티티를 저장합니다.</p>
     *
     * @param commentClosures 저장할 댓글 클로저 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void saveAll(List<CommentClosure> commentClosures) {
        commentClosureRepository.saveAll(commentClosures);
    }

    /**
     * <h3>댓글과 클로저 함께 저장</h3>
     * <p>댓글과 해당 댓글의 클로저 엔티티들을 함께 저장합니다.</p>
     * <p>부모 댓글이 있는 경우 부모 클로저 구조를 참조하여 새로운 클로저 엔티티들을 생성하고 저장합니다.</p>
     *
     * @param comment  저장할 댓글 엔티티
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void saveCommentWithClosure(Comment comment, Long parentId) {
        Comment savedComment = commentRepository.save(comment);

        List<CommentClosure> closuresToSave = new ArrayList<>();
        closuresToSave.add(CommentClosure.createCommentClosure(savedComment, savedComment, 0));

        if (parentId != null) {
            List<CommentClosure> parentClosures = getParentClosures(parentId)
                    .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다."));

            for (CommentClosure parentClosure : parentClosures) {
                closuresToSave.add(CommentClosure.createCommentClosure(
                        parentClosure.getAncestor(),
                        savedComment,
                        parentClosure.getDepth() + 1));
            }
        }
        commentClosureRepository.saveAll(closuresToSave);
    }

    /**
     * <h3>부모 댓글의 클로저 계층 조회</h3>
     * <p>댓글 저장 시 부모 댓글의 클로저 계층 구조를 조회합니다.</p>
     * <p>saveCommentWithClosure 메서드에서 대댓글 생성 시 사용</p>
     *
     * @param parentId 부모 댓글 ID
     * @return Optional<List<CommentClosure>> 부모 댓글의 클로저 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private Optional<List<CommentClosure>> getParentClosures(Long parentId) {
        return findByDescendantId(parentId);
    }

    /**
     * <h3>자손 ID로 댓글 클로저 목록 조회</h3>
     * <p>주어진 자손 댓글 ID에 해당하는 모든 댓글 클로저 엔티티 목록을 조회합니다.</p>
     * <p>댓글 저장 시 부모 클로저 계층 구조 생성에 사용</p>
     *
     * @param descendantId 자손 댓글 ID
     * @return Optional<List<CommentClosure>> 조회된 댓글 클로저 엔티티 목록. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    private Optional<List<CommentClosure>> findByDescendantId(Long descendantId) {
        return commentClosureRepository.findByDescendantId(descendantId);
    }
}