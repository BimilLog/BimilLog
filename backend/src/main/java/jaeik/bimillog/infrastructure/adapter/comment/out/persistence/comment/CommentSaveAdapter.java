package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentSavePort;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure.CommentClosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}