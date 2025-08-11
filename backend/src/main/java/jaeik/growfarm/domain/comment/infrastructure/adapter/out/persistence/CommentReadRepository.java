package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import com.querydsl.core.Tuple;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CommentReadRepository {
    List<Tuple> findCommentsWithLatestOrder(Long postId, Pageable pageable);
    List<Tuple> findPopularComments(Long postId);
    Long countRootCommentsByPostId(Long postId);
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);
    Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable);
}




