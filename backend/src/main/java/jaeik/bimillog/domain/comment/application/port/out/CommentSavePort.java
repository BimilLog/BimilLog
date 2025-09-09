package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;

import java.util.List;

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
     * <h3>댓글과 클로저 함께 저장</h3>
     * <p>댓글과 해당 댓글의 클로저 엔티티들을 함께 저장합니다.</p>
     * <p>부모 댓글이 있는 경우 부모 클로저 구조를 참조하여 새로운 클로저 엔티티들을 생성하고 저장합니다.</p>
     *
     * @param comment  저장할 댓글 엔티티
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    void saveCommentWithClosure(Comment comment, Long parentId);


}