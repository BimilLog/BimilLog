package jaeik.growfarm.repository.post;

import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.entity.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * <h2>게시글 커스텀 저장소</h2>
 * <p>
 * 커스텀 쿼리를 정의하는 인터페이스
 * </p>
 * 
 * @author jaeik
 * @version 1.0
 */
public interface PostCustomRepository {

    void updateRealtimePopularPosts();

    List<Post> updateWeeklyPopularPosts();

    List<Post> updateHallOfFamePosts();

    Page<Post> findByLikedPosts(Long userId, Pageable pageable);

    /**
     * <h3>게시글 목록 조회</h3>
     * <p>
     * 최신순으로 페이징하여 게시글 목록을 조회한다..
     * </p>
     *
     * @param pageable 페이지 정보
     * @return 게시글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    Page<SimplePostDTO> findPostsWithCommentAndLikeCounts(Pageable pageable);

    /**
     * <h3>게시글 목록 검색</h3>
     * <p>
     * 검색어와 검색 유형에 따라 게시글을 검색한다.
     * </p>
     * <p>
     * 게시글 마다의 총 댓글 수, 총 추천 수를 반환한다.
     * </p>
     *
     * @param keyword    검색어
     * @param searchType 검색 유형 (TITLE, TITLE_CONTENT, AUTHOR 등)
     * @param pageable   페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    Page<SimplePostDTO> searchPosts(String keyword, String searchType, Pageable pageable);

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>
     * 게시글 정보, 좋아요 수, 사용자 좋아요 여부를 한 번의 쿼리로 조회한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID (null 가능)
     * @return PostDTO 게시글 상세 정보
     * @since 1.0.0
     * @author Jaeik
     */
    PostDTO findPostById(Long postId, Long userId);

}
