package jaeik.growfarm.service.comment.write;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;

public interface CommentWriteService {

    void writeComment(CustomUserDetails userDetails, CommentDTO commentDTO);
}
