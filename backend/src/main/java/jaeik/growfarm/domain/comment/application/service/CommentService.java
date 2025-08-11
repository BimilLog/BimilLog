package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.post.application.port.out.LoadPostPort;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.dto.comment.CommentDto;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.CommentClosure;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.Tuple;
import java.util.*;
import jaeik.growfarm.entity.comment.CommentLike;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentService implements CommentCommandUseCase, CommentQueryUseCase {

    private final LoadCommentPort loadCommentPort;
    private final SaveCommentPort saveCommentPort;
    private final DeleteCommentPort deleteCommentPort;
    private final LoadCommentLikePort loadCommentLikePort;
    private final SaveCommentLikePort saveCommentLikePort;
    private final DeleteCommentLikePort deleteCommentLikePort;
    private final LoadCommentClosurePort loadCommentClosurePort;
    private final SaveCommentClosurePort saveCommentClosurePort;
    private final DeleteCommentClosurePort deleteCommentClosurePort;
    private final ApplicationEventPublisher eventPublisher;
    private final UserQueryUseCase userQueryUseCase;
    private final LoadPostPort loadPostPort;


    // ============== Query ==============

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getPopularComments(Long postId, CustomUserDetails userDetails) {
        try {
            List<Tuple> popularTuples = loadCommentPort.findPopularComments(postId);

            if (popularTuples.isEmpty()) {
                return List.of();
            }

            List<Long> popularCommentIds = popularTuples.stream()
                    .map(tuple -> {
                        Comment comment = tuple.get(QComment.comment);
                        return comment != null ? comment.getId() : null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            List<Long> userLikedCommentIds = getUserLikedCommentIds(popularCommentIds, userDetails);

            List<CommentDto> popularComments = new ArrayList<>();
            for (Tuple tuple : popularTuples) {
                Comment comment = tuple.get(QComment.comment);
                if (comment == null)
                    continue;

                try {
                    CommentDto commentDTO = new CommentDto(comment);

                    Long likeCount = tuple.get(1, Long.class);
                    commentDTO.setLikes(likeCount != null ? likeCount.intValue() : 0);

                    commentDTO.setUserLike(userLikedCommentIds.contains(comment.getId()));

                    Long parentId = tuple.get(2, Long.class);
                    commentDTO.setParentId(parentId);

                    commentDTO.setPopular(true);

                    popularComments.add(commentDTO);
                } catch (Exception e) {
                    throw new CustomException(ErrorCode.POPULAR_COMMENT_FAILED, e);
                }
            }

            return popularComments;
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDto> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails) {
        if (page < 0) {
            return Page.empty();
        }

        Pageable pageable = Pageable.ofSize(20).withPage(page);

        try {
            List<Tuple> commentTuples = loadCommentPort.findCommentsWithLatestOrder(postId, pageable);

            if (commentTuples.isEmpty()) {
                return Page.empty(pageable);
            }

            List<Long> commentIds = commentTuples.stream()
                    .map(tuple -> {
                        Comment comment = tuple.get(QComment.comment);
                        return comment != null ? comment.getId() : null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            List<Long> userLikedCommentIds = getUserLikedCommentIds(commentIds, userDetails);

            Map<Long, CommentDto> commentMap = new HashMap<>();
            List<CommentDto> rootComments = new ArrayList<>();

            for (Tuple tuple : commentTuples) {
                Comment comment = tuple.get(QComment.comment);
                if (comment == null)
                    continue;

                Long commentId = comment.getId();
                if (commentMap.containsKey(commentId))
                    continue;

                try {
                    CommentDto dto = new CommentDto(comment);

                    Long likeCount = tuple.get(1, Long.class);
                    dto.setLikes(likeCount != null ? likeCount.intValue() : 0);

                    dto.setUserLike(userLikedCommentIds.contains(commentId));

                    Long parentId = tuple.get(4, Long.class);
                    dto.setParentId(parentId);

                    commentMap.put(commentId, dto);

                    Integer depth = tuple.get(3, Integer.class);
                    if (depth != null && depth == 0) {
                        rootComments.add(dto);
                    }
                } catch (Exception e) {
                    throw new CustomException(ErrorCode.COMMENT_FAILED, e);
                }
            }

            Long totalCount = loadCommentPort.countRootCommentsByPostId(postId);

            return new PageImpl<>(rootComments, pageable, totalCount != null ? totalCount : 0L);

        } catch (Exception e) {
            return Page.empty(pageable);
        }
    }


    // ============== Command ==============

    @Override
    public void writeComment(CustomUserDetails userDetails, CommentDto commentDto) {
        Post post = loadPostPort.findById(commentDto.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        
        Users user = null;
        if (userDetails != null) {
            user = userQueryUseCase.findById(userDetails.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        }

        saveCommentWithClosure(
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

    private void saveCommentWithClosure(Post post, Users user, String content, Integer password, Long parentId) {
        try {
            Comment comment = saveCommentPort.save(Comment.createComment(post, user, content, password));

            CommentClosure selfClosure = CommentClosure.createCommentClosure(comment, comment, 0);
            saveCommentClosurePort.save(selfClosure);

            if (parentId != null) {
                Comment parentComment = loadCommentPort.findById(parentId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));
                List<CommentClosure> parentClosures = loadCommentClosurePort.findByDescendantId(parentComment.getId())
                        .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));

                for (CommentClosure parentClosure : parentClosures) {
                    Comment ancestor = parentClosure.getAncestor();
                    int newDepth = parentClosure.getDepth() + 1;
                    CommentClosure newClosure = CommentClosure.createCommentClosure(ancestor, comment, newDepth);
                    saveCommentClosurePort.save(newClosure);
                }
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_WRITE_FAILED, e);
        }
    }

    @Override
    public void updateComment(CommentDto commentDto, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDto, userDetails);
        comment.updateComment(commentDto.getContent());
        saveCommentPort.save(comment);
    }

    private Comment validateComment(CommentDto commentDto, CustomUserDetails userDetails) {
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
    public void deleteComment(CommentDto commentDto, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDto, userDetails);
        Long commentId = commentDto.getId();

        try {
            boolean hasDescendants = loadCommentClosurePort.hasDescendants(commentId);
            if (hasDescendants) {
                comment.softDelete();
                saveCommentPort.save(comment);
            } else {
                deleteCommentClosurePort.deleteByDescendantId(commentId);
                deleteCommentPort.delete(comment);
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_DELETE_FAILED, e);
        }
    }

    @Override
    public void likeComment(CommentDto commentDto, CustomUserDetails userDetails) {
        Long commentId = commentDto.getId();
        Long userId = userDetails.getUserId();

        // TODO: Port로 변경해야 함
        Comment comment = loadCommentPort.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        Users user = userQueryUseCase.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Optional<CommentLike> existingLike = loadCommentLikePort.findByCommentIdAndUserId(commentId, userId);

        if (existingLike.isPresent()) {
            deleteCommentLikePort.delete(existingLike.get());
        } else {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            saveCommentLikePort.save(commentLike);
        }
    }

    @Override
    public void anonymizeUserComments(Long userId) {
        saveCommentPort.anonymizeUserComments(userId);
    }

    private List<Long> getUserLikedCommentIds(List<Long> commentIds, CustomUserDetails userDetails) {
        return (userDetails != null)
                ? loadCommentPort.findUserLikedCommentIds(commentIds, userDetails.getUserId())
                : List.of();
    }
}
