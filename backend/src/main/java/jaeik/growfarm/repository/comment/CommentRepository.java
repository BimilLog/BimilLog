package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
 * 댓글 Repository
 * 댓글 관련 데이터베이스 작업을 수행하는 Repository
 * 커스텀 댓글 저장소를 상속받음
 * 수정일 : 2025-05-03
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentCustomRepository {

        int countByPostId(Long postId);

        List<Comment> findByCommentList(Long postId);

        // 해당 유저의 작성 댓글 목록 반환
        Page<Comment> findByUserId(Long userId, Pageable pageable);

        // 해당 유저가 추천 누른 댓글 목록 반환
        Page<Comment> findByLikedComments(Long userId, Pageable pageable);

        @Modifying
        @Query("UPDATE Comment c SET c.popular = false")
        void resetAllCommentFeaturedFlags();

        @Query("""
                            SELECT c
                            FROM Comment c
                            WHERE (SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment = c) >= 3
                            ORDER BY c.post.id, (SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment = c) DESC
                        """)
        List<Comment> findPopularComments();

        /**
         * <h3>클로저 테이블 기반 루트 댓글 조회</h3>
         * <p>
         * 특정 게시글의 루트 댓글들을 페이징하여 조회한다.
         * </p>
         *
         * @param postId   게시글 ID
         * @param pageable 페이징 정보
         * @author Jaeik
         * @since 1.0.0
         */
        @Query("""
                        SELECT DISTINCT c FROM Comment c
                        JOIN CommentClosure cc ON c.id = cc.descendant.id
                        WHERE c.post.id = :postId
                        AND cc.depth = 0
                        AND c.deleted = false
                        ORDER BY c.createdAt ASC
                        """)
        Page<Comment> findRootCommentsByPostId(@Param("postId") Long postId, Pageable pageable);

        /**
         * <h3>특정 루트 댓글들의 모든 자손 댓글 조회</h3>
         * <p>
         * 루트 댓글 ID 리스트로 해당 댓글들의 모든 자손을 조회한다.
         * </p>
         *
         * @param rootCommentIds 루트 댓글 ID 리스트
         * @return 자손 댓글 리스트
         * @author Jaeik
         * @since 1.0.0
         */
        @Query("""
                        SELECT c FROM Comment c
                        JOIN CommentClosure cc ON c.id = cc.descendant.id
                        WHERE cc.ancestor.id IN :rootCommentIds
                        ORDER BY cc.ancestor.id, cc.depth, c.createdAt ASC
                        """)
        List<Comment> findDescendantsByRootCommentIds(@Param("rootCommentIds") List<Long> rootCommentIds);

        /**
         * <h3>댓글과 부모 관계를 한 번에 조회 (성능 최적화)</h3>
         * <p>
         * 루트 댓글들의 모든 자손과 부모 관계를 한 번의 쿼리로 조회한다.
         * </p>
         *
         * @param rootCommentIds 루트 댓글 ID 리스트
         * @return [댓글ID, 부모댓글ID, depth, 댓글엔티티] 형태의 결과
         * @author Jaeik
         * @since 1.0.0
         */
        @Query("""
                        SELECT c.id,
                               CASE WHEN cc.depth = 1 THEN cc.ancestor.id ELSE NULL END as parentId,
                               cc.depth,
                               c
                        FROM Comment c
                        JOIN CommentClosure cc ON c.id = cc.descendant.id
                        WHERE cc.ancestor.id IN :rootCommentIds
                        ORDER BY cc.ancestor.id, cc.depth, c.createdAt ASC
                        """)
        List<Object[]> findCommentsWithParentByRootIds(@Param("rootCommentIds") List<Long> rootCommentIds);

        /**
         * <h3>여러 댓글의 추천 수 배치 조회</h3>
         * <p>
         * 댓글 ID 리스트로 각 댓글의 추천 수를 한 번에 조회한다.
         * </p>
         *
         * @param commentIds 댓글 ID 리스트
         * @return 댓글 ID와 추천 수의 리스트
         * @author Jaeik
         * @since 1.0.0
         */
        @Query("""
                        SELECT c.id as commentId, COUNT(cl.id) as likeCount
                        FROM Comment c
                        LEFT JOIN CommentLike cl ON c.id = cl.comment.id
                        WHERE c.id IN :commentIds
                        GROUP BY c.id
                        """)
        List<Object[]> findLikeCountsByCommentIds(@Param("commentIds") List<Long> commentIds);

        /**
         * <h3>사용자가 추천한 댓글 배치 조회</h3>
         * <p>
         * 특정 사용자가 댓글 ID 리스트 중 추천한 댓글들을 조회한다.
         * </p>
         * 
         * @param commentIds 댓글 ID 리스트
         * @param userId     사용자 ID
         * @return 추천한 댓글 ID 리스트
         * @author Jaeik
         * @since 1.0.0
         */
        @Query("""
                        SELECT cl.comment.id
                        FROM CommentLike cl
                        WHERE cl.comment.id IN :commentIds
                        AND cl.user.id = :userId
                        """)
        List<Long> findUserLikedCommentIds(@Param("commentIds") List<Long> commentIds, @Param("userId") Long userId);
}
