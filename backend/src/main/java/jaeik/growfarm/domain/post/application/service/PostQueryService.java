package jaeik.growfarm.domain.post.application.service;


import jaeik.growfarm.domain.post.application.port.out.PostCommentQueryPort;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.application.port.out.PostCacheQueryPort;
import jaeik.growfarm.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.growfarm.domain.post.application.port.out.PostQueryPort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.PostDetail;
import jaeik.growfarm.domain.post.entity.PostSearchResult;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * <h2>PostQueryService</h2>
 * <p>
 *     PostQueryUseCase의 구현체입니다.
 *     게시글 조회 관련 비즈니스 로직을 처리합니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostQueryService implements PostQueryUseCase {

    private final PostQueryPort postQueryPort;
    private final PostLikeQueryPort postLikeQueryPort;
    private final PostCacheSyncService postCacheSyncService;
    private final PostCacheQueryPort postCacheQueryPort;
    private final PostCommentQueryPort postCommentQueryPort;

    /**
     * <h3>게시판 조회</h3>
     * <p>최신순으로 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> getBoard(Pageable pageable) {
        return postQueryPort.findByPage(pageable);
    }

    /**
     * <h3>인기글 여부 확인</h3>
     * <p>주어진 게시글 ID가 현재 캐시된 인기글(실시간, 주간, 전설, 공지)에 포함되어 있는지 확인합니다.</p>
     * <p>PostCacheQueryPort의 최적화된 메서드를 사용하여 O(1) 시간 복잡도로 확인합니다.</p>
     *
     * @param postId 게시글 ID
     * @return 인기글 여부 (true: 인기글, false: 일반글)
     * @author Jaeik
     * @since 2.0.0
     */
    private boolean isPopularPost(Long postId) {
        return postCacheQueryPort.isPopularPost(postId);
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>게시글 ID를 통해 게시글 상세 정보를 조회합니다.</p>
     * <p>인기글인 경우 캐시에서 먼저 조회를 시도하고, 캐시에 없거나 일반 게시글인 경우 DB에서 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 현재 로그인한 사용자 ID (Optional, 추천 여부 확인용)
     * @return 게시글 상세 정보 DTO
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public PostDetail getPost(Long postId, Long userId) {
        // 1. 인기글인 경우 캐시에서 조회 시도
        if (isPopularPost(postId)) {
            PostDetail cachedPost = postCacheQueryPort.getCachedPost(postId);
            if (cachedPost != null) {
                // 사용자의 좋아요 정보만 추가 확인 필요 (Post 엔티티 로드 없이 ID로만 확인)
                if (userId != null) {
                    boolean isLiked = postLikeQueryPort.existsByPostIdAndUserId(postId, userId);
                    return cachedPost.withIsLiked(isLiked);
                }
                return cachedPost;
            }
        }

        // 2. 캐시에 없거나 일반 게시글인 경우 DB에서 조회
        return getPostFromDatabase(postId, userId);
    }

    /**
     * <h3>데이터베이스에서 게시글 조회</h3>
     * <p>게시글 정보와 관련 데이터를 한 번에 조회하여 PostDetail을 생성합니다.</p>
     * <p>중복 조회 로직을 통합하여 성능을 최적화합니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 현재 로그인한 사용자 ID (Optional)
     * @return 게시글 상세 정보 DTO
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private PostDetail getPostFromDatabase(Long postId, Long userId) {
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        long likeCount = postLikeQueryPort.countByPost(post);
        boolean isLiked = userId != null && postLikeQueryPort.existsByPostIdAndUserId(postId, userId);
        int commentCount = postCommentQueryPort.countByPostId(postId);
        
        return PostDetail.of(post, Math.toIntExact(likeCount), commentCount, isLiked);
    }

    /**
     * <h3>게시글 검색</h3>
     * <p>검색 유형과 검색어를 통해 게시글을 검색하고 최신순으로 페이지네이션합니다.</p>
     *
     * @param type     검색 유형 (예: title, content, writer)
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> searchPost(String type, String query, Pageable pageable) {
        return postQueryPort.findBySearch(type, query, pageable);
    }


    /**
     * <h3>레전드 인기 게시글 목록 조회 (페이징)</h3>
     * <p>캐시된 레전드 게시글을 페이지네이션으로 조회합니다. 캐시가 없는 경우 업데이트 후 조회합니다.</p>
     * <p>Redis List 구조를 활용하여 효율적인 페이징을 제공합니다.</p>
     *
     * @param type 조회할 인기 게시글 유형 (PostCacheFlag.LEGEND만 지원)
     * @param pageable 페이지 정보
     * @return 인기 게시글 목록 페이지
     * @throws CustomException 유효하지 않은 캐시 유형인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> getPopularPostLegend(PostCacheFlag type, Pageable pageable) {
        // 타입 검증: LEGEND만 허용
        if (type != PostCacheFlag.LEGEND) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        if (!postCacheQueryPort.hasPopularPostsCache(type)) {
            postCacheSyncService.updateLegendaryPosts();
        }
        return postCacheQueryPort.getCachedPostListPaged(type, pageable);
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>캐시된 공지사항 목록을 조회합니다. 캐시가 없는 경우 PostCacheManageService를 통해 업데이트 후 조회합니다.</p>
     *
     * @return 공지사항 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<PostSearchResult> getNoticePosts() {
        return postCacheQueryPort.getCachedPostList(PostCacheFlag.NOTICE);
    }

    /**
     * <h3>게시글 ID로 조회 </h3>
     * <p>다른 도메인에서 게시글 엔티티가 필요한 경우 사용하는 메소드입니다.</p>
     *
     * @param postId 게시글 ID
     * @return 게시글 엔티티 (Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public java.util.Optional<Post> findById(Long postId) {
        return postQueryPort.findById(postId);
    }

    /**
     * <h3>사용자 작성 게시글 목록 조회 </h3>
     * <p>특정 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> getUserPosts(Long userId, Pageable pageable) {
        return postQueryPort.findPostsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 게시글 목록 조회 </h3>
     * <p>특정 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> getUserLikedPosts(Long userId, Pageable pageable) {
        return postQueryPort.findLikedPostsByUserId(userId, pageable);
    }

    /**
     * <h3>실시간/주간 인기 게시글 일괄 조회</h3>
     * <p>Redis 캐시에서 실시간과 주간 인기 게시글을 한 번에 조회합니다.</p>
     * <p>각 타입별로 캐시가 없는 경우 개별적으로 PostCacheSyncService를 통해 DB에서 생성합니다.</p>
     * <p>Frontend에서 홈페이지 로딩 시 두 타입을 동시에 필요로 하는 API용 편의 메서드입니다.</p>
     *
     * @return Redis에서 조회된 실시간/주간 인기 게시글 맵 (key: "realtime"/"weekly", value: 게시글 목록)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Map<String, List<PostSearchResult>> getRealtimeAndWeeklyPosts() {
        // 캐시 상태 확인 및 업데이트
        if (!postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)) {
            postCacheSyncService.updateRealtimePopularPosts();
        }
        if (!postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)) {
            postCacheSyncService.updateWeeklyPopularPosts();
        }
        
        // 두 타입의 데이터를 한 번에 조회
        List<PostSearchResult> realtimePosts = postCacheQueryPort.getCachedPostList(PostCacheFlag.REALTIME);
        List<PostSearchResult> weeklyPosts = postCacheQueryPort.getCachedPostList(PostCacheFlag.WEEKLY);
        
        return Map.of(
            "realtime", realtimePosts,
            "weekly", weeklyPosts
        );
    }

}
