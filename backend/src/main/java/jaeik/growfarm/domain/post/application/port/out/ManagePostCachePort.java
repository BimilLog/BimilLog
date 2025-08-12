package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;

import java.util.List;

public interface ManagePostCachePort {

    void cachePosts(PostCacheFlag type, List<SimplePostResDTO> cachePosts);

    void deletePopularPostsCache(PostCacheFlag type);

    void cacheFullPost(FullPostResDTO post);

    void deleteFullPostCache(Long postId);
}
