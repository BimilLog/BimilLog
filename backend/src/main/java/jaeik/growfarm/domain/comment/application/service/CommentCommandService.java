package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentClosure;
import jaeik.growfarm.domain.comment.entity.CommentRequest;
import jaeik.growfarm.domain.comment.event.CommentCreatedEvent;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentCommandService implements CommentCommandUseCase {

    private final ApplicationEventPublisher eventPublisher;
    private final LoadPostPort loadPostPort;
    private final LoadUserPort loadUserPort;
    private final CommentCommandPort commentCommandPort;
    private final CommentQueryPort commentQueryPort;
    private final CommentClosureQueryPort commentClosureQueryPort;
    private final CommentClosureCommandPort commentClosureCommandPort;

    @Override
    public void writeComment(Long userId, CommentRequest commentRequest) {
        Post post = loadPostPort.findById(commentRequest.postId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        User user = null;
        String userName = "익명";
        if (userId != null) {
            user = loadUserPort.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            userName = user.getUserName();
        }

        saveCommentWithClosure(
                post,
                user,
                commentRequest.content(),
                commentRequest.password(),
                commentRequest.parentId());

        if (post.getUser() != null) {
            eventPublisher.publishEvent(new CommentCreatedEvent(
                    this,
                    post.getUser().getId(),
                    userName,
                    commentRequest.postId()));
        }
    }

    @Override
    public void updateComment(Long userId, CommentRequest commentRequest) {
        Comment comment = validateComment(commentRequest, userId);
        comment.updateComment(commentRequest.content());
        commentCommandPort.save(comment);
    }

    @Override
    public void deleteComment(Long userId, CommentRequest commentRequest) {
        Comment comment = validateComment(commentRequest, userId);
        Long commentId = comment.getId();
        try {
            boolean wasHardDeleted = commentCommandPort.deleteCommentOptimized(commentId);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_DELETE_FAILED, e);
        }
    }


    /**
     * <h3>댓글 유효성 검사 및 조회</h3>
     * <p>댓글 요청과 사용자 ID를 기반으로 댓글의 유효성을 검사하고 댓글 엔티티를 조회합니다.</p>
     * <p>비밀번호가 일치하지 않거나, 사용자 본인이 아닌 경우 예외를 발생시킵니다.</p>
     *
     * @param commentRequest  댓글 요청 (비밀번호 포함)
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @return Comment 유효성 검사를 통과한 댓글 엔티티
     * @throws CustomException 댓글을 찾을 수 없거나, 비밀번호가 일치하지 않거나, 사용자 권한이 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private Comment validateComment(CommentRequest commentRequest, Long userId) {
        Comment comment = commentQueryPort.findById(commentRequest.id())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (commentRequest.password() != null && !Objects.equals(comment.getPassword(), commentRequest.password())) {
            throw new CustomException(ErrorCode.COMMENT_PASSWORD_NOT_MATCH);
        }

        if (commentRequest.password() == null
                && (userId == null 
                    || comment.getUser() == null 
                    || !Objects.equals(comment.getUser().getId(), userId))) {
            throw new CustomException(ErrorCode.ONLY_COMMENT_OWNER_UPDATE);
        }
        return comment;
    }

    /**
     * <h3>댓글과 클로저 엔티티 함께 저장 (배치 최적화)</h3>
     * <p>새로운 댓글을 저장하고 댓글의 계층 구조를 관리하는 클로저 엔티티를 함께 저장합니다.</p>
     * <p>부모 댓글이 있는 경우 해당 댓글의 모든 상위 클로저 엔티티와 새로운 댓글을 연결합니다.</p>
     * <p>성능 최적화: 클로저 엔티티들을 배치 저장으로 처리하여 N번의 INSERT를 1번으로 최적화합니다.</p>
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

            // 클로저 엔티티들을 배치로 저장하기 위한 리스트
            List<CommentClosure> closuresToSave = new ArrayList<>();
            
            // 자기 자신에 대한 클로저 추가 (depth = 0)
            CommentClosure selfClosure = CommentClosure.createCommentClosure(comment, comment, 0);
            closuresToSave.add(selfClosure);

            // 부모 댓글이 있는 경우 부모의 모든 조상과의 클로저 생성
            if (parentId != null) {
                Comment parentComment = commentQueryPort.findById(parentId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));
                List<CommentClosure> parentClosures = commentClosureQueryPort.findByDescendantId(parentComment.getId())
                        .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));

                for (CommentClosure parentClosure : parentClosures) {
                    Comment ancestor = parentClosure.getAncestor();
                    int newDepth = parentClosure.getDepth() + 1;
                    CommentClosure newClosure = CommentClosure.createCommentClosure(ancestor, comment, newDepth);
                    closuresToSave.add(newClosure);
                }
            }
            commentClosureCommandPort.saveAll(closuresToSave);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_WRITE_FAILED, e);
        }
    }

}
