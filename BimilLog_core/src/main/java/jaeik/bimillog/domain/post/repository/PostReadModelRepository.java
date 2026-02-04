package jaeik.bimillog.domain.post.repository;

import jaeik.bimillog.domain.post.entity.jpa.PostReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <h2>Post Read Model Repository</h2>
 * <p>CQRS 조회 전용 모델의 CRUD 및 원자적 업데이트를 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
public interface PostReadModelRepository extends JpaRepository<PostReadModel, Long> {

    /**
     * 좋아요 수 원자적 증가
     */
    @Modifying
    @Query("UPDATE PostReadModel p SET p.likeCount = p.likeCount + 1 WHERE p.postId = :postId")
    int incrementLikeCount(@Param("postId") Long postId);

    /**
     * 좋아요 수 원자적 감소 (음수 방지)
     */
    @Modifying
    @Query("UPDATE PostReadModel p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.postId = :postId")
    int decrementLikeCount(@Param("postId") Long postId);

    /**
     * 댓글 수 원자적 증가
     */
    @Modifying
    @Query("UPDATE PostReadModel p SET p.commentCount = p.commentCount + 1 WHERE p.postId = :postId")
    int incrementCommentCount(@Param("postId") Long postId);

    /**
     * 댓글 수 원자적 감소 (음수 방지)
     */
    @Modifying
    @Query("UPDATE PostReadModel p SET p.commentCount = CASE WHEN p.commentCount > 0 THEN p.commentCount - 1 ELSE 0 END WHERE p.postId = :postId")
    int decrementCommentCount(@Param("postId") Long postId);

    /**
     * 조회수 원자적 증가
     */
    @Modifying
    @Query("UPDATE PostReadModel p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * 조회수 지정 양만큼 원자적 증가 (벌크 업데이트용)
     */
    @Modifying
    @Query("UPDATE PostReadModel p SET p.viewCount = p.viewCount + :amount WHERE p.postId = :postId")
    int incrementViewCountByAmount(@Param("postId") Long postId, @Param("amount") Long amount);

    /**
     * 제목 업데이트
     */
    @Modifying
    @Query("UPDATE PostReadModel p SET p.title = :title WHERE p.postId = :postId")
    int updateTitle(@Param("postId") Long postId, @Param("title") String title);
}
