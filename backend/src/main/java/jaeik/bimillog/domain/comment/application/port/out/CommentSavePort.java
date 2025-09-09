package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 저장 어댑터</h2>
 * <p>댓글 및 댓글 클로저 엔티티의 생성/저장을 위한 아웃바운드 어댑터</p>
 * <p>관심사 기반으로 분리된 저장 전용 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentSavePort {

    /**
     * <h3>댓글 저장</h3>
     * <p>주어진 댓글 엔티티를 저장합니다.</p>
     *
     * @param comment 저장할 댓글 엔티티
     * @return Comment 저장된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Comment save(Comment comment);

    /**
     * <h3>댓글 클로저 저장</h3>
     * <p>주어진 댓글 클로저 엔티티를 저장합니다.</p>
     *
     * @param commentClosure 저장할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void save(CommentClosure commentClosure);

    /**
     * <h3>댓글 클로저 배치 저장</h3>
     * <p>주어진 댓글 클로저 엔티티 목록을 배치로 저장합니다.</p>
     * <p>성능 최적화를 위해 한 번의 트랜잭션으로 여러 엔티티를 저장합니다.</p>
     *
     * @param commentClosures 저장할 댓글 클로저 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    void saveAll(List<CommentClosure> commentClosures);

    /**
     * <h3>부모 댓글의 모든 조상 댓글 클로저 조회</h3>
     * <p>주어진 부모 댓글 ID에 해당하는 모든 조상 댓글 클로저 엔티티를 조회합니다.</p>
     * <p>부모 댓글이 null이거나 존재하지 않으면 빈 Optional을 반환합니다.</p>
     *
     * @param parentId 부모 댓글 ID
     * @return Optional<List<CommentClosure>> 조회된 조상 댓글 클로저 목록. 부모가 null이거나 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<List<CommentClosure>> getParentClosures(Long parentId);
}