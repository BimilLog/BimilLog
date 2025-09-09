package jaeik.bimillog.infrastructure.adapter.comment.out.post;

import jaeik.bimillog.domain.comment.application.port.out.CommentToPostPort;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>게시글 어댑터</h2>
 * <p>
 * Comment 도메인에서 Post 도메인에 접근하기 위한 아웃바운드 어댑터
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class CommentToPostAdapter implements CommentToPostPort {

    private final PostQueryUseCase postQueryUseCase;

    /**
     * {@inheritDoc}
     * 
     * <p>Post 도메인의 예외를 위임하여 Comment 서비스는 순수한 Post 엔티티를 받음</p>
     */
    @Override
    public Post findById(Long postId) {
        return postQueryUseCase.findById(postId);
    }
}
