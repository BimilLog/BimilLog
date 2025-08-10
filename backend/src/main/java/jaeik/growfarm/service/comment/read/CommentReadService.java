package jaeik.growfarm.service.comment.read;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CommentReadService {

    List<CommentDTO> getPopularComments(Long postId, CustomUserDetails userDetails);

    Page<CommentDTO> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails);
}
