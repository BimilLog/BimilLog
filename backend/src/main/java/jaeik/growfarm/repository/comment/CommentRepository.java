package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.board.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentCustomRepository {

    int countByPostId(Long postId);

    List<Comment> findByCommentList(Long postId);

    // 해당 유저의 작성 댓글 목록 반환
    Page<Comment> findByUserId(Long userId, Pageable pageable);

    // 해당 유저가 추천 누른 댓글 목록 반환
    Page<Comment> findByLikedComments(Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM comment_like WHERE comment_id IN (SELECT comment_id FROM comment WHERE user_id = :userId)")
    void deleteCommentLikesByCommentUserIds(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM comment_like WHERE user_id = :userId")
    void deleteCommentLikesByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM comment WHERE post_id IN (SELECT post_id FROM post WHERE user_id = :userId)")
    void deleteCommentsByPostUserIds(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM comment WHERE user_id = :userId")
    void deleteCommentsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Comment c SET c.isFeatured = false")
    void resetAllCommentFeaturedFlags();

    @Query("""
    SELECT c
    FROM Comment c
    WHERE (SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment = c) >= 3
    ORDER BY c.post.id, (SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment = c) DESC
""")
    List<Comment> findPopularComments();

}
