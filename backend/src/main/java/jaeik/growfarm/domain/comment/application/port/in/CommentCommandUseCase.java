package jaeik.growfarm.domain.comment.application.port.in;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;

public interface CommentCommandUseCase {

    void writeComment(CustomUserDetails userDetails, CommentDTO commentDto);

    void updateComment(CommentDTO commentDto, CustomUserDetails userDetails);

    void deleteComment(CommentDTO commentDto, CustomUserDetails userDetails);

    void likeComment(CommentDTO commentDto, CustomUserDetails userDetails);
}
