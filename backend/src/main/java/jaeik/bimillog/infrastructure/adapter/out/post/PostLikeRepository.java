package jaeik.bimillog.infrastructure.adapter.out.post;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


/**
 * <h2>PostLikeRepository</h2>
 * <p>PostLike 엔티티 JPA Repository 인터페이스입니다.</p>
 * <p>PostLikeCommandAdapter와 PostLikeQueryAdapter에서 좋아요 관련 작업 시 호출됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    /**
     * <h3>사용자와 게시글로 추천 삭제</h3>
     * <p>특정 사용자의 특정 게시글에 대한 추천를 삭제합니다.</p>
     *
     * @param member 사용자 엔티티
     * @param post 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByMemberAndPost(Member member, Post post);

    /**
     * <h3>게시글 ID로 모든 추천 삭제</h3>
     * <p>특정 게시글 ID에 해당하는 모든 추천 기록을 삭제합니다.</p>
     *
     * @param postId 삭제할 게시글의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}

