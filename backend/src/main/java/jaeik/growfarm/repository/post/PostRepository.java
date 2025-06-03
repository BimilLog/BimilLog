package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.post.Post;
import jakarta.validation.constraints.NotNull;
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
 * @since 1.0.0
 * @author Jaeik
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostCustomRepository {

    /**
     * <h3>전체 게시글 조회</h3>
     *
     * <p>
     * 모든 게시글을 페이지네이션으로 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param pageable 페이지 정보
     * @return 게시글 페이지
     */
    Page<Post> findAll(Pageable pageable);

    /**
     * <h3>제목으로 게시글 검색</h3>
     *
     * <p>
     * 제목에 특정 문자열이 포함된 게시글을 검색한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 페이지
     */
    Page<Post> findByTitleContaining(@NotNull String query, Pageable pageable);

    /**
     * <h3>제목 또는 내용으로 게시글 검색</h3>
     *
     * <p>
     * 제목 또는 내용에 특정 문자열이 포함된 게시글을 검색한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param titleQuery   제목 검색어
     * @param contentQuery 내용 검색어
     * @param pageable     페이지 정보
     * @return 검색된 게시글 페이지
     */
    Page<Post> findByTitleContainingOrContentContaining(@NotNull String titleQuery, @NotNull String contentQuery,
            Pageable pageable);

    /**
     * <h3>농장 이름으로 게시글 검색</h3>
     *
     * <p>
     * 작성자의 농장 이름에 특정 문자열이 포함된 게시글을 검색한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 페이지
     */
    Page<Post> findByUser_farmNameContaining(@NotNull String query, Pageable pageable);

    /**
     * <h3>사용자별 게시글 조회</h3>
     *
     * <p>
     * 특정 사용자가 작성한 게시글을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 사용자 게시글 페이지
     */
    Page<Post> findByUserId(Long userId, Pageable pageable);

    /**
     * <h3>실시간 인기글 조회</h3>
     *
     * <p>
     * 실시간 인기글로 설정된 게시글들을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 실시간 인기글 목록
     */
    List<Post> findByIsRealtimePopularTrue();

    /**
     * <h3>주간 인기글 조회</h3>
     *
     * <p>
     * 주간 인기글로 설정된 게시글들을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 주간 인기글 목록
     */
    List<Post> findByIsWeeklyPopularTrue();

    /**
     * <h3>명예의 전당 게시글 조회</h3>
     *
     * <p>
     * 명예의 전당으로 설정된 게시글들을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 명예의 전당 게시글 목록
     */
    List<Post> findByIsHallOfFameTrue();

    /**
     * <h3>실시간 인기글 초기화</h3>
     *
     * <p>
     * 모든 게시글의 실시간 인기글 상태를 false로 초기화한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     */
    @Modifying
    @Query("UPDATE Post p SET p.isRealtimePopular = false")
    void resetRealtimePopular();

    /**
     * <h3>주간 인기글 초기화</h3>
     *
     * <p>
     * 모든 게시글의 주간 인기글 상태를 false로 초기화한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     */
    @Modifying
    @Query("UPDATE Post p SET p.isWeeklyPopular = false")
    void resetWeeklyPopular();

    /**
     * <h3>명예의 전당 초기화</h3>
     *
     * <p>
     * 모든 게시글의 명예의 전당 상태를 false로 초기화한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     */
    @Modifying
    @Query("UPDATE Post p SET p.isHallOfFame = false")
    void resetHallOfFame();

    // PostCustomRepository 상속 PostCustomRepository 상속 실시간 인기글에 등록
    void updateRealtimePopularPosts();

    // PostCustomRepository 상속 주간 인기글에 등록
    List<Post> updateWeeklyPopularPosts();

    // PostCustomRepository 상속 명예의 전당에 등록
    List<Post> updateHallOfFamePosts();

    // 조회수 증가
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE post SET views = views + 1 WHERE post_id = :postId")
    void incrementViews(@Param("postId") Long postId);

    // 해당 유저가 추천 누른 글 목록 반환
    Page<Post> findByLikedPosts(Long userId, Pageable pageable);

}
