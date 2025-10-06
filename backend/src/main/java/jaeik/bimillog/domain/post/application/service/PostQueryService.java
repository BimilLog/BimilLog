package jaeik.bimillog.domain.post.application.service;


import jaeik.bimillog.domain.global.application.port.out.GlobalPostQueryPort;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostQueryPort;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.infrastructure.adapter.in.post.web.PostQueryController;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
@RequiredArgsConstructor
public class PostQueryService implements PostQueryUseCase {

    private final PostQueryPort postQueryPort;
    private final GlobalPostQueryPort globalPostQueryPort;
    private final PostLikeQueryPort postLikeQueryPort;
    private final PostCacheSyncService postCacheSyncService;
    private final RedisPostQueryPort redisPostQueryPort;
    private final RedisPostCommandPort redisPostCommandPort;

    /**
     * <h3>게시판 목록 조회</h3>
     * <p>전체 게시글을 최신순으로 정렬하여 페이지 단위로 조회합니다.</p>
     * <p>공지사항은 제외하고 일반 게시글만 조회</p>
     * <p>{@link PostQueryController}에서 게시판 목록 요청 시 호출됩니다.</p>
     *
     * @param pageable 페이지 정보 (크기, 페이지 번호, 정렬 기준)
     * @return Page&lt;PostSimpleDetail&gt; 페이지네이션된 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSimpleDetail> getBoard(Pageable pageable) {
        return postQueryPort.findByPage(pageable);
    }


    /**
     * <h3>게시글 상세 조회 (캐시 어사이드 패턴)</h3>
     * <p>모든 게시글에 대해 Redis 캐시를 우선 확인하고, 캐시 미스 시 DB 조회 후 캐시에 저장합니다.</p>
     * <p>캐시 히트: 사용자 좋아요 정보만 추가 확인</p>
     * <p>캐시 미스: DB 조회 → 캐시 저장 → 반환</p>
     * <p>{@link PostQueryController}에서 게시글 상세 조회 요청 시 호출됩니다.</p>
     *
     * @param postId 게시글 ID
     * @param memberId 현재 로그인한 사용자 ID (추천 여부 확인용, null 허용)
     * @return PostDetail 게시글 상세 정보 (좋아요 수, 댓글 수, 사용자 좋아요 여부 포함)
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public PostDetail getPost(Long postId, Long memberId) {
        // 1. 캐시 확인 (Cache-Aside Read)
        PostDetail cachedPost = redisPostQueryPort.getCachedPostIfExists(postId);
        if (cachedPost != null) {
            // 캐시 히트: 사용자 좋아요 정보만 추가 확인
            if (memberId != null) {
                boolean isLiked = postLikeQueryPort.existsByPostIdAndUserId(postId, memberId);
                return cachedPost.withIsLiked(isLiked);
            }
            return cachedPost;
        }

        // 2. 캐시 미스: DB 조회 후 캐시 저장
        PostDetail postDetail = getPostFromDatabaseOptimized(postId, memberId);
        redisPostCommandPort.cachePostDetail(postDetail);
        return postDetail;
    }

    /**
     * <h3>데이터베이스에서 게시글 조회</h3>
     * <p>캐시 미스 또는 일반 게시글에 대해 JOIN 쿼리로 모든 필요 정보를 조회합니다.</p>
     * <p>게시글 기본 정보, 좋아요 수, 댓글 수, 사용자 좋아요 여부를 한번에 처리합니다.</p>
     * <p>getPost 메서드에서 캐시 조회 실패 시 호출됩니다.</p>
     *
     * @param postId 게시글 ID
     * @param memberId 현재 로그인한 사용자 ID (좋아요 여부 확인용, null 허용)
     * @return PostDetail 게시글 상세 정보 DTO
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private PostDetail getPostFromDatabaseOptimized(Long postId, Long memberId) {
        return postQueryPort.findPostDetailWithCounts(postId, memberId)
                .orElseThrow(() -> new PostCustomException(PostErrorCode.POST_NOT_FOUND));
    }

    /**
     * <h3>게시글 검색 전략 선택</h3>
     * <p>검색 조건에 따라 최적의 검색 전략을 선택하여 게시글을 검색합니다.</p>
     * <p>검색 전략:</p>
     * <ul>
     *     <li>3글자 이상 + WRITER 아님 → 전문검색 시도 (실패 시 부분검색)</li>
     *     <li>WRITER + 4글자 이상 → 접두사 검색 (인덱스 활용)</li>
     *     <li>그 외 → 부분 검색</li>
     * </ul>
     * <p>{@link PostQueryController}에서 검색 요청 시 호출됩니다.</p>
     *
     * @param type     검색 유형 (TITLE, WRITER, TITLE_CONTENT)
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return Page&lt;PostSimpleDetail&gt; 검색된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSimpleDetail> searchPost(PostSearchType type, String query, Pageable pageable) {
        // 전략 1: 3글자 이상 + 작성자 검색 아님 → 전문 검색 시도
        if (query.length() >= 3 && type != PostSearchType.WRITER) {
            Page<PostSimpleDetail> fullTextResult = postQueryPort.findByFullTextSearch(type, query, pageable);
            if (!fullTextResult.isEmpty()) {
                return fullTextResult;
            }
            // 전문 검색 실패 시 부분 검색으로 폴백
            return postQueryPort.findByPartialMatch(type, query, pageable);
        }

        // 전략 2: 작성자 검색 + 4글자 이상 → 접두사 검색 (인덱스 활용)
        if (type == PostSearchType.WRITER && query.length() >= 4) {
            return postQueryPort.findByPrefixMatch(type, query, pageable);
        }

        // 전략 3: 그 외 → 부분 검색
        return postQueryPort.findByPartialMatch(type, query, pageable);
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
    public Page<PostSimpleDetail> getPopularPostLegend(PostCacheFlag type, Pageable pageable) {
        // 타입 검증: LEGEND만 허용
        if (type != PostCacheFlag.LEGEND) {
            throw new PostCustomException(PostErrorCode.INVALID_INPUT_VALUE);
        }
        return redisPostQueryPort.getCachedPostListPaged(pageable);
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>Redis에 캐시된 공지사항 목록을 조회합니다.</p>
     * <p>캐시가 없는 경우 빈 리스트를 반환합니다.</p>
     * <p>공지사항 등록/해제는 PostCacheService.syncNoticeCache()를 통해 관리됩니다.</p>
     *
     * @return 공지사항 목록 (캐시가 없으면 빈 리스트)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<PostSimpleDetail> getNoticePosts() {
        return redisPostQueryPort.getCachedPostList(PostCacheFlag.NOTICE);
    }

    /**
     * <h3>게시글 엔티티 조회 </h3>
     * <p>PostQueryUseCase 인터페이스의 엔티티 조회 기능을 구현 Post 엔티티가 필요한 경우 사용</p>
     *
     * @param postId 게시글 ID
     * @return Post 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Post findById(Long postId) {
        return globalPostQueryPort.findById(postId);
    }

    /**
     * <h3>사용자 작성 게시글 목록 조회 </h3>
     * <p>특정 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSimpleDetail> getMemberPosts(Long memberId, Pageable pageable) {
        return postQueryPort.findPostsByMemberId(memberId, pageable);
    }

    /**
     * <h3>사용자 추천한 게시글 목록 조회 </h3>
     * <p>특정 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSimpleDetail> getMemberLikedPosts(Long memberId, Pageable pageable) {
        return postQueryPort.findLikedPostsByMemberId(memberId, pageable);
    }

    /**
     * <h3>실시간/주간 인기 게시글 일괄 조회</h3>
     * <p>실시간 인기글은 Redis Sorted Set에서 postId 목록을 조회하고, 주간 인기글은 캐시를 우선 확인합니다.</p>
     * <p>실시간 인기글: 이벤트 기반 점수 시스템으로 관리되는 postId 목록 조회 → 상세 캐시 활용</p>
     * <p>주간 인기글: 캐시가 없는 경우 PostCacheSyncService를 통해 DB에서 생성</p>
     * <p>Frontend에서 홈페이지 로딩 시 두 타입을 동시에 필요로 하는 API용 편의 메서드입니다.</p>
     *
     * @return Redis에서 조회된 실시간/주간 인기 게시글 맵 (key: "realtime"/"weekly", value: 게시글 목록)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Map<String, List<PostSimpleDetail>> getRealtimeAndWeeklyPosts() {
        // 실시간 인기글: Redis Sorted Set에서 postId 목록 조회 후 상세 캐시 활용
        List<Long> realtimePostIds = redisPostQueryPort.getRealtimePopularPostIds();
        List<PostSimpleDetail> realtimePosts = realtimePostIds.stream()
                .map(postId -> {
                    // 캐시 어사이드 패턴으로 조회 (캐시 미스 시 DB 조회 후 캐시 저장)
                    PostDetail postDetail = redisPostQueryPort.getCachedPostIfExists(postId);
                    if (postDetail == null) {
                        postDetail = postQueryPort.findPostDetailWithCounts(postId, null).orElse(null);
                        if (postDetail != null) {
                            redisPostCommandPort.cachePostDetail(postDetail);
                        }
                    }
                    return postDetail;
                })
                .filter(java.util.Objects::nonNull)
                .map(PostDetail::toSearchResult)
                .toList();
        List<PostSimpleDetail> weeklyPosts = redisPostQueryPort.getCachedPostList(PostCacheFlag.WEEKLY);

        return Map.of(
            "realtime", realtimePosts,
            "weekly", weeklyPosts
        );
    }
}
