package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
     * <h3>사용자별 게시글 조회</h3>
     * <p>
     * 특정 사용자가 작성한 게시글을 조회한다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 사용자 게시글 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    Page<Post> findByUserId(Long userId, Pageable pageable);


    // PostCustomRepository 상속 PostCustomRepository 상속 실시간 인기글에 등록
    void updateRealtimePopularPosts();

    // PostCustomRepository 상속 주간 인기글에 등록
    List<Post> updateWeeklyPopularPosts();

    // PostCustomRepository 상속 명예의 전당에 등록
    List<Post> updateHallOfFamePosts();

    // 해당 유저가 추천 누른 글 목록 반환
    Page<Post> findByLikedPosts(Long userId, Pageable pageable);

}
