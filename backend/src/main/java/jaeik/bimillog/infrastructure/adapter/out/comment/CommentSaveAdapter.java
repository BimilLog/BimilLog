package jaeik.bimillog.infrastructure.adapter.out.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentSavePort;
import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentClosureRepository;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 저장 어댑터</h2>
 * <p>댓글 저장 포트의 구현체입니다.</p>
 * <p>댓글 엔티티 저장, 클로저 엔티티 저장, 배치 저장</p>
 * <p>계층 구조 조회, 부모 댓글의 조상 관계 조회</p>
 * <p>계층형 댓글 시스템의 클로저 테이블 관리</p>
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
     * <h3>댓글 엔티티 저장</h3>
     * <p>새로운 댓글 엔티티를 데이터베이스에 저장하고 자동 생성된 ID를 포함한 엔티티를 반환합니다.</p>
     * <p>{@link CommentCommandService}에서 새 댓글 작성 시 호출됩니다.</p>
     *
     * @param comment 저장할 댓글 엔티티
     * @return Comment 저장된 댓글 엔티티 (생성된 ID 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }


    /**
     * <h3>단일 댓글 클로저 관계 저장</h3>
     * <p>댓글 간의 조상-후손 관계를 나타내는 클로저 엔티티를 저장합니다.</p>
     * <p>{@link CommentCommandService}에서 개별 클로저 관계 저장 시 호출됩니다.</p>
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
     * <h3>다중 댓글 클로저 관계 일괄 저장</h3>
     * <p>여러 개의 댓글 클로저 엔티티를 한 번의 배치 작업으로 저장합니다.</p>
     * <p>{@link CommentCommandService}에서 대댓글 생성 시 다중 클로저 관계를 일괄 생성할 때 호출됩니다.</p>
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
     * <h3>부모 댓글의 조상 클로저 관계 조회</h3>
     * <p>특정 부모 댓글의 모든 조상 댓글과의 클로저 관계를 조회합니다.</p>
     * <p>부모 댓글이 존재하지 않거나 null인 경우 빈 Optional을 반환하여 최상위 댓글임을 나타냅니다.</p>
     * <p>{@link CommentCommandService}에서 대댓글 생성 시 클로저 관계 구성을 위해 호출됩니다.</p>
     *
     * @param parentId 부모 댓글 ID
     * @return Optional<List<CommentClosure>> 조상 댓글 클로저 목록. 최상위 댓글이면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<List<CommentClosure>> getParentClosures(Long parentId) {
        return commentClosureRepository.findByDescendantId(parentId);
    }
}