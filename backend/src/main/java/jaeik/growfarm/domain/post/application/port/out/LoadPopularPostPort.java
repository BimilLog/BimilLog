package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.dto.post.SimplePostResDTO;

import java.util.List;

public interface LoadPopularPostPort {

    List<SimplePostResDTO> findRealtimePopularPosts();

    List<SimplePostResDTO> findWeeklyPopularPosts();

    List<SimplePostResDTO> findLegendaryPosts();

    List<SimplePostResDTO> findNoticePosts();

    void applyPopularFlag(List<Long> postIds, PostCacheFlag postCacheFlag);

    void resetPopularFlag(PostCacheFlag postCacheFlag);
}
