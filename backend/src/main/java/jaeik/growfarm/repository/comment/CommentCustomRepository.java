package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/*
 * 커스텀 댓글 Repository
 * 댓글 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
@Repository
public interface CommentCustomRepository {

    List<Comment> findByCommentList(Long postId);

    Page<Comment> findByLikedComments(Long userId, Pageable pageable);

    /**
     * <h3>게시글별 댓글 수 조회</h3>
     * <p>
     * 게시글 ID 리스트로 각 게시글의 댓글 수를 조회한다.
     * </p>
     *
     * @param postIds 게시글 ID 리스트
     * @return 게시글 ID와 댓글 수의 맵
     * @author Jaeik
     * @since 1.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);
}
