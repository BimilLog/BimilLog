package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * <h3>게시글 Repository</h3>
 * <p>
 * 게시글 관련 데이터베이스 작업을 수행하는 Repository
 * </p>
 * <p>
 * 커스텀 게시글 저장소를 상속받음
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostCustomRepository {

    /**
     * <h3>게시글 조회수 증가</h3>
     * <p>
     * 게시글의 조회수를 1 증가시킨다.
     * </p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 1.0.0
     */
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE post SET views = views + 1 WHERE post_id = :postId")
    void incrementViews(@Param("postId") Long postId);

    /**
     * <h3>글 ID로 사용자 ID 조회</h3>
     * <p>
     * 글 ID로 해당 댓글을 작성한 사용자의 ID를 조회합니다.
     * </p>
     *
     * @param postId 글 ID
     * @return 사용자 ID
     * @author Jaeik
     * @since 1.0.0
     */
    @Query(nativeQuery = true, value = "SELECT user_id FROM post WHERE post_id = :postId")
    Long findUserIdByPostId(@Param("postId") Long postId);

}
