package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.out.GlobalMemberBlacklistAdapter;
import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.global.out.GlobalPostQueryAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.domain.post.event.PostUnlikeEvent;
import jaeik.bimillog.domain.post.out.PostLikeRepository;
import jaeik.bimillog.domain.post.out.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>PostInteractionService</h2>
 * <p>게시글 상호작용 서비스</p>
 * <p>좋아요 토글과 조회수 증가 등</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostInteractionService {

    private final PostRepository postRepository;
    private final GlobalPostQueryAdapter globalPostQueryAdapter;
    private final PostLikeRepository postLikeRepository;
    private final GlobalMemberQueryAdapter globalMemberQueryAdapter;
    private final ApplicationEventPublisher eventPublisher;
    private final GlobalMemberBlacklistAdapter globalMemberBlacklistAdapter;

    /**
     * <h3>게시글 좋아요 토글 비즈니스 로직 실행</h3>
     * <p>사용자별 좋아요 상태 토글 규칙을 적용합니다.</p>
     * <p>이미 좋아요한 게시글인 경우 좋아요를 취소하고, 좋아요하지 않은 게시글인 경우 좋아요를 추가합니다.</p>
     *
     * @param memberId 현재 로그인한 사용자 ID
     * @param postId 좋아요 대상 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void likePost(Long memberId, Long postId) {
        // 1. ID 기반으로 좋아요 존재 여부 확인 (엔티티 로딩 최소화)
        boolean isAlreadyLiked = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
        
        // 2. 좋아요 토글을 위해 필요한 엔티티만 로딩
        Member member = globalMemberQueryAdapter.getReferenceById(memberId);
        Post post = globalPostQueryAdapter.findById(postId);

        // 블랙리스트 확인 (익명 게시글이 아닌 경우에만)
        if (post.getMember() != null) {
            globalMemberBlacklistAdapter.checkMemberBlacklist(memberId, post.getMember().getId());
        }

        if (isAlreadyLiked) {
            postLikeRepository.deleteByMemberAndPost(member, post);

            // 실시간 인기글 점수 감소 이벤트 발행
            eventPublisher.publishEvent(new PostUnlikeEvent(postId));
        } else {
            PostLike postLike = PostLike.builder().member(member).post(post).build();
            postLikeRepository.save(postLike);

            // 실시간 인기글 점수 증가 및 상호작용 점수 증가 이벤트 발행
            Long postAuthorId = post.getMember() != null ? post.getMember().getId() : null;
            eventPublisher.publishEvent(new PostLikeEvent(postId, postAuthorId, memberId));
        }
    }

    /**
     * <h3>게시글 조회수 증가 비즈니스 로직 실행</h3>
     * <p>PostQueryController에서 게시글 상세 조회 완료 후 호출됩니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void incrementViewCount(Long postId) {
        postRepository.incrementViewsByPostId(postId);
        log.debug("게시글 조회수 증가됨: postId={}", postId);
    }
}