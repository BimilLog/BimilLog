package jaeik.bimillog.domain.comment.repository;

import jaeik.bimillog.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>댓글 레포지토리 인터페이스</h2>
 * <p>
 * 댓글(`Comment`) 엔티티의 데이터베이스 작업을 처리하는 레포지토리 인터페이스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * <h3>클로저 테이블에서 댓글 관련 모든 관계 삭제</h3>
     * <p>댓글과 관련된 모든 클로저 관계를 삭제합니다.</p>
     * <p>descendant_id와 ancestor_id 양방향으로 참조하는 레코드를 모두 삭제하여 FK 제약 조건 위반을 방지합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = "DELETE FROM comment_closure WHERE descendant_id = :commentId OR ancestor_id = :commentId", nativeQuery = true)
    void deleteClosuresByDescendantId(@Param("commentId") Long commentId);

    /**
     * <h3>댓글 하드 삭제</h3>
     * <p>자손이 없는 댓글을 완전히 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = "DELETE FROM comment WHERE comment_id = :commentId", nativeQuery = true)
    void hardDeleteComment(@Param("commentId") Long commentId);

    /**
     * <h3>댓글 ID 목록으로 댓글 리스트 조회 (회원 정보 즉시 로딩)</h3>
     * <p>주어진 ID 목록에 해당하는 댓글들을 조회하며, 댓글 작성자(member) 정보를 즉시 로딩(fetch join)합니다.</p>
     *
     * @param commentIds 조회할 댓글 ID 목록
     * @return List<Comment> 조회된 댓글 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.member WHERE c.id IN :commentIds")
    List<Comment> findAllByIdsWithMember(@Param("commentIds") List<Long> commentIds);

    /**
     * <h3>특정 글의 모든 댓글 조회</h3>
     * <p>특정 게시글의 모든 댓글을 조회합니다.</p>
     * <p>계층 구조와 무관하게 플랫한 리스트로 반환하며, 삭제 표시된 댓글도 포함합니다.</p>
     *
     * @param postId 댓글을 조회할 게시글 ID
     * @return List<Comment> 해당 게시글의 모든 댓글 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<Comment> findByPost_Id(Long postId);

    /**
     * <h3>특정 사용자의 모든 댓글 조회</h3>
     * <p>사용자 탈퇴 시 댓글 처리를 위해 특정 사용자의 모든 댓글 엔티티를 조회합니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return List<Comment> 사용자가 작성한 모든 댓글 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Comment> findByMember_Id(Long memberId);
}

