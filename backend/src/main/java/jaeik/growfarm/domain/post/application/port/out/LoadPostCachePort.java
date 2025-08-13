package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;

import java.util.List;

/**
 * <h2>게시글 캐시 조회 Port</h2>
 * <p>게시글 관련 캐시 데이터를 조회하는 인터페이스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadPostCachePort {

    /**
     * <h3>캐시된 인기 게시글 조회</h3>
     * <p>지정된 유형의 캐시된 인기 게시글 목록을 조회합니다.</p>
     *
     * @param type 캐시할 게시글 유형 (예: REALTIME, WEEKLY, LEGEND, NOTICE)
     * @return 캐시된 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<SimplePostResDTO> getCachedPopularPosts(PostCacheFlag type);

    /**
     * <h3>캐시된 전체 게시글 조회</h3>
     * <p>지정된 게시글 ID의 캐시된 전체 게시글 상세 정보를 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @return 캐시된 FullPostResDTO
     * @author Jaeik
     * @since 2.0.0
     */
    FullPostResDTO getCachedFullPost(Long postId);

    /**
     * <h3>인기 게시글 캐시 존재 여부 확인</h3>
     * <p>지정된 유형의 인기 게시글 캐시가 존재하는지 확인합니다.</p>
     *
     * @param type 캐시할 게시글 유형 (예: REALTIME, WEEKLY, LEGEND, NOTICE)
     * @return 캐시 존재 여부
     * @author Jaeik
     * @since 2.0.0
     */
    boolean hasPopularPostsCache(PostCacheFlag type);
}
