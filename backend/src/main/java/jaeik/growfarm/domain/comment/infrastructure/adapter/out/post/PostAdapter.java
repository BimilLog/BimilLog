package jaeik.growfarm.domain.comment.infrastructure.adapter.out.post;

import jaeik.growfarm.domain.comment.application.port.out.LoadPostPort;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.domain.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostAdapter implements LoadPostPort {

    private final PostQueryUseCase postQueryUseCase;

    @Override
    public Optional<Post> findById(Long postId) {
        return postQueryUseCase.findById(postId);
    }
}
