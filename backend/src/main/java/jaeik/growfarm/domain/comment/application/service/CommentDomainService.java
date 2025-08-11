
package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.out.DeleteCommentClosurePort;
import jaeik.growfarm.domain.comment.application.port.out.DeleteCommentPort;
import jaeik.growfarm.domain.comment.application.port.out.LoadCommentClosurePort;
import jaeik.growfarm.domain.comment.application.port.out.LoadCommentPort;
import jaeik.growfarm.domain.comment.application.port.out.SaveCommentClosurePort;
import jaeik.growfarm.domain.comment.application.port.out.SaveCommentPort;
import jaeik.growfarm.domain.comment.domain.Comment;
import jaeik.growfarm.domain.comment.domain.CommentClosure;
import jaeik.growfarm.domain.post.domain.Post;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentDomainService {

    private final LoadCommentPort loadCommentPort;
    private final SaveCommentPort saveCommentPort;
    private final DeleteCommentPort deleteCommentPort;
    private final LoadCommentClosurePort loadCommentClosurePort;
    private final SaveCommentClosurePort saveCommentClosurePort;
    private final DeleteCommentClosurePort deleteCommentClosurePort;

    public void saveCommentWithClosure(Post post, User user, String content, Integer password, Long parentId) {
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

    public void deleteComment(Comment comment) {
        Long commentId = comment.getId();
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
}
