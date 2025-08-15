package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentCommandService implements CommentCommandUseCase {

    private final LoadPostPort loadPostPort;
    private final LoadUserPort loadUserPort;
    private final CommentService commentService;
    private final ApplicationEventPublisher eventPublisher;
    private final CommentLikeCommandPort commentLikeCommandPort;
    private final CommentCommandPort commentCommandPort;
    private final CommentQueryPort commentQueryPort;


    @Override
    public void writeComment(CustomUserDetails userDetails, CommentDTO commentDto) {
        Post post = loadPostPort.findById(commentDto.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        User user = null;
        if (userDetails != null) {
            user = loadUserPort.findById(userDetails.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        }

        commentService.saveCommentWithClosure(
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
        commentCommandPort.save(comment);
    }

    @Override
    public void deleteComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDto, userDetails);
        commentService.deleteComment(comment);
    }

    @Override
    public void likeComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Long commentId = commentDto.getId();
        Long userId = userDetails.getUserId();

        Comment comment = commentQueryPort.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (commentQueryPort.isLikedByUser(commentId, userId)) {
            commentCommandPort.deleteLike(comment, user);
        } else {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeCommandPort.save(commentLike);
        }
    }

    /**
     * <h3>댓글 유효성 검사 및 조회</h3>
     * <p>댓글 DTO와 사용자 인증 정보를 기반으로 댓글의 유효성을 검사하고 댓글 엔티티를 조회합니다.</p>
     * <p>비밀번호가 일치하지 않거나, 사용자 본인이 아닌 경우 예외를 발생시킵니다.</p>
     *
     * @param commentDto  댓글 DTO
     * @param userDetails 사용자 인증 정보
     * @return Comment 유효성 검사를 통과한 댓글 엔티티
     * @throws CustomException 댓글을 찾을 수 없거나, 비밀번호가 일치하지 않거나, 사용자 권한이 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private Comment validateComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Comment comment = commentQueryPort.findById(commentDto.getId())
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
}
