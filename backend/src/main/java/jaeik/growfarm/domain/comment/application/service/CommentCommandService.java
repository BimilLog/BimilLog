package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentClosure;
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

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentCommandService implements CommentCommandUseCase {

    private final LoadPostPort loadPostPort;
    private final LoadUserPort loadUserPort;
    private final ApplicationEventPublisher eventPublisher;
    private final CommentLikeCommandPort commentLikeCommandPort;
    private final CommentCommandPort commentCommandPort;
    private final CommentQueryPort commentQueryPort;
    private final CommentClosureQueryPort commentClosureQueryPort;
    private final CommentClosureCommandPort commentClosureCommandPort;



    @Override
    public void writeComment(CustomUserDetails userDetails, CommentDTO commentDto) {
        Post post = loadPostPort.findById(commentDto.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        User user = null;
        if (userDetails != null) {
            user = loadUserPort.findById(userDetails.getUserId())
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

    @Override
    public void updateComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDto, userDetails);
        comment.updateComment(commentDto.getContent());
        commentCommandPort.save(comment);
    }

    @Override
    public void deleteComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDto, userDetails);
        Long commentId = comment.getId();
        try {
            boolean hasDescendants = commentClosureQueryPort.hasDescendants(commentId);
            if (hasDescendants) {
                comment.softDelete();
                commentCommandPort.save(comment);
            } else {
                commentClosureCommandPort.deleteByDescendantId(commentId);
                commentCommandPort.delete(comment);
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_DELETE_FAILED, e);
        }
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
            commentLikeCommandPort.deleteLike(comment, user);
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

    /**
     * <h3>댓글과 클로저 엔티티 함께 저장</h3>
     * <p>새로운 댓글을 저장하고 댓글의 계층 구조를 관리하는 클로저 엔티티를 함께 저장합니다.</p>
     * <p>부모 댓글이 있는 경우 해당 댓글의 모든 상위 클로저 엔티티와 새로운 댓글을 연결합니다.</p>
     *
     * @param post     댓글이 속한 게시글 엔티티
     * @param user     댓글 작성 사용자 엔티티
     * @param content  댓글 내용
     * @param password 댓글 비밀번호 (선택 사항)
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    private void saveCommentWithClosure(Post post, User user, String content, Integer password, Long parentId) {
        try {
            Comment comment = commentCommandPort.save(Comment.createComment(post, user, content, password));

            CommentClosure selfClosure = CommentClosure.createCommentClosure(comment, comment, 0);
            commentClosureCommandPort.save(selfClosure);

            if (parentId != null) {
                Comment parentComment = commentQueryPort.findById(parentId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));
                List<CommentClosure> parentClosures = commentClosureQueryPort.findByDescendantId(parentComment.getId())
                        .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));

                for (CommentClosure parentClosure : parentClosures) {
                    Comment ancestor = parentClosure.getAncestor();
                    int newDepth = parentClosure.getDepth() + 1;
                    CommentClosure newClosure = CommentClosure.createCommentClosure(ancestor, comment, newDepth);
                    commentClosureCommandPort.save(newClosure);
                }
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_WRITE_FAILED, e);
        }
    }
}
