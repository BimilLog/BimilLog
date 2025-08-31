package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.in.CommentLikeUseCase;
import jaeik.growfarm.domain.comment.application.port.out.CommentLikePort;
import jaeik.growfarm.domain.comment.application.port.out.CommentQueryPort;
import jaeik.growfarm.domain.comment.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentLikeService implements CommentLikeUseCase {

    private final LoadUserPort loadUserPort;
    private final CommentLikePort commentLikePort;
    private final CommentLikePort CommentLikePort;
    private final CommentQueryPort commentQueryPort;



    @Override
    public void likeComment(Long userId, Long commentId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        boolean commentExists = commentQueryPort.findById(commentId).isPresent();
        if (!commentExists) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }

        boolean userExists = loadUserPort.findById(userId).isPresent();
        if (!userExists) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        if (CommentLikePort.isLikedByUser(commentId, userId)) {
            commentLikePort.deleteLikeByIds(commentId, userId);
        } else {
            Comment comment = commentQueryPort.findById(commentId).get();
            User user = loadUserPort.findById(userId).get();
            
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikePort.save(commentLike);
        }
    }
}
