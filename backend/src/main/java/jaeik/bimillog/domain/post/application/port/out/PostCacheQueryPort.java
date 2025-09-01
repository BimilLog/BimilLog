package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * <h2>글 캐시 조회 포트</h2>
 * <p>Redis에서 글을 조회하는 역할을 한다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCacheQueryPort {

    /**
     * <h3>캐시글 목록 조회</h3>
     * <p>지정된 유형의 캐시글 목록을 조회합니다.</p>
     *
     * @param type 캐시할 게시글 유형 (예: REALTIME, WEEKLY, LEGEND, NOTICE)
     * @return 캐시된 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSearchResult> getCachedPostList(PostCacheFlag type);

    /**
     * <h3>레전드 게시글 목록 페이지네이션 조회</h3>
     * <p>레전드 게시글 목록을 페이지네이션으로 조회합니다. Redis List 구조를 활용합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 캐시된 레전드 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getCachedPostListPaged(Pageable pageable);

    /**
     * <h3>캐시글 상세 정보 조회 </h3>
     * <p>게시글 상세 정보를 캐시에서 조회하며, 캐시에 존재하지 않으면 null을 반환합니다.</p>
     * <p>null이 아닌 값을 반환하면 해당 게시글은 자동으로 인기글로 간주됩니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @return 캐시된 PostDetail (캐시에 없으면 null)
     * @author Jaeik
     * @since 2.0.0
     */
    PostDetail getCachedPostIfExists(Long postId);

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

    /**
     * <h3>특정 게시글이 인기글인지 확인</h3>
     * <p>주어진 게시글 ID가 현재 캐시되어 있는지 빠르게 확인합니다.</p>
     * <p>Redis Set 구조를 활용하여 O(1) 시간 복잡도로 확인합니다.</p>
     *
     * @param postId 확인할 게시글 ID
     * @return 인기글이면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean isPopularPost(Long postId);




}
