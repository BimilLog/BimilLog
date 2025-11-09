package jaeik.bimillog.domain.post.out;

import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.comment.service.CommentQueryService;
import jaeik.bimillog.domain.post.service.PostCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h2>PostToCommentAdapter</h2>
 * <p>
 * Post 도메인에서 Comment 도메인으로의 크로스 도메인 어댑터입니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PostToCommentAdapter {

    private final CommentQueryService commentQueryService;
    private final CommentCommandService commentCommandService;


    /**
     * <h3>여러 게시글의 댓글 수 배치 조회</h3>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return commentQueryService.findCommentCountsByPostIds(postIds);
    }

    /**
     * <h3>특정 글의 모든 댓글 삭제</h3>
     * <p>Post 도메인에서 Comment 도메인으로 댓글 삭제를 위임하는 어댑터 메서드입니다.</p>
     * <p>{@link PostCommandService#deletePost}에서 게시글 삭제 시 호출됩니다.</p>
     *
     * @param postId 댓글을 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public void deleteCommentInPost(Long postId) {
        commentCommandService.deleteCommentsByPost(postId);
    }
}
