package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure;

import jaeik.bimillog.domain.comment.application.port.out.CommentClosureCommandPort;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>댓글 클로저 명령 어댑터</h2>
 * <p>댓글 클로저 엔티티의 생성, 수정, 삭제 작업을 처리하는 아웃바운드 어댑터 구현체</p>
 * <p>CQRS 패턴에 따른 명령 전용 어댑터</p>
 * <p>계층형 댓글 구조를 관리하기 위한 Closure Table 패턴 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentClosureCommandAdapter implements CommentClosureCommandPort {

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
