package jaeik.bimillog.domain.comment.application.service;

import jaeik.bimillog.domain.comment.application.port.in.CommentLikeUseCase;
import jaeik.bimillog.domain.comment.application.port.out.CommentLikePort;
import jaeik.bimillog.domain.comment.application.port.out.CommentQueryPort;
import jaeik.bimillog.domain.comment.application.port.out.CommentToUserPort;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>댓글 추천 서비스</h2>
 * <p>
 * 댓글 추천 관련 기능을 구현하는 서비스 클래스
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 댓글 추천 관련 비즈니스 로직을 처리하는 비즈니스 로직 구현
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentLikeService implements CommentLikeUseCase {

    private final CommentToUserPort commentToUserPort;
    private final CommentLikePort commentLikePort;
    private final CommentQueryPort commentQueryPort;



    /**
     * <h3>댓글 추천/취소</h3>
     * <p>사용자가 댓글에 추천을 누르거나 취소합니다.</p>
     * <p>이미 추천한 댓글이면 취소하고, 추천하지 않은 댓글이면 추천을 추가합니다.</p>
     *
     * @param userId 사용자 ID (로그인한 경우), null인 경우 예외 발생
     * @param commentId 추천/취소할 댓글 ID
     * @throws CustomException 사용자나 댓글이 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void likeComment(Long userId, Long commentId) {
        Comment comment = commentQueryPort.findById(commentId);
        User user = commentToUserPort.findById(userId);

        if (commentLikePort.isLikedByUser(commentId, userId)) {
            commentLikePort.deleteLikeByIds(commentId, userId);
        } else {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikePort.save(commentLike);
        }
    }
}
