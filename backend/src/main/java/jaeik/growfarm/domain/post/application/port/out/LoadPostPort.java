package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.entity.post.Post;
import java.util.Optional;

public interface LoadPostPort {
    Optional<Post> findById(Long postId);
}
