package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;

import java.util.List;

/**
 * <h2>게시글 캐시 관리 Port</h2>
 * <p>게시글 관련 캐시 데이터를 관리하는 인터페이스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ManagePostCachePort {

    /**
     * <h3>게시글 목록 캐시</h3>
     * <p>인기 게시글 목록을 캐시합니다.</p>
     *
     * @param type       캐시할 게시글 유형 (예: REALTIME, WEEKLY, LEGEND, NOTICE)
     * @param cachePosts 캐시할 SimplePostResDTO 목록
     * @author Jaeik
     * @since 2.0.0
     */
    void cachePosts(PostCacheFlag type, List<SimplePostResDTO> cachePosts);

    /**
     * <h3>인기 게시글 캐시 삭제</h3>
     * <p>지정된 유형의 인기 게시글 캐시를 삭제합니다.</p>
     *
     * @param type 캐시할 게시글 유형 (예: REALTIME, WEEKLY, LEGEND, NOTICE)
     * @author Jaeik
     * @since 2.0.0
     */
    void deletePopularPostsCache(PostCacheFlag type);

    /**
     * <h3>전체 게시글 캐시</h3>
     * <p>FullPostResDTO 형태의 게시글 상세 정보를 캐시합니다.</p>
     *
     * @param post 캐시할 FullPostResDTO
     * @author Jaeik
     * @since 2.0.0
     */
    void cacheFullPost(FullPostResDTO post);

    /**
     * <h3>전체 게시글 캐시 삭제</h3>
     * <p>지정된 게시글 ID의 전체 게시글 캐시를 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteFullPostCache(Long postId);
}
