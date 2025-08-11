package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.post.domain.Post;

import java.util.Optional;

public interface LoadPostPort {
    Optional<Post> findById(Long postId);
}
