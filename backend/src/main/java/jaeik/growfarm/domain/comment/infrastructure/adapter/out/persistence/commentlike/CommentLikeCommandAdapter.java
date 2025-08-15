package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence.commentlike;

import jaeik.growfarm.domain.comment.application.port.out.CommentLikeCommandPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import jaeik.growfarm.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class CommentLikeCommandAdapter implements CommentLikeCommandPort {

    private final CommentLikeRepository commentLikeRepository;


    /**
     * <h3>댓글 추천 저장</h3>
     * <p>주어진 댓글 추천 엔티티를 저장합니다.</p>
     *
     * @param commentLike 저장할 댓글 추천 엔티티
     * @return CommentLike 저장된 댓글 추천 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public CommentLike save(CommentLike commentLike) {
        return commentLikeRepository.save(commentLike);
    }


    /**
     * <h3>댓글 추천 삭제</h3>
     * <p>주어진 댓글과 사용자의 추천 관계를 삭제합니다.</p>
     *
     * @param comment 추천를 삭제할 댓글 엔티티
     * @param user    추천를 삭제할 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    @Override
    public void deleteLike(Comment comment, User user) {
        commentLikeRepository.deleteByCommentAndUser(comment, user);
    }
}
