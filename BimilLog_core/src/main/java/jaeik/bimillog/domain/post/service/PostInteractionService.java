package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.event.CheckBlacklistEvent;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.async.PostCountSync;
import jaeik.bimillog.domain.post.async.RealtimePostSync;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.jpa.PostLike;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.domain.post.repository.PostLikeRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * <h2>PostInteractionService</h2>
 * <p>게시글 상호작용 서비스</p>
 * <p>좋아요 토글 등</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostInteractionService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostToMemberAdapter postToMemberAdapter;
    private final ApplicationEventPublisher eventPublisher;
    private final RealtimePostSync realtimePostSync;
    private final PostCountSync postCountSync;

    /**
     * <h3>게시글 좋아요 토글 비즈니스 로직 실행</h3>
     * <p>사용자별 좋아요 상태 토글 규칙을 적용합니다.</p>
     * <p>이미 좋아요한 게시글인 경우 좋아요를 취소하고, 좋아요하지 않은 게시글인 경우 좋아요를 추가합니다.</p>
     *
     * @param memberId 현재 로그인한 사용자 ID
     * @param postId 좋아요 대상 게시글 ID
     */
    @Transactional
    public void likePost(Long memberId, Long postId) {
        // 1. ID 기반으로 좋아요 존재 여부 확인 (엔티티 로딩 최소화)
        boolean isAlreadyLiked = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);

        // 2. 좋아요 토글을 위해 필요한 엔티티만 로딩
        Member member = postToMemberAdapter.getMember(memberId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 블랙리스트 확인 (익명 게시글이 아닌 경우에만)
        if (post.getMember() != null) {
            eventPublisher.publishEvent(new CheckBlacklistEvent(memberId, post.getMember().getId()));
        }

        if (isAlreadyLiked) {
            postLikeRepository.deleteByMemberAndPost(member, post);

            // 좋아요 수 DB 직접 반영
            postRepository.decrementLikeCount(postId);

            // 카운터 캐시 감소
            postCountSync.incrementLikeCounter(postId, -1);

            // 비동기로 실시간 인기글 점수 감소
            realtimePostSync.updateRealtimeScore(postId, -4.0);
        } else {
            PostLike postLike = PostLike.builder().member(member).post(post).build();
            postLikeRepository.save(postLike);
            postRepository.incrementLikeCount(postId); // 좋아요 수 DB 직접 반영
            postCountSync.incrementLikeCounter(postId, 1); // 비동기로 카운터 캐시 증가
            realtimePostSync.updateRealtimeScore(postId, 4.0); // 비동기로 실시간 인기글 점수 증가

            // 상호작용 점수 증가 이벤트 발행
            if (post.getMember() != null) {
                Long postAuthorId = post.getMember().getId();
                if (!Objects.equals(postAuthorId, memberId)) {
                    eventPublisher.publishEvent(new PostLikeEvent(postId, postAuthorId, memberId));
                }
            }
        }
    }

}
