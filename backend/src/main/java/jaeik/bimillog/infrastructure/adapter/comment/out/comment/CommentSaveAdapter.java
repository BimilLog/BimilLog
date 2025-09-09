package jaeik.bimillog.infrastructure.adapter.comment.out.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentSavePort;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentClosureRepository;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 저장 어댑터</h2>
 * <p>
 * 헥사고날 아키텍처의 Secondary Adapter로서 CommentSavePort 인터페이스를 구현합니다.
 * </p>
 * <p>
 * JPA Repository를 사용하여 댓글 및 댓글 클로저 엔티티의 저장 작업을 수행합니다.
 * 계층 구조 댓글 시스템에서 클로저 테이블 관리를 위한 전용 어댑터입니다.
 * </p>
 * <p>
 * 이 어댑터가 존재하는 이유: 댓글 저장은 단순한 엔티티 저장을 넘어서
 * 클로저 테이블의 복잡한 계층 관계 생성 로직이 필요하여 별도 어댑터로 분리하였습니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentSaveAdapter implements CommentSavePort {

    private final CommentClosureRepository commentClosureRepository;
    private final CommentRepository commentRepository;

    /**
     * <h3>댓글 저장</h3>
     * <p>주어진 댓글 엔티티를 JPA로 저장합니다.</p>
     * <p>CommentCreateUseCase가 새로운 댓글 생성 시 호출합니다.</p>
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
     * <p>주어진 댓글 클로저 엔티티를 JPA로 저장합니다.</p>
     * <p>CommentCreateUseCase가 댓글 계층 관계 생성 시 호출합니다.</p>
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
     * <p>주어진 댓글 클로저 엔티티 목록을 JPA로 배치 저장합니다.</p>
     * <p>CommentCreateUseCase가 대댓글 생성 시 여러 클로저 관계를 한 번에 생성하기 위해 호출합니다.</p>
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
     * <h3>부모 댓글의 클로저 계층 조회</h3>
     * <p>댓글 저장 시 부모 댓글의 클로저 계층 구조를 JPA로 조회합니다.</p>
     * <p>CommentCreateUseCase가 대댓글 생성 시 부모 계층 구조를 파악하기 위해 호출합니다.</p>
     *
     * @param parentId 부모 댓글 ID
     * @return Optional<List<CommentClosure>> 부모 댓글의 클로저 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<List<CommentClosure>> getParentClosures(Long parentId) {
        return findByDescendantId(parentId);
    }

    /**
     * <h3>자손 ID로 댓글 클로저 목록 조회</h3>
     * <p>주어진 자손 댓글 ID에 해당하는 모든 댓글 클로저 엔티티 목록을 JPA로 조회합니다.</p>
     * <p>댓글 저장 시 부모 클로저 계층 구조 생성에 사용됩니다.</p>
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