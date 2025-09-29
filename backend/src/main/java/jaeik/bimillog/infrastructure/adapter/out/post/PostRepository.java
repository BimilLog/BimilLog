package jaeik.bimillog.infrastructure.adapter.out.post;

import jaeik.bimillog.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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
     * <h3>조회수 직접 증가 (최적화)</h3>
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
}

