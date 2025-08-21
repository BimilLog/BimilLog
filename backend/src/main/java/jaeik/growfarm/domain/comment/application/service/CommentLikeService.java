package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.in.CommentLikeUseCase;
import jaeik.growfarm.domain.comment.application.port.out.CommentLikeCommandPort;
import jaeik.growfarm.domain.comment.application.port.out.CommentLikeQueryPort;
import jaeik.growfarm.domain.comment.application.port.out.CommentQueryPort;
import jaeik.growfarm.domain.comment.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentReqDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
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
    private final CommentLikeCommandPort commentLikeCommandPort;
    private final CommentLikeQueryPort commentLikeQueryPort;
    private final CommentQueryPort commentQueryPort;



    @Override
    public void likeComment(CommentReqDTO commentReqDto, CustomUserDetails userDetails) {
        Long commentId = commentReqDto.getId();
        Long userId = userDetails.getUserId();

        Comment comment = commentQueryPort.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (commentLikeQueryPort.isLikedByUser(commentId, userId)) {
            commentLikeCommandPort.deleteLike(comment, user);
        } else {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeCommandPort.save(commentLike);
        }
    }
}
