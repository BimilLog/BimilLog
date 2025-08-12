package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;

import java.util.List;

public interface LoadPostCachePort {

    List<SimplePostResDTO> getCachedPopularPosts(PostCacheFlag type);

    FullPostResDTO getCachedFullPost(Long postId);

    boolean hasPopularPostsCache(PostCacheFlag type);
}
