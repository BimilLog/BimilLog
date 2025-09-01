package jaeik.bimillog.infrastructure.adapter.comment.out;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostCommentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h2>Post 도메인용 Comment 쿼리 어댑터</h2>
 * <p>Comment 도메인에 위치하면서 Post 도메인의 PostCommentQueryPort를 구현합니다.</p>
 * <p>헥사고날 아키텍처 원칙에 따라 Post Service가 Comment UseCase를 직접 의존하지 않도록 중간 어댑터 역할을 합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PostCommentQueryAdapter implements PostCommentQueryPort {

    private final CommentQueryUseCase commentQueryUseCase;

    /**
     * {@inheritDoc}
     *
     * <p>Comment 도메인의 UseCase를 통해 단일 게시글의 댓글 수를 조회합니다.</p>
     */
    @Override
    public void countByPostId(Long postId) {
        commentQueryUseCase.countByPostId(postId);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Comment 도메인의 UseCase를 통해 여러 게시글의 댓글 수를 배치 조회합니다.</p>
     */
    @Override
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return commentQueryUseCase.findCommentCountsByPostIds(postIds);
    }
}