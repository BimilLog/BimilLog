package jaeik.bimillog.domain.post.adapter;

import jaeik.bimillog.domain.comment.service.CommentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h2>PostToCommentAdapter</h2>
 * <p>
 * Post 도메인에서 Comment 도메인으로의 어댑터입니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PostToCommentAdapter {
    private final CommentQueryService commentQueryService;

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
}
