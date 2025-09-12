package jaeik.bimillog.domain.post.application.service;


import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostCacheQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.infrastructure.adapter.post.in.web.PostQueryController;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * <h2>게시글 조회 서비스</h2>
 * <p>게시글 도메인의 조회 비즈니스 로직을 처리하는 서비스입니다.</p>
 * <p>게시판 목록 조회, 게시글 상세 조회, 검색 기능</p>
 * <p>인기글 조회, 사용자 활동 내역 조회</p>
 * <p>Redis 캐시와 MySQL 조회를 조합한 성능 최적화</p>
 *
 * @author Jaeik
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

    /**
     * <h3>게시판 목록 조회</h3>
     * <p>전체 게시글을 최신순으로 정렬하여 페이지 단위로 조회합니다.</p>
     * <p>공지사항은 제외하고 일반 게시글만 조회</p>
     * <p>{@link PostQueryController}에서 게시판 목록 요청 시 호출됩니다.</p>
     *
     * @param pageable 페이지 정보 (크기, 페이지 번호, 정렬 기준)
     * @return Page&lt;PostSearchResult&gt; 페이지네이션된 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> getBoard(Pageable pageable) {
        return postQueryPort.findByPage(pageable);
    }


    /**
     * <h3>게시글 상세 조회</h3>
     * <p>특정 게시글의 상세 내용과 댓글수, 추천수, 사용자 추천여부를 조회합니다.</p>
     * <p>인기 게시글인 경우 Redis 캐시에서 우선 조회하고, 캐시 미스 시 JOIN 쿼리로 조회합니다.</p>
     * <p>{@link PostQueryController}에서 게시글 상세 조회 요청 시 호출됩니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 현재 로그인한 사용자 ID (추천 여부 확인용, null 허용)
     * @return PostDetail 게시글 상세 정보 (좋아요 수, 댓글 수, 사용자 좋아요 여부 포함)
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public PostDetail getPost(Long postId, Long userId) {
        // 1. 캐시에서 인기글 조회 시도 (최적화: 1번의 Redis 호출로 통합)
        PostDetail cachedPost = postCacheQueryPort.getCachedPostIfExists(postId);
        if (cachedPost != null) {
            // 캐시 히트: 사용자 좋아요 정보만 추가 확인 필요
            if (userId != null) {
                boolean isLiked = postLikeQueryPort.existsByPostIdAndUserId(postId, userId);
                return cachedPost.withIsLiked(isLiked);
            }
            return cachedPost;
        }

        // 2. 캐시 미스 또는 일반 게시글: 최적화된 JOIN 쿼리로 한방에 조회
        return getPostFromDatabaseOptimized(postId, userId);
    }

    /**
     * <h3>데이터베이스에서 게시글 조회</h3>
     * <p>캐시 미스 또는 일반 게시글에 대해 JOIN 쿼리로 모든 필요 정보를 조회합니다.</p>
     * <p>게시글 기본 정보, 좋아요 수, 댓글 수, 사용자 좋아요 여부를 한번에 처리합니다.</p>
     * <p>getPost 메서드에서 캐시 조회 실패 시 호출됩니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 현재 로그인한 사용자 ID (좋아요 여부 확인용, null 허용)
     * @return PostDetail 게시글 상세 정보 DTO
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private PostDetail getPostFromDatabaseOptimized(Long postId, Long userId) {
        return postQueryPort.findPostDetailWithCounts(postId, userId)
                .orElseThrow(() -> new PostCustomException(PostErrorCode.POST_NOT_FOUND));
    }

    /**
     * <h3>게시글 검색</h3>
     * <p>검색 타입과 검색어를 기반으로 게시글을 검색합니다.</p>
     * <p>MySQL Full-text Search와 ngram 파서를 활용한 한국어 검색을 지원합니다.</p>
     * <p>{@link PostQueryController}에서 검색 요청 시 호출됩니다.</p>
     *
     * @param type     검색 유형 (title: 제목, content: 내용, writer: 작성자)
     * @param query    검색어 (한글, 영문, 숫자 지원)
     * @param pageable 페이지 정보 (크기, 페이지 번호, 정렬 기준)
     * @return Page&lt;PostSearchResult&gt; 검색된 게시글 목록 페이지
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
     * @throws PostCustomException 유효하지 않은 캐시 유형인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> getPopularPostLegend(PostCacheFlag type, Pageable pageable) {
        // 타입 검증: LEGEND만 허용
        if (type != PostCacheFlag.LEGEND) {
            throw new PostCustomException(PostErrorCode.INVALID_INPUT_VALUE);
        }

        if (!postCacheQueryPort.hasPopularPostsCache(type)) {
            postCacheSyncService.updateLegendaryPosts();
        }
        return postCacheQueryPort.getCachedPostListPaged(pageable);
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
     * <h3>게시글 엔티티 조회 (도메인 간 통신용)</h3>
     * <p>PostQueryUseCase 인터페이스의 엔티티 조회 기능을 구현하며, 다른 도메인에서 Post 엔티티가 필요한 경우 사용됩니다.</p>
     * <p>Comment 도메인에서 댓글 작성 시 게시글 존재성 검증과 알림 발송을 위해 호출됩니다.</p>
     * <p>Admin 도메인에서 신고 처리 시 게시글 정보 확인을 위해 호출됩니다.</p>
     * <p>순수한 엔티티를 반환하여 도메인 경계를 명확히 유지합니다.</p>
     *
     * @param postId 게시글 ID
     * @return Post 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Post findById(Long postId) {
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
