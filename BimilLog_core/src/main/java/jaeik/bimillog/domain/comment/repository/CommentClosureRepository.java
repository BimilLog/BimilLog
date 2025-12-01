package jaeik.bimillog.domain.comment.repository;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.domain.comment.service.CommentCommandService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 클로저 레포지토리 인터페이스</h2>
 * <p>
 * 댓글의 계층 구조를 관리하는 클로저 엔티티(`CommentClosure`)의 데이터베이스 작업을 처리하는 레포지토리 인터페이스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface CommentClosureRepository extends JpaRepository<CommentClosure, Long> {

    /**
     * <h3>자손 ID로 댓글 클로저 목록 조회</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티 목록을 조회합니다.</p>
     *
     * @param descendantId 자손 댓글 ID
     * @return Optional<List<CommentClosure>> 조회된 댓글 클로저 엔티티 목록. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<List<CommentClosure>> findByDescendantId(Long descendantId);

    /**
     * <h3>자손 댓글 존재 여부 확인</h3>
     * <p>특정 댓글이 자손 댓글을 가지고 있는지 확인합니다.</p>
     * <p>클로저 테이블에서 depth > 0이고 ancestor가 해당 댓글인 경우가 있는지 확인합니다.</p>
     * <p>{@link CommentCommandService}에서 댓글 삭제 시 하드/소프트 삭제 결정을 위해 호출됩니다.</p>
     *
     * @param commentId 확인할 댓글 ID
     * @return boolean 자손 댓글이 있으면 true, 없으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByAncestor_IdAndDepthGreaterThan(Long commentId);


}













