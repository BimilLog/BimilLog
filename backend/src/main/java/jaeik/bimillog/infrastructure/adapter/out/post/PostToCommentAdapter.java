package jaeik.bimillog.infrastructure.adapter.out.post;

import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostToCommentPort;
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
public class PostToCommentAdapter implements PostToCommentPort {

    private final CommentQueryUseCase commentQueryUseCase;
    private final CommentCommandUseCase commentCommandUseCase;


    /**
     * <h3>여러 게시글의 댓글 수 배치 조회</h3>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return commentQueryUseCase.findCommentCountsByPostIds(postIds);
    }

    /**
     * <h3>특정 글의 모든 댓글 삭제</h3>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteCommentInPost(Long postId) {
        commentCommandUseCase.deleteCommentsByPost(postId);
    }
}