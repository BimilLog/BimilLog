package jaeik.growfarm.service.comment.like;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;

public interface CommentLikeService {

    void likeComment(CommentDTO commentDTO, CustomUserDetails userDetails);
}
