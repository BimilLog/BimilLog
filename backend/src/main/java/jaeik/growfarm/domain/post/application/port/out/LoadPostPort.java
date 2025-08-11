package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.domain.Post;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;

/**
 * <h2>LoadPostPort</h2>
 * <p>
 *     게시글 데이터를 조회하는 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 1.0
 */
public interface LoadPostPort {

    /**
     * <h3>ID로 게시글 조회</h3>
     * <p>
     *     게시글 ID를 이용하여 특정 게시글을 조회합니다.
     * </p>
     * @param id 조회할 게시글 ID
     * @return Optional<Post>
     */
    Optional<Post> findById(Long id);

    /**
     * <h3>게시판 목록 조회</h3>
     * <p>
     *     최신순으로 게시글 목록을 페이지네이션하여 조회합니다.
     * </p>
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지
     */
    Page<SimplePostResDTO> findByPage(Pageable pageable);

    /**
     * <h3>게시글 검색</h3>
     * <p>
     *     검색 유형과 검색어를 통해 게시글을 검색하고 최신순으로 페이지네이션합니다.
     * </p>
     * @param type 검색 유형
     * @param query 검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 목록 페이지
     */
    Page<SimplePostResDTO> findBySearch(String type, String query, Pageable pageable);

    Page<SimplePostResDTO> findPostsByUserId(Long userId, Pageable pageable);

    Page<SimplePostResDTO> findLikedPostsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>
     *     공지사항으로 지정된 게시글 목록을 조회합니다.
     * </p>
     * @return 공지사항 목록
     */
    List<SimplePostResDTO> findNoticePosts();

    /**
     * <h3>조회수 증가</h3>
     * <p>
     *     게시글의 조회수를 1 증가시킵니다.
     * </p>
     * @param post 조회수를 증가시킬 게시글
     */
    void incrementView(Post post);
}
