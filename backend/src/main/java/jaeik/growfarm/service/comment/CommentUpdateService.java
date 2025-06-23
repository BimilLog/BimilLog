package jaeik.growfarm.service.comment;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.CommentClosure;
import jaeik.growfarm.entity.comment.CommentLike;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentClosureRepository;
import jaeik.growfarm.repository.comment.CommentLikeRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 업데이트 서비스</h2>
 * <p>
 * 댓글의 DB작업을 처리하는 서비스
 * </p>
 *
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CommentUpdateService {

    private final CommentRepository commentRepository;
    private final CommentClosureRepository commentClosureRepository;
    private final CommentLikeRepository commentLikeRepository;

    /**
     * <h3>댓글 작성</h3>
     * <p>댓글과 댓글 클로저 테이블에 댓글을 저장한다</p>
     *
     * @param post     댓글이 달릴 게시글
     * @param user     댓글 작성자 (비로그인 시 null)
     * @param content  댓글 내용
     * @param password 댓글 비밀번호 (선택적)
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void saveCommentWithClosure(Post post, Users user, String content, Integer password, Long parentId) {
        try {
            Comment comment = commentRepository.save(Comment.createComment(post, user, content, password));

            CommentClosure selfClosure = CommentClosure.createCommentClosure(comment, comment, 0);
            commentClosureRepository.save(selfClosure);

            if (parentId != null) {
                Comment parentComment = commentRepository.getReferenceById(parentId);
                List<CommentClosure> parentClosures = commentClosureRepository.findByDescendantId(parentComment.getId())
                        .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));

                for (CommentClosure parentClosure : parentClosures) {
                    Comment ancestor = parentClosure.getAncestor();
                    int newDepth = parentClosure.getDepth() + 1;
                    CommentClosure newClosure = CommentClosure.createCommentClosure(ancestor, comment, newDepth);
                    commentClosureRepository.save(newClosure);
                }
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_WRITE_FAILED, e);
        }
    }

    /**
     * <h3>댓글 수정</h3>
     * <p>댓글 내용을 수정한다.</p>
     *
     * @param commentDTO 댓글 DTO
     * @param comment    수정할 댓글 엔티티
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void commentUpdate(CommentDTO commentDTO, Comment comment) {
        comment.updateComment(commentDTO.getContent());
    }

    /**
     * <h3>댓글 삭제</h3>
     * <p>해당 댓글에 대댓글이 존재하지 않을 때 댓글 자체를 테이블에서 삭제한다.</p>
     *
     * @param commentId 댓글 ID
     * @param comment   수정할 내용
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void hardDelete(Long commentId, Comment comment) {
        commentClosureRepository.deleteByDescendantId(commentId);
        commentRepository.delete(comment);
    }





    /**
     * <h3>댓글 추천 / 추천 취소</h3>
     * <p>댓글 추천 테이블에 엔티티가 이미 존재할 때는 추천 취소를 한다.</p>
     * <p>존재하지 않을 때는 추천을 추가한다.</p>
     *
     * @param existingLike Optional<CommentLike> 댓글 추천 엔티티
     * @param comment      댓글 엔티티
     * @param user         추천을 누른 사용자 엔티티
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void saveCommentLike(Optional<CommentLike> existingLike, Comment comment, Users user) {
        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
        } else {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeRepository.save(commentLike);
        }
    }
}
