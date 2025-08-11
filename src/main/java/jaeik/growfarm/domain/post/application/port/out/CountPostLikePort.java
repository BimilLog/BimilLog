package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.domain.Post;

public interface CountPostLikePort {
    long countByPost(Post post);
}
