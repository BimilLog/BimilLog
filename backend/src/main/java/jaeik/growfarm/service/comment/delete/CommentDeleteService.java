package jaeik.growfarm.service.comment.delete;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;

public interface CommentDeleteService {

    void deleteComment(CommentDTO commentDTO, CustomUserDetails userDetails);
}
