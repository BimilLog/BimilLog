package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


/**
 * <h2>게시글 추천 JPA Repository</h2>
 * <p>게시글 추천 엔티티에 대한 데이터 접근을 처리하는 Spring Data JPA Repository 인터페이스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostLikeJpaRepository extends JpaRepository<PostLike, Long> {
    /**
     * <h3>사용자와 게시글로 추천 존재 여부 확인</h3>
     * <p>특정 사용자가 특정 게시글에 추천를 눌렀는지 여부를 확인합니다.</p>
     *
     * @param user 사용자 엔티티
     * @param post 게시글 엔티티
     * @return 추천가 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByUserAndPost(User user, Post post);
    /**
     * <h3>사용자와 게시글로 추천 삭제</h3>
     * <p>특정 사용자의 특정 게시글에 대한 추천를 삭제합니다.</p>
     *
     * @param user 사용자 엔티티
     * @param post 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByUserAndPost(User user, Post post);
    /**
     * <h3>게시글 추천 수 조회</h3>
     * <p>특정 게시글의 추천 총 개수를 조회합니다.</p>
     *
     * @param post 게시글 엔티티
     * @return 해당 게시글의 추천 개수
     * @author Jaeik
     * @since 2.0.0
     */
    long countByPost(Post post);

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

