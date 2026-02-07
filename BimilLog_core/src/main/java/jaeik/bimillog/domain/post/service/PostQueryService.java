package jaeik.bimillog.domain.post.service;


import jaeik.bimillog.domain.global.event.CheckBlacklistEvent;
import jaeik.bimillog.domain.post.adapter.PostToCommentAdapter;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.controller.PostQueryController;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.domain.post.repository.*;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jaeik.bimillog.domain.post.dto.CursorPageResponse;

import java.util.*;
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.FIRST_PAGE_SIZE;

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
@Log
public class PostQueryService {
    private final PostQueryRepository postQueryRepository;
    private final PostReadModelQueryRepository postReadModelQueryRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final PostToCommentAdapter postToCommentAdapter;
    private final PostToMemberAdapter postToMemberAdapter;
    private final PostSearchRepository postSearchRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisFirstPagePostAdapter redisFirstPagePostAdapter;

    /**
     * <h3>게시판 목록 조회</h3>
     * <p>PostReadModel에서 커서 기반 페이지네이션으로 게시글 목록을 조회합니다.</p>
     * <p>회원은 블랙리스트 필터링이 적용됩니다.</p>
     * <p>첫 페이지 요청 시 Redis 캐시를 사용합니다.</p>
     *
     * @param cursor   마지막으로 조회한 게시글 ID (null이면 처음부터)
     * @param size     조회할 개수
     * @param memberId 회원 ID (null이면 비회원)
     * @return CursorPageResponse 커서 기반 페이지 응답
     */
    public CursorPageResponse<PostSimpleDetail> getBoardByCursor(Long cursor, int size, Long memberId) {
        // === 첫 페이지 캐시 분기 ===
        if (cursor == null) {
            return getFirstPage(memberId, size);
        }

        // PostReadModel에서 조회 (비정규화된 단일 테이블)
        List<PostSimpleDetail> posts = postReadModelQueryRepository.findBoardPostsByCursor(cursor, size);

        // hasNext 판단: size + 1개 조회했으므로 size보다 많으면 다음 페이지 존재
        boolean hasNext = posts.size() > size;
        if (hasNext) {
            posts = new ArrayList<>(posts.subList(0, size));
        }

        // 비회원이면 바로 반환 (PostReadModel에 이미 commentCount 포함)
        if (memberId == null) {
            Long nextCursor = posts.isEmpty() ? null : posts.getLast().getId();
            return CursorPageResponse.of(posts, nextCursor, hasNext, size);
        }

        // 회원이면 블랙리스트 필터링
        List<PostSimpleDetail> filteredPosts = removePostsWithBlacklist(memberId, posts);

        Long nextCursor = filteredPosts.isEmpty() ? null : filteredPosts.getLast().getId();
        return CursorPageResponse.of(filteredPosts, nextCursor, hasNext, size);
    }

