package jaeik.growfarm.domain.comment.application.service;

import com.querydsl.core.Tuple;

import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.comment.domain.Comment;
import jaeik.growfarm.domain.comment.domain.CommentClosure;
import jaeik.growfarm.domain.comment.domain.CommentLike;
import jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence.CommentReadRepository;
import jaeik.growfarm.domain.post.domain.Post;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.global.event.PostDeletedEvent;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentService implements CommentCommandUseCase, CommentQueryUseCase {

    private final LoadCommentPort loadCommentPort;
    private final SaveCommentPort saveCommentPort;
    private final DeleteCommentPort deleteCommentPort;
    private final SaveCommentLikePort saveCommentLikePort;
    private final LoadCommentLikePort loadCommentLikePort;
    private final ApplicationEventPublisher eventPublisher;
    private final LoadUserPort loadUserPort;
    private final LoadPostPort loadPostPort;
    private final CommentReadRepository commentReadRepository;
    private final CommentDomainService commentDomainService;


    // ============== Query ==============

    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> getPopularComments(Long postId, CustomUserDetails userDetails) {
        List<Long> likedCommentIds = getUserLikedCommentIdsForPopular(postId, userDetails);
        List<CommentDTO> popularComments = commentReadRepository.findPopularComments(postId, likedCommentIds);

        if (!popularComments.isEmpty()) {
            List<Long> commentIds = popularComments.stream().map(CommentDTO::getId).collect(Collectors.toList());
            Map<Long, Long> likeCounts = loadCommentLikePort.countByCommentIds(commentIds);
            popularComments.forEach(comment -> comment.setLikes(likeCounts.getOrDefault(comment.getId(), 0L).intValue()));
        }
        return popularComments;
    }

    private List<Long> getUserLikedCommentIdsForPopular(Long postId, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return Collections.emptyList();
        }
        // 이 부분은 개선의 여지가 있습니다. 인기 댓글 ID를 먼저 가져오고, 그 ID들로 좋아요 여부를 확인하는 것이 더 효율적입니다.
        // 현재는 postId 전체 댓글에 대해 좋아요 여부를 확인하게 될 수 있습니다.
        return loadCommentPort.findUserLikedCommentIdsByPostId(postId, userDetails.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDTO> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails) {
        Pageable pageable = Pageable.ofSize(20).withPage(page);
        List<Long> likedCommentIds = getUserLikedCommentIdsByPage(postId, pageable, userDetails);
        Page<CommentDTO> commentPage = commentReadRepository.findCommentsWithLatestOrder(postId, pageable, likedCommentIds);

        if (commentPage.hasContent()) {
            List<Long> commentIds = commentPage.getContent().stream().map(CommentDTO::getId).collect(Collectors.toList());
            Map<Long, Long> likeCounts = loadCommentLikePort.countByCommentIds(commentIds);
            commentPage.getContent().forEach(comment -> comment.setLikes(likeCounts.getOrDefault(comment.getId(), 0L).intValue()));
        }
        return commentPage;
    }

    private List<Long> getUserLikedCommentIdsByPage(Long postId, Pageable pageable, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return Collections.emptyList();
        }
        // 이 또한 comment ID 목록을 먼저 가져온 후 좋아요 여부를 확인하는 것이 더 효율적입니다.
        return loadCommentPort.findUserLikedCommentIdsByPostId(postId, userDetails.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Comment> findById(Long commentId) {
        return loadCommentPort.findById(commentId);
    }


    // ============== Command ==============

    @Override
    public void writeComment(CustomUserDetails userDetails, CommentDTO commentDto) {
        Post post = loadPostPort.findById(commentDto.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        
        User user = null;
        if (userDetails != null) {
            user = loadUserPort.findById(userDetails.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        }

        commentDomainService.saveCommentWithClosure(
                post,
                user,
                commentDto.getContent(),
                commentDto.getPassword(),
                commentDto.getParentId());

        if (post.getUser() != null) {
            eventPublisher.publishEvent(new CommentCreatedEvent(
                    this,
                    post.getUser().getId(),
                    commentDto.getUserName(),
                    commentDto.getPostId()));
        }
    }

    @Override
    public void updateComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDto, userDetails);
        comment.updateComment(commentDto.getContent());
        saveCommentPort.save(comment);
    }

    private Comment validateComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Comment comment = loadCommentPort.findById(commentDto.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (commentDto.getPassword() != null && !Objects.equals(comment.getPassword(), commentDto.getPassword())) {
            throw new CustomException(ErrorCode.COMMENT_PASSWORD_NOT_MATCH);
        }

        if (commentDto.getPassword() == null
                && (userDetails == null || !Objects.equals(comment.getUser().getId(), userDetails.getUserId()))) {
            throw new CustomException(ErrorCode.ONLY_COMMENT_OWNER_UPDATE);
        }
        return comment;
    }

    @Override
    public void deleteComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDto, userDetails);
        commentDomainService.deleteComment(comment);
    }

    @Override
    public void likeComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Long commentId = commentDto.getId();
        Long userId = userDetails.getUserId();

        Comment comment = loadCommentPort.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (loadCommentPort.isLikedByUser(commentId, userId)) {
            deleteCommentPort.deleteLike(comment, user);
        } else {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            saveCommentLikePort.save(commentLike);
        }
    }

    public void anonymizeUserComments(Long userId) {
        saveCommentPort.anonymizeUserComments(userId);
    }

    private List<Long> getUserLikedCommentIds(List<Long> commentIds, CustomUserDetails userDetails) {
        return (userDetails != null)
                ? loadCommentPort.findUserLikedCommentIds(commentIds, userDetails.getUserId())
                : List.of();
    }

    @Async
    @Transactional
    @EventListener
    public void handleUserWithdrawnEvent(UserWithdrawnEvent event) {
        log.info("User (ID: {}) withdrawn event received. Anonymizing comments.", event.getUserId());
        anonymizeUserComments(event.getUserId());
    }

    @Async
    @Transactional
    @EventListener
    public void handlePostDeletedEvent(PostDeletedEvent event) {
        log.info("Post (ID: {}) deleted event received. Deleting all comments.", event.getPostId());
        deleteCommentPort.deleteAllByPostId(event.getPostId());
    }
}
