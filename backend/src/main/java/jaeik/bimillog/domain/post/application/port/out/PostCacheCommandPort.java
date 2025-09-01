package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;

import java.util.List;

/**
 * <h2>게시글 캐시 관리 포트</h2>
 * <p>게시글 관련 캐시 데이터를 관리하는 인터페이스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCacheCommandPort {

    /**
     * <h3>게시글 캐시</h3>
     * <p>게시글 목록과 각 게시글의 상세 정보를 함께 캐시합니다.</p>
     * <p>PostDetail에서 PostSearchResult를 추출하여 목록 캐시를 생성하고,</p>
     * <p>각 PostDetail을 개별 상세 캐시로 저장합니다.</p>
     *
     * @param type      캐시할 게시글 유형 (예: REALTIME, WEEKLY, LEGEND, NOTICE)
     * @param fullPosts 캐시할 게시글 상세 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    void cachePostsWithDetails(PostCacheFlag type, List<PostDetail> fullPosts);

    /**
     * <h3>캐시 삭제</h3>
     * <p>캐시를 삭제합니다. type이 null이면 특정 게시글의 모든 캐시를 삭제하고,</p>
     * <p>type이 지정되면 해당 타입의 목록 캐시와 관련 상세 캐시를 삭제합니다.</p>
     *
     * @param type   캐시할 게시글 유형 (null이면 특정 게시글 삭제 모드)
     * @param postId 게시글 ID (type이 null일 때만 사용)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteCache(PostCacheFlag type, Long postId);

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

}
