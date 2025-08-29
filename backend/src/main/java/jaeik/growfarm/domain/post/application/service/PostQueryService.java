package jaeik.growfarm.domain.post.application.service;


import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.application.port.out.*;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.user.entity.User;
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
import java.util.Objects;

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
    private final LoadUserInfoPort loadUserInfoPort;
    private final PostCacheSyncService postCacheSyncService;
    private final PostCacheQueryPort postCacheQueryPort;
    private final CommentQueryUseCase commentQueryUseCase;

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
     *
     * @param postId 게시글 ID
     * @return 인기글 여부 (true: 인기글, false: 일반글)
     * @author Jaeik
     * @since 2.0.0
     */
    private boolean isPopularPost(Long postId) {
        // 모든 인기글 타입에 대해 확인
        for (PostCacheFlag flag : PostCacheFlag.getPopularPostTypes()) {
            // 해당 타입의 캐시가 있는지 확인
            if (postCacheQueryPort.hasPopularPostsCache(flag)) {
                // 캐시된 인기글 목록 조회
                List<PostSearchResult> cachedPosts = postCacheQueryPort.getCachedPostList(flag);
                // 해당 ID의 게시글이 목록에 있는지 확인
                if (cachedPosts.stream().anyMatch(post -> post.getId().equals(postId))) {
                    return true;
                }
            }
        }
        return false;
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
                // 사용자의 좋아요 정보만 추가 확인 필요
                if (userId != null) {
                    Post post = postQueryPort.findById(postId)
                            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
                    boolean isLiked = checkUserLikedPost(post, userId);
                    return cachedPost.withIsLiked(isLiked);
                }
                return cachedPost;
            }
        }

        // 2. 캐시에 없거나 일반 게시글인 경우 DB에서 조회
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        long likeCount = postLikeQueryPort.countByPost(post);
        boolean isLiked = (userId != null) ? checkUserLikedPost(post, userId) : false;
        int commentCount = commentQueryUseCase.countByPostId(postId);
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
     * <h3>실시간, 주간 인기 게시글 목록 조회</h3>
     * <p>캐시된 인기 게시글 목록(실시간, 주간)을 조회합니다. 캐시가 없는 경우 업데이트 후 조회합니다.</p>
     *
     * @param type 조회할 인기 게시글 유형
     * @return 인기 게시글 목록
     * @throws CustomException 유효하지 않은 캐시 유형인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<PostSearchResult> getPopularPosts(PostCacheFlag type) {
        if (!postCacheQueryPort.hasPopularPostsCache(type)) {
            switch (type) {
                case REALTIME -> postCacheSyncService.updateRealtimePopularPosts();
                case WEEKLY -> postCacheSyncService.updateWeeklyPopularPosts();
                case LEGEND -> postCacheSyncService.updateLegendaryPosts();
                default -> throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        return postCacheQueryPort.getCachedPostList(type);
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
     * <h3>실시간/주간 인기 게시글 목록 일괄 조회</h3>
     * <p>실시간과 주간 인기 게시글을 한 번에 조회합니다.</p>
     * <p>성능 최적화를 위해 한 번의 호출로 두 타입의 데이터를 가져옵니다.</p>
     *
     * @return 실시간/주간 인기 게시글 맵 (key: "realtime"/"weekly", value: 게시글 목록)
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

    /**
     * <h3>사용자 게시글 좋아요 여부 확인</h3>
     * <p>중복 로직을 제거하기 위한 private 메서드</p>
     *
     * @param post 게시글 엔티티
     * @param userId 사용자 ID
     * @return boolean 좋아요 여부
     * @author Jaeik
     * @since 2.0.0
     */
    private boolean checkUserLikedPost(Post post, Long userId) {
        User user = loadUserInfoPort.getReferenceById(userId);
        return postLikeQueryPort.existsByUserAndPost(user, post);
    }
}
