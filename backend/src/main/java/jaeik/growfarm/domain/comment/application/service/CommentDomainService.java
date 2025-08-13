
package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.out.DeleteCommentClosurePort;
import jaeik.growfarm.domain.comment.application.port.out.DeleteCommentPort;
import jaeik.growfarm.domain.comment.application.port.out.LoadCommentClosurePort;
import jaeik.growfarm.domain.comment.application.port.out.LoadCommentPort;
import jaeik.growfarm.domain.comment.application.port.out.SaveCommentClosurePort;
import jaeik.growfarm.domain.comment.application.port.out.SaveCommentPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentClosure;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>댓글 도메인 서비스</h2>
 * <p>
 * 댓글의 생성, 삭제 등 핵심 비즈니스 로직을 담당하는 도메인 서비스
 * </p>
 * <p>
 * 댓글 계층 구조 관리 및 연관된 클로저 엔티티 처리를 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
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

    /**
     * <h3>댓글 삭제</h3>
     * <p>주어진 댓글을 삭제합니다. 대댓글이 있는 경우 소프트 삭제를 수행하고, 없는 경우 물리적으로 삭제합니다.</p>
     *
     * @param comment 삭제할 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
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
