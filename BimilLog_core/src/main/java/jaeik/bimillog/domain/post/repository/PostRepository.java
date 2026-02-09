package jaeik.bimillog.domain.post.repository;

import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * <h2>PostRepository</h2>
 * <p>Post 엔티티 JPA Repository 인터페이스입니다.</p>
 * <p>PostCommandAdapter에서 게시글 저장, 삭제, 조회수 증가 작업 시 호출됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * <h3>사용자 작성 게시글 ID 조회</h3>
     * <p>사용자가 작성한 모든 게시글의 ID 목록을 반환합니다.</p>
     * <p>캐시 무효화를 위해 사용됩니다 (Redis delete는 멱등성 보장)</p>
     *
     * @param memberId 게시글을 작성한 사용자 ID
     * @return List<Long> 게시글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT p.id FROM Post p WHERE p.member.id = :memberId")
    List<Long> findIdsWithCacheFlagByMemberId(Long memberId);

    /**
     * <h3>회원 작성 게시글 일괄 삭제</h3>
     * <p>특정 사용자가 작성한 모든 게시글을 한 번에 삭제합니다.</p>
     *
     * @param memberId 게시글을 작성한 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM Post p WHERE p.member.id = :memberId")
    void deleteAllByMemberId(Long memberId);

    /**
     * <h3>조회수 증가</h3>
     * <p>게시글 ID를 통해 조회수를 1 증가시킵니다.</p>
     * <p>SELECT 없이 바로 UPDATE만 실행하여 성능을 최적화합니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("UPDATE Post p SET p.views = p.views + 1 WHERE p.id = :postId")
    void incrementViewsByPostId(Long postId);

    /**
     * <h3>조회수 일괄 증가</h3>
     * <p>지정된 양만큼 조회수를 증가시킵니다.</p>
     *
     * @param postId 게시글 ID
     * @param amount 증가량
     */
    @Modifying
    @Query("UPDATE Post p SET p.views = p.views + :amount WHERE p.id = :postId")
    void incrementViewsByAmount(Long postId, Long amount);

    /**
     * <h3>좋아요 수 원자적 증가</h3>
     */
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    /**
     * <h3>좋아요 수 원자적 감소 (음수 방지)</h3>
     */
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :postId")
    void decrementLikeCount(@Param("postId") Long postId);

    /**
     * <h3>댓글 수 원자적 증가</h3>
     */
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") Long postId);

    /**
     * <h3>댓글 수 원자적 감소 (음수 방지)</h3>
     */
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = CASE WHEN p.commentCount > 0 THEN p.commentCount - 1 ELSE 0 END WHERE p.id = :postId")
    void decrementCommentCount(@Param("postId") Long postId);

    /**
     * <h3>특정 타입의 featuredType 일괄 초기화</h3>
     * <p>스케줄러에서 WEEKLY/LEGEND 전체 교체 시 기존 값을 null로 초기화합니다.</p>
     */
    @Modifying
    @Query("UPDATE Post p SET p.featuredType = null WHERE p.featuredType = :type")
    void clearFeaturedType(@Param("type") PostCacheFlag type);

    /**
     * <h3>featuredType 설정 (null인 경우만)</h3>
     * <p>WEEKLY 스케줄러에서 사용. NOTICE/LEGEND를 덮어쓰지 않습니다.</p>
     */
    @Modifying
    @Query("UPDATE Post p SET p.featuredType = :type WHERE p.id IN :ids AND p.featuredType IS NULL")
    void setFeaturedType(@Param("ids") List<Long> ids, @Param("type") PostCacheFlag type);

    /**
     * <h3>featuredType 설정 (null 또는 특정 타입 덮어쓰기)</h3>
     * <p>LEGEND 스케줄러에서 사용. WEEKLY는 덮어쓰지만 NOTICE는 유지합니다.</p>
     */
    @Modifying
    @Query("UPDATE Post p SET p.featuredType = :newType WHERE p.id IN :ids AND (p.featuredType IS NULL OR p.featuredType = :overridable)")
    void setFeaturedTypeOverriding(@Param("ids") List<Long> ids, @Param("newType") PostCacheFlag newType, @Param("overridable") PostCacheFlag overridable);

    /**
     * <h3>좋아요 수 일괄 증감</h3>
     * <p>카운트 버퍼에서 누적된 증감량을 DB에 반영합니다.</p>
     */
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + :amount WHERE p.id = :postId")
    void incrementLikeCountByAmount(@Param("postId") Long postId, @Param("amount") Long amount);

    /**
     * <h3>댓글 수 일괄 증감</h3>
     * <p>카운트 버퍼에서 누적된 증감량을 DB에 반영합니다.</p>
     */
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + :amount WHERE p.id = :postId")
    void incrementCommentCountByAmount(@Param("postId") Long postId, @Param("amount") Long amount);
}

