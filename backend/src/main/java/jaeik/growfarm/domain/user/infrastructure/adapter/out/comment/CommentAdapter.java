package jaeik.growfarm.domain.user.infrastructure.adapter.out.comment;

import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.user.application.port.out.LoadCommentPort;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * <h2>댓글 조회 어댑터</h2>
 * <p>User 도메인에서 Comment 도메인의 In-Port를 통해 접근하는 어댑터</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class CommentAdapter implements LoadCommentPort {

    private final CommentQueryUseCase commentQueryUseCase;

    @Override
    public Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable) {
        return commentQueryUseCase.getUserComments(userId, pageable);
    }

    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        return commentQueryUseCase.getUserLikedComments(userId, pageable);
    }
}