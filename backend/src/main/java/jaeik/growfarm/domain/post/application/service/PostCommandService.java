package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.in.PostCommandUseCase;
import jaeik.growfarm.domain.post.application.port.out.*;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.User;
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
 *     PostCommandUseCase의 구현체로, 게시글 작성, 수정, 삭제 등의 명령형 비즈니스 로직을 처리합니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
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
    private final LoadUserPort loadUserPort;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * <h3>게시글 작성</h3>
     * <p>사용자가 작성한 게시글을 저장하고, 해당 게시글의 ID를 반환합니다.</p>
     *
     * @param userId      게시글 작성자의 사용자 ID
     * @param postReqDTO  게시글 요청 DTO
     * @return 저장된 게시글의 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Long writePost(Long userId, PostReqDTO postReqDTO) {
        User user = loadUserPort.getReferenceById(userId);
        Post newPost = Post.createPost(user, postReqDTO);
        Post savedPost = savePostPort.save(newPost);
        return savedPost.getId();
    }

    /**
     * <h3>게시글 공지 설정</h3>
     * <p>관리자 권한으로 특정 게시글을 공지로 설정합니다.</p>
     *
     * @param postId 공지로 설정할 게시글 ID
     * @since 2.0.0
     * @author Jaeik
     */
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
    public void updatePost(Long userId, Long postId, PostReqDTO postReqDTO) {
        Post post = validatePostOwner(userId, postId);
        post.updatePost(postReqDTO);
        savePostPort.save(post);
    }

    @Override
    public void deletePost(Long userId, Long postId) {
        Post post = validatePostOwner(userId, postId);
        deletePostPort.delete(post);
        eventPublisher.publishEvent(new PostDeletedEvent(postId));
    }

    @Override
    public void likePost(Long userId, Long postId) {
        User user = loadUserPort.getReferenceById(userId);
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

    private Post validatePostOwner(Long userId, Long postId) {
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!Objects.equals(post.getUser().getId(), userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return post;
    }

    @Async
    @Transactional
    @EventListener
    public void handlePostDeletedEvent(PostDeletedEvent event) {
        log.info("Post (ID: {}) deleted event received. Deleting all post likes.", event.postId());
        deletePostLikePort.deleteAllByPostId(event.postId());
    }
}
