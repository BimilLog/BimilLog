package jaeik.bimillog.domain.post.service;


import jaeik.bimillog.domain.global.event.CheckBlacklistEvent;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.async.RealtimePostSync;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.*;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jaeik.bimillog.domain.post.dto.CursorPageResponse;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>게시글 조회 서비스</h2>
 * <p>게시글 도메인의 조회 비즈니스 로직을 처리하는 서비스입니다.</p>
 * <p>게시판 목록 조회, 게시글 상세 조회, 검색 기능, 인기글 조회</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Log
public class PostQueryService {
    private final PostQueryRepository postQueryRepository;
    private final PostRepository postRepository;
    private final PostToMemberAdapter postToMemberAdapter;
    private final ApplicationEventPublisher eventPublisher;
    private final PostCacheService postCacheService;
    private final RealtimePostSync realtimePostSync;

    /**
     * <h3>게시판 목록 조회</h3>
     * <p>커서 기반 페이지네이션으로 게시글 목록을 조회합니다.</p>
     * <p>회원은 블랙리스트 필터링이 적용됩니다.</p>
     * <p>첫 페이지 요청 시 Redis 캐시를 사용합니다.</p>
     *
     * @param cursor   마지막으로 조회한 게시글 ID (null이면 처음부터)
     * @param size     조회할 개수
     * @param memberId 회원 ID (null이면 비회원)
     * @return CursorPageResponse 커서 기반 페이지 응답
     */
    public CursorPageResponse<PostSimpleDetail> getBoardByCursor(Long cursor, int size, Long memberId) {
        List<PostSimpleDetail> posts;

        // 첫 페이지라면 캐시 조회 아니라면 DB 조회
        if (cursor == null) {
            posts = postCacheService.getFirstPagePosts();
        } else {
            posts = postQueryRepository.findBoardPostsByCursor(cursor, size);
        }

        // 다음 페이지가 있는지 판단
        boolean hasNext = posts.size() > size;
        if (hasNext) {
            posts = new ArrayList<>(posts.subList(0, size));
        }

        // 블랙리스트 필터링
        if (memberId != null && !posts.isEmpty()) {
            Set<Long> blacklistSet = new HashSet<>(postToMemberAdapter.getInterActionBlacklist(memberId));
            posts = posts.stream().filter(post -> !blacklistSet.contains(post.getMemberId())).collect(Collectors.toList());
        }

        Long nextCursor = hasNext && !posts.isEmpty() ? posts.getLast().getId() : null;
        return CursorPageResponse.of(posts, nextCursor);
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>DB에서 직접 조회합니다.</p>
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
        PostDetail result = postQueryRepository.findPostDetail(postId, memberId).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. 비동기로 실시간 인기글 점수, 조회 수 증가
        realtimePostSync.postDetailCheck(postId, viewerKey);

        // 3. 비회원이면 바로 반환
        if (memberId == null) {
            return result;
        }

        // 4. 회원 블랙리스트 체크
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
        Page<PostSimpleDetail> writePosts = postQueryRepository.findPostsByMemberId(memberId, pageable);
        Page<PostSimpleDetail> likedPosts = postQueryRepository.findLikedPostsByMemberId(memberId, pageable);
        return new MemberActivityPost(writePosts, likedPosts);
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
}
