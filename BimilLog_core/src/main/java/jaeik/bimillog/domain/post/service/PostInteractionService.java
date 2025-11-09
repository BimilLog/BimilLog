package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.global.out.GlobalPostQueryAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.domain.post.event.PostUnlikeEvent;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.out.PostCommandAdapter;
import jaeik.bimillog.domain.post.out.PostLikeCommandAdapter;
import jaeik.bimillog.domain.post.out.PostLikeQueryAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>PostInteractionService</h2>
 * <p>
 * 게시글 상호작용 관련 UseCase 인터페이스의 구체적 구현체로서 사용자 참여 비즈니스 로직을 오케스트레이션합니다.
 * </p>
 * <p>
 * 게시글 도메인의 사용자 상호작용 처리를 담당하며, 좋아요 토글과 조회수 증가 등
 * 사용자 참여를 통한 게시글 활성도 측정과 인기도 산정을 위한 비즈니스 규칙을 관리합니다.
 * </p>
 * <p>
 * 트랜잭션 경계를 설정하여 좋아요 상태 변경의 원자성을 보장하고, 조회수 증가를 통한 게시글 노출도 관리를 수행합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostInteractionService {

    private final PostCommandAdapter postCommandAdapter;
    private final GlobalPostQueryAdapter globalPostQueryAdapter;
    private final PostLikeCommandAdapter postLikeCommandAdapter;
    private final PostLikeQueryAdapter postLikeQueryAdapter;
    private final GlobalMemberQueryAdapter globalMemberQueryAdapter;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>게시글 좋아요 토글 비즈니스 로직 실행</h3>
     * <p>PostInteractionUseCase 인터페이스의 좋아요 기능을 구현하며, 사용자별 좋아요 상태 토글 규칙을 적용합니다.</p>
     * <p>이미 좋아요한 게시글인 경우 좋아요를 취소하고, 좋아요하지 않은 게시글인 경우 좋아요를 추가합니다.</p>
     * <p>PostCommandController에서 좋아요 토글 요청 시 호출됩니다.</p>
     *
     * @param memberId 현재 로그인한 사용자 ID
     * @param postId 좋아요 대상 게시글 ID
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void likePost(Long memberId, Long postId) {
        // 1. ID 기반으로 좋아요 존재 여부 확인 (엔티티 로딩 최소화)
        boolean isAlreadyLiked = postLikeQueryAdapter.existsByPostIdAndUserId(postId, memberId);
        
        // 2. 좋아요 토글을 위해 필요한 엔티티만 로딩
        Member member = globalMemberQueryAdapter.getReferenceById(memberId);
        Post post = globalPostQueryAdapter.findById(postId);

        if (isAlreadyLiked) {
            postLikeCommandAdapter.deletePostLike(member, post);

            // 실시간 인기글 점수 감소 이벤트 발행
            eventPublisher.publishEvent(new PostUnlikeEvent(postId));
        } else {
            PostLike postLike = PostLike.builder().member(member).post(post).build();
            postLikeCommandAdapter.savePostLike(postLike);

            // 실시간 인기글 점수 증가 이벤트 발행
            eventPublisher.publishEvent(new PostLikeEvent(postId));
        }
    }

    /**
     * <h3>게시글 조회수 증가 비즈니스 로직 실행</h3>
     * <p>PostInteractionUseCase 인터페이스의 조회수 증가 기능을 구현하며, 게시글 노출도 측정 규칙을 적용합니다.</p>
     * <p>게시글의 인기도 산정과 실시간 인기 게시글 선정을 위한 핵심 지표로 활용됩니다.</p>
     * <p>성능 최적화를 위해 별도 엔티티 조회 없이 직접 UPDATE 쿼리로 조회수를 증가시킵니다.</p>
     * <p>PostQueryController에서 게시글 상세 조회 완료 후 호출됩니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void incrementViewCount(Long postId) {
        postCommandAdapter.incrementViewByPostId(postId);
        log.debug("게시글 조회수 증가됨: postId={}", postId);
    }
}