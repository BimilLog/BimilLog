package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.board.Post;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
 * 게시글 Repository
 * 게시글 관련 데이터베이스 작업을 수행하는 Repository
 * 커스텀 게시글 저장소를 상속 받음
 * 수정일 : 2025-05-03
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostCustomRepository {
    // 전체 검색
    Page<Post> findAll(Pageable pageable);
    // 제목 검색
    Page<Post> findByTitleContaining(@NotNull String query, Pageable pageable);
    // 제목 내용 검색
    Page<Post> findByTitleContainingOrContentContaining(@NotNull String titleQuery, @NotNull String contentQuery, Pageable pageable);
    // 작성 농장 검색
    Page<Post> findByUser_farmNameContaining(@NotNull String query, Pageable pageable);
    // PostCustomRepository 상속 실시간 인기글에 등록
    void updateRealtimePopularPosts();
    // PostCustomRepository 상속 주간 인기글에 등록
    List<Post> updateWeeklyPopularPosts();
    // PostCustomRepository 상속 명예의 전당에 등록
    List<Post> updateHallOfFamePosts();
    // 실시간 인기글 검색
    List<Post> findByIsRealtimePopularTrue();
    // 주간 인기글 검색
    List<Post> findByIsWeeklyPopularTrue();
    // 명예의 전당 검색
    List<Post> findByIsHallOfFameTrue();

    // 조회수 증가
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE post SET views = views + 1 WHERE post_id = :postId")
    void incrementViews(@Param("postId") Long postId);

    // 해당 유저의 작성 글 목록 반환
    Page<Post> findByUserId(Long userId, Pageable pageable);

    // 해당 유저가 추천 누른 글 목록 반환
    Page<Post> findByLikedPosts(Long userId, Pageable pageable);

}
