package jaeik.growfarm.domain.comment.application.port.in;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import jaeik.growfarm.domain.comment.domain.Comment;

public interface CommentQueryUseCase {

    List<CommentDTO> getPopularComments(Long postId, CustomUserDetails userDetails);

    Page<CommentDTO> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails);

    Optional<Comment> findById(Long commentId);
}
