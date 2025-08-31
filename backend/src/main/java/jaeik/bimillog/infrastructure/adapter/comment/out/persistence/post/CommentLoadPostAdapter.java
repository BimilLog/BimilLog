package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.post;

import jaeik.bimillog.domain.comment.application.port.out.LoadPostPort;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>게시글 어댑터</h2>
 * <p>
 * Comment 도메인에서 Post 도메인에 접근하기 위한 Outgoing-Adapter
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class CommentLoadPostAdapter implements LoadPostPort {

    private final PostQueryUseCase postQueryUseCase;

    @Override
    public Optional<Post> findById(Long postId) {
        return postQueryUseCase.findById(postId);
    }
}
