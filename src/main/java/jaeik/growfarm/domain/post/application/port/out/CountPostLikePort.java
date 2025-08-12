package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.Post;

public interface CountPostLikePort {
    long countByPost(Post post);
}
