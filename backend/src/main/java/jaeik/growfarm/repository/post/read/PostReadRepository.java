package jaeik.growfarm.repository.post.read;

import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * <h2>게시글 조회 저장소</h2>
 * <p>
 * 기본적인 게시글 목록/상세 조회 기능을 담당한다.
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 * @since 1.1.0
 */
@Repository
public interface PostReadRepository {

    /**
     * <h3>게시글 목록 조회</h3>
     * <p>
     * 최신순으로 페이징하여 게시글 목록을 조회한다.
     * </p>
     *
     * @param pageable 페이지 정보
     * @return 게시글 목록
     * @author Jaeik
     * @since 1.1.0
     */
    Page<SimplePostResDTO> findSimplePost(Pageable pageable);

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>
     * 게시글 정보, 좋아요 수, 사용자 좋아요 여부를 조회한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID (null 가능)
     * @return PostDTO 게시글 상세 정보
     * @since 1.1.0
     * @author Jaeik
     */
    FullPostResDTO findPostById(Long postId, Long userId);
}


