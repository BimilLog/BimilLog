package jaeik.growfarm.domain.comment.infrastructure.adapter.out.post;

import jaeik.growfarm.domain.comment.application.port.out.LoadPostPort;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.entity.Post;
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
@Component("commentPostAdapter")
@RequiredArgsConstructor
public class PostAdapter implements LoadPostPort {

    private final PostQueryUseCase postQueryUseCase;

    @Override
    public Optional<Post> findById(Long postId) {
        return postQueryUseCase.findById(postId);
    }
}
