package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment;

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
     * <h3>사용자 댓글 익명화</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 익명화 처리합니다. (사용자 탈퇴 시 호출)</p>
     *
     * @param userId 익명화할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("UPDATE Comment c SET c.user = null, c.content = '탈퇴한 사용자의 댓글입니다.' WHERE c.user.id = :userId")
    void anonymizeUserComments(@Param("userId") Long userId);

    /**
     * <h3>게시글 ID로 모든 댓글 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 댓글을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);

    /**
     * <h3>조건부 소프트 삭제</h3>
     * <p>자손이 있는 댓글에 대해서만 소프트 삭제를 수행합니다.</p>
     * <p>자손이 없는 댓글은 업데이트하지 않아 반환값이 0이 됩니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return int 소프트 삭제된 댓글 수 (자손이 있으면 1, 없으면 0)
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = """
        UPDATE comment c 
        SET c.deleted = true,
            c.content = '삭제된 댓글입니다.',
            c.modified_at = NOW()
        WHERE c.comment_id = :commentId 
          AND :commentId IN (
              SELECT cc.ancestor_id FROM comment_closure cc 
              WHERE cc.depth > 0
          )
        """, nativeQuery = true)
    int conditionalSoftDelete(@Param("commentId") Long commentId);

    /**
     * <h3>클로저 테이블에서 자손 관계 삭제</h3>
     * <p>자손이 없는 댓글의 모든 클로저 관계를 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = "DELETE FROM comment_closure WHERE descendant_id = :commentId", nativeQuery = true)
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
     * <h3>사용자 댓글 ID 목록 조회</h3>
     * <p>특정 사용자가 작성한 모든 댓글 ID 목록을 조회합니다.</p>
     *
     * @param userId 사용자 ID
     * @return List<Long> 사용자가 작성한 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT c.id FROM Comment c WHERE c.user.id = :userId AND c.deleted = false")
    List<Long> findCommentIdsByUserId(@Param("userId") Long userId);

    /**
     * <h3>사용자 탈퇴 시 자손이 있는 댓글들 소프트 삭제</h3>
     * <p>특정 사용자가 작성한 댓글 중 자손이 있는 댓글들을 한 번에 소프트 삭제합니다.</p>
     *
     * @param userId 탈퇴하는 사용자 ID
     * @return int 소프트 삭제된 댓글 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = """
        UPDATE comment c 
        SET c.deleted = true,
            c.content = '삭제된 댓글입니다.',
            c.modified_at = NOW()
        WHERE c.user_id = :userId 
          AND c.comment_id IN (
              SELECT cc.ancestor_id FROM comment_closure cc 
              WHERE cc.depth > 0
          )
        """, nativeQuery = true)
    int batchSoftDeleteUserCommentsWithDescendants(@Param("userId") Long userId);

    /**
     * <h3>사용자 탈퇴 시 자손이 없는 댓글들 하드 삭제</h3>
     * <p>특정 사용자가 작성한 댓글 중 자손이 없는 댓글들을 한 번에 하드 삭제합니다.</p>
     * <p>클로저 테이블 정리도 함께 수행됩니다.</p>
     *
     * @param userId 탈퇴하는 사용자 ID
     * @return int 하드 삭제된 댓글 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = """
        DELETE c, cc FROM comment c
        LEFT JOIN comment_closure cc ON c.comment_id = cc.descendant_id
        WHERE c.user_id = :userId 
          AND c.comment_id NOT IN (
              SELECT ancestor_id FROM comment_closure 
              WHERE depth > 0
          )
        """, nativeQuery = true)
    int batchHardDeleteUserCommentsWithoutDescendants(@Param("userId") Long userId);
}

