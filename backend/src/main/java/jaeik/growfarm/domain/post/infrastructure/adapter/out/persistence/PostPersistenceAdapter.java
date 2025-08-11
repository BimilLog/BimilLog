package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.post.application.port.out.LoadPostPort;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostPersistenceAdapter implements LoadPostPort {

    private final PostRepository postRepository;

    @Override
    public Optional<Post> findById(Long postId) {
        return postRepository.findById(postId);
    }
}
