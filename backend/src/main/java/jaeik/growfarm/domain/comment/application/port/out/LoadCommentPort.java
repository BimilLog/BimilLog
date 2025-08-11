package jaeik.growfarm.domain.comment.application.port.out;

import com.querydsl.core.Tuple;
import jaeik.growfarm.domain.comment.domain.Comment;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import jaeik.growfarm.domain.post.domain.Post;

public interface LoadCommentPort {

    List<Comment> findByPost(Post post);
    Optional<Comment> findById(Long id);

    Long countRootCommentsByPostId(Long postId);

    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);

    List<Long> findUserLikedCommentIds(List<Long> commentIds, Long userId);

    Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable);

    Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable);

    boolean isLikedByUser(Long commentId, Long userId);

    List<Long> findUserLikedCommentIdsByPostId(Long postId, Long userId);
}
