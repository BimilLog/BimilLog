package jaeik.growfarm.domain.comment.application.port.out;

import com.querydsl.core.Tuple;
import jaeik.growfarm.entity.comment.Comment;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadCommentPort {

    Optional<Comment> findById(Long commentId);

    List<Tuple> findCommentsWithLatestOrder(Long postId, Pageable pageable);

    List<Tuple> findPopularComments(Long postId);

    Long countRootCommentsByPostId(Long postId);

    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);

    List<Long> findUserLikedCommentIds(List<Long> commentIds, Long userId);
}
