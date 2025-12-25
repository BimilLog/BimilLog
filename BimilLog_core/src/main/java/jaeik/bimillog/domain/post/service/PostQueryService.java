package jaeik.bimillog.domain.post.service;


import jaeik.bimillog.domain.global.out.GlobalMemberBlacklistAdapter;
import jaeik.bimillog.domain.post.controller.PostQueryController;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.out.*;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.post.RedisPostQueryAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostSaveAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>게시글 조회 서비스</h2>
 * <p>게시글 도메인의 조회 비즈니스 로직을 처리하는 서비스입니다.</p>
 * <p>게시판 목록 조회, 게시글 상세 조회, 검색 기능, 인기글 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostQueryService {
    private final PostQueryRepository postQueryRepository;
    private final PostLikeRepository postLikeRepository;
    private final RedisPostQueryAdapter redisPostQueryAdapter;
    private final RedisPostSaveAdapter redisPostSaveAdapter;
    private final PostRepository postRepository;
    private final PostToCommentAdapter postToCommentAdapter;
    private final GlobalMemberBlacklistAdapter globalMemberBlacklistAdapter;
    private final PostSearchRepository postSearchRepository;

    /**
     * <h3>게시판 목록 조회</h3>
     * <p>전체 게시글을 최신순으로 정렬하여 페이지 단위로 조회합니다.</p>
     * <p>공지사항은 제외하고 일반 게시글만 조회</p>
     * <p>{@link PostQueryController}에서 게시판 목록 요청 시 호출됩니다.</p>
     *
     * @param pageable 페이지 정보 (크기, 페이지 번호, 정렬 기준)
     * @return Page&lt;PostSimpleDetail&gt; 페이지네이션된 게시글 목록
     * @author Jaeik
     * @since 2.3.0
     */
    public Page<PostSimpleDetail> getBoard(Pageable pageable, Long memberId) {
        Page<PostSimpleDetail> posts = postQueryRepository.findByPage(pageable, memberId);
        List<PostSimpleDetail> blackListFilterPosts = removePostsWithBlacklist(memberId, posts.getContent());
        enrichPostsCommentCount(blackListFilterPosts);

        return new PageImpl<>(blackListFilterPosts, posts.getPageable(),
                posts.getTotalElements() - (posts.getContent().size() - blackListFilterPosts.size()));
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>모든 게시글에 대해 Redis 캐시를 우선 확인하고, 캐시 미스 시 DB 조회 후 캐시에 저장합니다.</p>
     * <p>캐시 히트: 사용자 좋아요 정보만 추가 확인</p>
     * <p>캐시 미스: DB 조회 → 캐시 저장 → 반환</p>
     * <p>{@link PostQueryController}에서 게시글 상세 조회 요청 시 호출됩니다.</p>
     *
     * @param postId 게시글 ID
     * @param memberId 현재 로그인한 사용자 ID (추천 여부 확인용, null 허용)
     * @return PostDetail 게시글 상세 정보 (좋아요 수, 댓글 수, 사용자 좋아요 여부 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    public PostDetail getPost(Long postId, Long memberId) {
        try {
            // 1. 캐시 확인 (Cache-Aside Read)
            PostDetail cachedPost = redisPostQueryAdapter.getCachedPostIfExists(postId);
//            if (cachedPost != null) {
//                // 비회원 확인
//                if (memberId != null) {
//                    // 블랙리스트 확인
//                    globalMemberBlacklistAdapter.checkMemberBlacklist(memberId, cachedPost.getMemberId());
//                }
//                 // 캐시 히트: 사용자 좋아요 정보만 추가 확인
//                 if (memberId != null) {
//                     boolean isLiked = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
//                     return cachedPost.withIsLiked(isLiked);
//                 }
            if (cachedPost != null) {
                return cachedPost;  // ✅ null이면 DB 조회로 진행
            }

//            }
        } catch (Exception e) {
            // 캐시 조회 실패 시 로그만 남기고 DB 조회로 진행
            log.warn("게시글 {} 캐시 조회 실패, DB 조회로 진행: {}", postId, e.getMessage());
        }

        // 2. 캐시 미스 또는 캐시 조회 실패: DB 조회 후 캐시 저장
        PostDetail postDetail = postQueryRepository.findPostDetailWithCounts(postId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        // 비회원 확인
//        if (memberId != null) {
//            // 블랙리스트 확인
//            globalMemberBlacklistAdapter.checkMemberBlacklist(memberId, postDetail.getMemberId());
//        }

        try {
            redisPostSaveAdapter.cachePostDetail(postDetail);
        } catch (Exception e) {
            // 캐시 저장 실패 시 로그만 남기고 계속 진행
            log.warn("게시글 {} 캐시 저장 실패: {}", postId, e.getMessage());
        }

        return postDetail;
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
    public Optional<Post> findById(Long postId) {
        return postRepository.findById(postId);
    }

    /**
     * <h3>게시글 ID 목록으로 게시글 리스트 반환</h3>
     */
    public List<Post> findAllByIds(List<Long> postIds) {
        return postQueryRepository.findAllByIds(postIds);
    }

    /**
     * <h3>사용자 작성, 추천 글 목록 조회</h3>
     * <p>특정 사용자가 작성, 추천한 글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return MemberActivityPost 마이페이지 글 정보
     * @author Jaeik
     * @since 2.0.0
     */
    public MemberActivityPost getMemberActivityPosts(Long memberId, Pageable pageable) {
        Page<PostSimpleDetail> writePosts = postQueryRepository.findPostsByMemberId(memberId, pageable, memberId);
        Page<PostSimpleDetail> likedPosts = postQueryRepository.findLikedPostsByMemberId(memberId, pageable);
        enrichPostsCommentCount(writePosts.getContent());
        enrichPostsCommentCount(likedPosts.getContent());
        return new MemberActivityPost(writePosts, likedPosts);
    }

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>어댑터에서 조회한 인기글 목록에 댓글 수를 주입합니다.</p>
     *
     * @return List<PostSimpleDetail> 주간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<PostSimpleDetail> getWeeklyPopularPosts() {
        List<PostSimpleDetail> posts = postQueryRepository.findWeeklyPopularPosts();
        enrichPostsCommentCount(posts);
        return posts;
    }

    /**
     * <h3>레전드 게시글 조회</h3>
     * <p>어댑터에서 조회한 레전드 글 목록에 댓글 수를 주입합니다.</p>
     *
     * @return List<PostSimpleDetail> 레전드 게시글 목록
     */
    public List<PostSimpleDetail> getLegendaryPosts() {
        List<PostSimpleDetail> posts = postQueryRepository.findLegendaryPosts();
        enrichPostsCommentCount(posts);
        return posts;
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
    public Page<PostSimpleDetail> searchPost(PostSearchType type, String query, Pageable pageable, Long memberId) {
        Page<PostSimpleDetail> posts;

        // 전략 1: 3글자 이상 + 작성자 검색 아님 → 전문 검색 시도
        if (query.length() >= 3 && type != PostSearchType.WRITER) {
            posts = postSearchRepository.findByFullTextSearch(type, query, pageable, memberId);
        }
        // 전략 2: 작성자 검색 + 4글자 이상 → 접두사 검색 (인덱스 활용)
        else if (type == PostSearchType.WRITER && query.length() >= 4) {
            posts = postSearchRepository.findByPrefixMatch(type, query, pageable, memberId);
        }
        // 전략 3: 그 외 → 부분 검색
        else {
            posts = postSearchRepository.findByPartialMatch(type, query, pageable, memberId);
        }

        List<PostSimpleDetail> blackListFilterPosts = removePostsWithBlacklist(memberId, posts.getContent());
        enrichPostsCommentCount(blackListFilterPosts);

        return new PageImpl<>(blackListFilterPosts, posts.getPageable(),
                posts.getTotalElements() - (posts.getContent().size() - blackListFilterPosts.size()));
    }

    /**
     * <h3>게시글 목록에 댓글 수 주입</h3>
     * <p>게시글 목록의 댓글 수를 배치로 조회하여 주입합니다.</p>
     * <p>좋아요 수는 PostQueryHelper에서 이미 처리되므로, 여기서는 댓글 수만 처리합니다.</p>
     *
     * @param posts 댓글 수를 채울 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private void enrichPostsCommentCount(List<PostSimpleDetail> posts) {
        if (posts.isEmpty()) {
            return;
        }

        List<Long> postIds = posts.stream()
                .map(PostSimpleDetail::getId)
                .toList();

        Map<Long, Integer> commentCounts = postToCommentAdapter.findCommentCountsByPostIds(postIds);

        posts.forEach(post -> {
            post.setCommentCount(commentCounts.getOrDefault(post.getId(), 0));
        });
    }

    /**
     * <h3>게시글에서 블랙리스트 제거</h3>
     */
    private List<PostSimpleDetail> removePostsWithBlacklist(Long memberId, List<PostSimpleDetail> posts) {
        if (memberId == null || posts.isEmpty()) {
            return posts;
        }

        List<Long> blacklistIds = globalMemberBlacklistAdapter.getInterActionBlacklist(memberId);
        Set<Long> blacklistSet = new HashSet<>(blacklistIds);
        return posts.stream().filter(post -> !blacklistSet.contains(post.getMemberId())).collect(Collectors.toList());
    }
}
