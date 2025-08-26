package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentRequest;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentCommandService implements CommentCommandUseCase {

    private final CommentCommandPort commentCommandPort;
    private final CommentQueryPort commentQueryPort;
    private final CommentClosureQueryPort commentClosureQueryPort;
    private final CommentClosureCommandPort commentClosureCommandPort;

    @Override
    public void updateComment(CommentRequest commentRequest, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentRequest, userDetails);
        comment.updateComment(commentRequest.content());
        commentCommandPort.save(comment);
    }

    @Override
    public void deleteComment(CommentRequest commentRequest, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentRequest, userDetails);
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


    /**
     * <h3>댓글 유효성 검사 및 조회</h3>
     * <p>댓글 요청 DTO와 사용자 인증 정보를 기반으로 댓글의 유효성을 검사하고 댓글 엔티티를 조회합니다.</p>
     * <p>비밀번호가 일치하지 않거나, 사용자 본인이 아닌 경우 예외를 발생시킵니다.</p>
     *
     * @param commentRequest  댓글 요청 (비밀번호 포함)
     * @param userDetails 사용자 인증 정보
     * @return Comment 유효성 검사를 통과한 댓글 엔티티
     * @throws CustomException 댓글을 찾을 수 없거나, 비밀번호가 일치하지 않거나, 사용자 권한이 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private Comment validateComment(CommentRequest commentRequest, CustomUserDetails userDetails) {
        Comment comment = commentQueryPort.findById(commentRequest.id())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (commentRequest.password() != null && !Objects.equals(comment.getPassword(), commentRequest.password())) {
            throw new CustomException(ErrorCode.COMMENT_PASSWORD_NOT_MATCH);
        }

        if (commentRequest.password() == null
                && (userDetails == null || !Objects.equals(comment.getUser().getId(), userDetails.getUserId()))) {
            throw new CustomException(ErrorCode.ONLY_COMMENT_OWNER_UPDATE);
        }
        return comment;
    }


}
