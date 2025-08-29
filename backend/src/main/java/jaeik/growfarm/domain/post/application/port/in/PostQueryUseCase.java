package jaeik.growfarm.domain.post.application.port.in;

import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.PostDetail;
import jaeik.growfarm.domain.post.entity.PostSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>PostQueryUseCase</h2>
 * <p>
 *     게시글 조회와 관련된 비즈니스 로직을 처리하는 UseCase 인터페이스입니다.
 * </p>
 * @author jaeik
 * @version 2.0.0
 */
public interface PostQueryUseCase {

    /**
     * <h3>게시판 조회</h3>
     * <p>
     *     최신순으로 게시글 목록을 페이지네이션으로 조회합니다.
     * </p>
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지
     * @since 2.0.0
     * @author jaeik
     */
    Page<PostSearchResult> getBoard(Pageable pageable);

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>
     *     게시글 ID를 통해 게시글 상세 정보를 조회합니다.
     * </p>
     * @param postId      게시글 ID
     * @param userId     현재 로그인한 사용자 ID
     * @return 게시글 상세 정보 DTO
     * @since 2.0.0
     * @author jaeik
     */
    PostDetail getPost(Long postId, Long userId);

    /**
     * <h3>게시글 검색</h3>
     * <p>
     *     검색 유형과 검색어를 통해 게시글을 검색하고 최신순으로 페이지네이션합니다.
     * </p>
     * @param type  검색 유형
     * @param query 검색어
     * @param pageable  페이지 정보
     * @return 검색된 게시글 목록 페이지
     * @since 2.0.0
     * @author jaeik
     */
    Page<PostSearchResult> searchPost(String type, String query, Pageable pageable);

    /**
     * <h3>인기 게시글 목록 조회</h3>
     * <p>
     *     캐시된 인기 게시글 목록(실시간, 주간, 레전드)을 조회합니다.
     * </p>
     * @param type 조회할 인기 게시글 유형
     * @return 인기 게시글 목록
     */
    List<PostSearchResult> getPopularPosts(PostCacheFlag type);

    /**
     * <h3>실시간/주간 인기 게시글 목록 일괄 조회</h3>
     * <p>
     *     실시간과 주간 인기 게시글을 한 번에 조회합니다. 
     *     성능 최적화를 위해 한 번의 호출로 두 타입의 데이터를 가져옵니다.
     * </p>
     * @return 실시간/주간 인기 게시글 맵 (key: "realtime"/"weekly", value: 게시글 목록)
     * @since 2.0.0
     * @author jaeik
     */
    Map<String, List<PostSearchResult>> getRealtimeAndWeeklyPosts();

    /**
     * <h3>레전드 인기 게시글 목록 조회 (페이징)</h3>
     * <p>
     *     레전드 인기 게시글 목록을 페이지네이션으로 조회합니다.
     * </p>
     * @param type 조회할 인기 게시글 유형 (PostCacheFlag.LEGEND)
     * @param pageable 페이지 정보
     * @return 레전드 인기 게시글 목록 페이지
     * @since 2.0.0
     * @author jaeik
     */
    Page<PostSearchResult> getPopularPostLegend(PostCacheFlag type, Pageable pageable);

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>
     *     캐시된 공지사항 목록을 조회합니다.
     * </p>
     * @return 공지사항 목록
     * @since 2.0.0
     * @author jaeik
     */
    List<PostSearchResult> getNoticePosts();

    /**
     * <h3>게시글 ID로 조회 (내부 도메인용)</h3>
     * <p>
     *     다른 도메인에서 게시글 엔티티가 필요한 경우 사용하는 메서드입니다.
     * </p>
     * @param postId 게시글 ID
     * @return 게시글 엔티티 (Optional)
     * @since 2.0.0
     * @author jaeik
     */
    Optional<Post> findById(Long postId);

    /**
     * <h3>사용자 작성 게시글 목록 조회 (도메인 간 연동용)</h3>
     * <p>
     *     특정 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.
     * </p>
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @since 2.0.0
     * @author jaeik
     */
    Page<PostSearchResult> getUserPosts(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천한 게시글 목록 조회 (도메인 간 연동용)</h3>
     * <p>
     *     특정 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.
     * </p>
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지
     * @since 2.0.0
     * @author jaeik
     */
    Page<PostSearchResult> getUserLikedPosts(Long userId, Pageable pageable);
}
