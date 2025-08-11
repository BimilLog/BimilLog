package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.in.PostCommandUseCase;
import jaeik.growfarm.domain.post.application.port.out.*;
import jaeik.growfarm.domain.post.domain.Post;
import jaeik.growfarm.domain.post.domain.PostLike;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.global.event.PostDeletedEvent;
import jaeik.growfarm.global.event.PostSetAsNoticeEvent;
import jaeik.growfarm.global.event.PostUnsetAsNoticeEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * <h2>PostCommandService</h2>
 * <p>
 *     PostCommandUseCase의 구현체입니다.
 *     게시글 생성, 수정, 삭제 등 CUD 관련 비즈니스 로직을 처리합니다.
 * </p>
 *
 * @author jaeik
 * @version 1.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostCommandService implements PostCommandUseCase {

    private final SavePostPort savePostPort;
    private final LoadPostPort loadPostPort;
    private final DeletePostPort deletePostPort;
    private final SavePostLikePort savePostLikePort;
    private final DeletePostLikePort deletePostLikePort;
    private final ExistPostLikePort existPostLikePort;
    private final ApplicationEventPublisher eventPublisher;



    @Override
    public Long writePost(User user, PostReqDTO postReqDTO) {
        Post newPost = Post.createPost(user, postReqDTO);
        Post savedPost = savePostPort.save(newPost);
        return savedPost.getId();
    }

    @Override
    public void setPostAsNotice(Long postId) {
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.setAsNotice();
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId));
    }

    @Override
    public void unsetPostAsNotice(Long postId) {
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.unsetAsNotice();
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId));
    }

    @Override
    public void updatePost(User user, Long postId, PostReqDTO postReqDTO) {
        Post post = validatePostOwner(user, postId);
        post.updatePost(postReqDTO);
        savePostPort.save(post);
    }

    @Override
    public void deletePost(User user, Long postId) {
        Post post = validatePostOwner(user, postId);
        deletePostPort.delete(post);
        eventPublisher.publishEvent(new PostDeletedEvent(postId));
    }

    @Override
    public void likePost(User user, Long postId) {
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (existPostLikePort.existsByUserAndPost(user, post)) {
            deletePostLikePort.deleteByUserAndPost(user, post);
        } else {
            PostLike postLike = PostLike.builder().user(user).post(post).build();
            savePostLikePort.save(postLike);
        }
    }

    @Override
    public void incrementViewCount(Long postId) {
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.incrementView();
        // savePostPort.save(post)는 @Transactional에 의해 더티 체킹되므로 명시적으로 호출할 필요가 없습니다.
    }

    private Post validatePostOwner(User user, Long postId) {
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!Objects.equals(post.getUser().getId(), user.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return post;
    }

    @Async
    @Transactional
    @EventListener
    public void handlePostDeletedEvent(PostDeletedEvent event) {
        log.info("Post (ID: {}) deleted event received. Deleting all post likes.", event.getPostId());
        deletePostLikePort.deleteAllByPostId(event.getPostId());
    }
}
