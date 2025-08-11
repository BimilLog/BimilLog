package jaeik.growfarm.domain.comment.application.port.in;

import jaeik.growfarm.dto.comment.CommentDto;
import jaeik.growfarm.global.auth.CustomUserDetails;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CommentQueryUseCase {

    List<CommentDto> getPopularComments(Long postId, CustomUserDetails userDetails);

    Page<CommentDto> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails);
}
