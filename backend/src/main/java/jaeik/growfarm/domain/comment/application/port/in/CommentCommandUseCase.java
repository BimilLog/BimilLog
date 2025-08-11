package jaeik.growfarm.domain.comment.application.port.in;

import jaeik.growfarm.dto.comment.CommentDto;
import jaeik.growfarm.global.auth.CustomUserDetails;

public interface CommentCommandUseCase {

    void writeComment(CustomUserDetails userDetails, CommentDto commentDto);

    void updateComment(CommentDto commentDto, CustomUserDetails userDetails);

    void deleteComment(CommentDto commentDto, CustomUserDetails userDetails);

    void likeComment(CommentDto commentDto, CustomUserDetails userDetails);

    void anonymizeUserComments(Long userId);
}
