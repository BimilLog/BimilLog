package jaeik.growfarm.service.comment.update;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;

public interface CommentUpdateService {

    void updateComment(CommentDTO commentDTO, CustomUserDetails userDetails);
}