    /**
     * <h3>첫 페이지 조회</h3>
     * <p>캐시에서 게시글 목록을 조회하고, 회원인 경우 블랙리스트 필터링을 적용합니다.</p>
     *
     * @param memberId 회원 ID (null이면 비회원)
     * @return 게시글 목록 (블랙리스트 필터링 적용됨)
     */
    public CursorPageResponse<PostSimpleDetail> getFirstPage(Long memberId, int size) {
        // 1. 캐시 조회
        List<PostSimpleDetail> posts = redisFirstPagePostAdapter.getFirstPage();

        // 2. 캐시 미스 시 DB 조회 (Redis 장애 대비 - 정상 시 발생 안 함)
        if (posts.isEmpty()) {
            log.warn("[FIRST_PAGE_CACHE] 캐시 미스 - DB 폴백");
            posts = postReadModelQueryRepository.findBoardPostsByCursor(null, FIRST_PAGE_SIZE);

            // findBoardPostsByCursor는 size+1 반환하므로 자르기
            if (posts.size() > FIRST_PAGE_SIZE) {
                posts = posts.subList(0, FIRST_PAGE_SIZE);
            }
        }

        // 3. 블랙리스트 필터링 (회원만)
        if (memberId != null) {
            posts = removePostsWithBlacklist(memberId, posts);
        }

        if (!posts.isEmpty()) {
            // 요청된 size만큼만 반환
            boolean hasNext = posts.size() > size;
            List<PostSimpleDetail> resultPosts = hasNext
                    ? new ArrayList<>(posts.subList(0, size))
                    : posts;
            Long nextCursor = resultPosts.isEmpty() ? null : resultPosts.getLast().getId();
            return CursorPageResponse.of(resultPosts, nextCursor, hasNext, size);
        }
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>DB에서 직접 조회합니다. (상세 캐시 제거됨)</p>
     * <p>회원일 경우 블랙리스트 조사 및 좋아요 확인 후 주입하여 반환합니다.</p>
     * <p>중복 조회 방지 후 조회수 버퍼링 및 실시간 인기글 점수 증가 이벤트를 발행합니다.</p>
     *
     * @param postId    게시글 ID
     * @param memberId  현재 로그인한 사용자 ID (추천 여부 확인용, null 허용)
     * @param viewerKey 조회자 식별 키 (중복 조회 방지용)
     * @return PostDetail 게시글 상세 정보 (좋아요 수, 댓글 수, 사용자 좋아요 여부 포함)
     */
    @Transactional
    public PostDetail getPost(Long postId, Long memberId, String viewerKey) {
        // 1. DB 조회
        PostDetail result = postQueryRepository.findPostDetail(postId, null)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. 조회수 증가 + 실시간 인기글 점수 이벤트 발행 (비동기 리스너에서 중복 체크)
        eventPublisher.publishEvent(new PostViewedEvent(postId, viewerKey));

        // 3. 비회원이면 바로 반환
        if (memberId == null) {
            return result;
        }

        // 4. 회원이면 좋아요 여부 확인 및 블랙리스트 체크
        boolean isLiked = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
        result = result.withIsLiked(isLiked);
        eventPublisher.publishEvent(new CheckBlacklistEvent(memberId, result.getMemberId()));
        return result;
    }

    /**
     * <h3>사용자 작성, 추천 글 목록 조회</h3>
     * <p>특정 사용자가 작성, 추천한 글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param pageable 페이지 정보
     * @return MemberActivityPost 마이페이지 글 정보
     */
    public MemberActivityPost getMemberActivityPosts(Long memberId, Pageable pageable) {
        Page<PostSimpleDetail> writePosts = postQueryRepository.findPostsByMemberId(memberId, pageable, memberId);
        Page<PostSimpleDetail> likedPosts = postQueryRepository.findLikedPostsByMemberId(memberId, pageable);
        enrichPostsCommentCount(writePosts.getContent());
        enrichPostsCommentCount(likedPosts.getContent());
        return new MemberActivityPost(writePosts, likedPosts);
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
     * <h3>게시글 엔티티 조회 </h3>
     *
     * @param postId 게시글 ID
     * @return Post 게시글 엔티티
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
     * <h3>게시글 목록에 댓글 수 주입</h3>
     * <p>게시글 목록의 댓글 수를 배치로 조회하여 주입합니다.</p>
     * <p>좋아요 수는 PostQueryHelper에서 이미 처리되므로, 여기서는 댓글 수만 처리합니다.</p>
     *
     * @param posts 댓글 수를 채울 게시글 목록
     */
    private void enrichPostsCommentCount(List<PostSimpleDetail> posts) {
        List<Long> postIds = posts.stream().map(PostSimpleDetail::getId).toList();
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

        List<Long> blacklistIds = postToMemberAdapter.getInterActionBlacklist(memberId);
        Set<Long> blacklistSet = new HashSet<>(blacklistIds);
        return posts.stream().filter(post -> !blacklistSet.contains(post.getMemberId())).collect(Collectors.toList());
    }
}
