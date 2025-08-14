package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;

import java.util.List;

/**
 * <h2>인기 게시글 조회 및 관리 Port</h2>
 * <p>인기 게시글 및 공지사항을 조회하고, 캐시 플래그를 적용하거나 초기화하는 인터페이스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCacheQueryPort {

    /**
     * <h3>실시간 인기 게시글 조회</h3>
     * <p>실시간 인기 게시글 목록을 조회합니다.</p>
     *
     * @return 실시간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<SimplePostResDTO> findRealtimePopularPosts();

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>주간 인기 게시글 목록을 조회합니다.</p>
     *
     * @return 주간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<SimplePostResDTO> findWeeklyPopularPosts();

    /**
     * <h3>전설의 게시글 조회</h3>
     * <p>전설의 게시글 목록을 조회합니다.</p>
     *
     * @return 전설의 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<SimplePostResDTO> findLegendaryPosts();

    /**
     * <h3>공지사항 게시글 조회</h3>
     * <p>공지사항 게시글 목록을 조회합니다.</p>
     *
     * @return 공지사항 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<SimplePostResDTO> findNoticePosts();

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>
     *     공지사항으로 지정된 게시글 목록을 조회합니다.
     * </p>
     * @return 공지사항 목록
     */
    List<SimplePostResDTO> findNoticePosts2();

    /**
     * <h3>인기 플래그 적용</h3>
     * <p>주어진 게시글 ID 목록에 특정 캐시 플래그를 적용합니다.</p>
     *
     * @param postIds       캐시 플래그를 적용할 게시글 ID 목록
     * @param postCacheFlag 적용할 캐시 플래그
     * @author Jaeik
     * @since 2.0.0
     */
    void applyPopularFlag(List<Long> postIds, PostCacheFlag postCacheFlag);

    /**
     * <h3>인기 플래그 초기화</h3>
     * <p>특정 캐시 플래그에 해당하는 모든 게시글의 플래그를 초기화합니다.</p>
     *
     * @param postCacheFlag 초기화할 캐시 플래그
     * @author Jaeik
     * @since 2.0.0
     */
    void resetPopularFlag(PostCacheFlag postCacheFlag);

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
