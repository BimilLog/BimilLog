package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.comment;

import jaeik.growfarm.domain.comment.entity.Comment;
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
     * <h3>사용자가 추천한 댓글 ID 목록 조회</h3>
     * <p>주어진 댓글 ID 목록 중 사용자가 추천를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @param userId     사용자 ID
     * @return List<Long> 사용자가 추천를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.comment.id IN :commentIds AND cl.user.id = :userId")
    List<Long> findUserLikedCommentIds(@Param("commentIds") List<Long> commentIds, @Param("userId") Long userId);


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
     * <h3>최적화된 댓글 삭제</h3>
     * <p>자손 존재 여부에 따라 자동으로 소프트/하드 삭제를 수행합니다.</p>
     * <p>단일 트랜잭션 내에서 조건부 삭제를 수행하여 성능을 최적화합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return boolean true면 하드 삭제 수행됨, false면 소프트 삭제 수행됨
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = """
        UPDATE comment c 
        SET c.deleted = CASE 
                WHEN EXISTS(
                    SELECT 1 FROM comment_closure cc 
                    WHERE cc.ancestor_id = :commentId AND cc.depth > 0
                ) THEN true
                ELSE c.deleted 
            END,
            c.content = CASE 
                WHEN EXISTS(
                    SELECT 1 FROM comment_closure cc 
                    WHERE cc.ancestor_id = :commentId AND cc.depth > 0
                ) THEN '삭제된 댓글 입니다.'
                ELSE c.content 
            END,
            c.modified_at = NOW()
        WHERE c.comment_id = :commentId
        """, nativeQuery = true)
    int conditionalSoftDelete(@Param("commentId") Long commentId);

    /**
     * <h3>클로저 테이블에서 자손 관계 삭제</h3>
     * <p>자손이 없는 댓글의 모든 클로저 관계를 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return int 삭제된 클로저 관계 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = "DELETE FROM comment_closure WHERE descendant_id = :commentId", nativeQuery = true)
    int deleteClosuresByDescendantId(@Param("commentId") Long commentId);

    /**
     * <h3>댓글 하드 삭제</h3>
     * <p>자손이 없는 댓글을 완전히 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return int 삭제된 댓글 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = "DELETE FROM comment WHERE comment_id = :commentId", nativeQuery = true)
    int hardDeleteComment(@Param("commentId") Long commentId);

    /**
     * <h3>최적화된 댓글 삭제 통합 메서드</h3>
     * <p>자손 존재 여부를 확인하고 적절한 삭제 방식을 선택합니다.</p>
     * <p>자손이 있으면 소프트 삭제, 없으면 하드 삭제를 수행합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return boolean true면 하드 삭제 수행됨, false면 소프트 삭제 수행됨
     * @author Jaeik
     * @since 2.0.0
     */
    default boolean deleteCommentOptimized(Long commentId) {
        // 1단계: 조건부 소프트 삭제 시도 (자손이 있는 경우만 실행됨)
        int softDeleteCount = conditionalSoftDelete(commentId);
        
        if (softDeleteCount > 0) {
            // 소프트 삭제가 수행됨 (자손이 있음)
            return false;
        } else {
            // 자손이 없으므로 하드 삭제 수행
            deleteClosuresByDescendantId(commentId);
            hardDeleteComment(commentId);
            return true;
        }
    }
}





