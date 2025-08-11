package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import com.querydsl.core.Tuple;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CommentReadRepository {
    Page<CommentDTO> findCommentsWithLatestOrder(Long postId, Pageable pageable, List<Long> likedCommentIds);
    List<CommentDTO> findPopularComments(Long postId, List<Long> likedCommentIds);
    Long countRootCommentsByPostId(Long postId);
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);
    Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable);
    Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable);
}




