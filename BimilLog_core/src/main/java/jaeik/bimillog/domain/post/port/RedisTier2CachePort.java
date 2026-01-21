package jaeik.bimillog.domain.post.port;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;

import java.util.List;

public interface RedisTier2CachePort {

    /**
     * 특정 범위의 ID 리스트를 반환한다.
     */
    List<Long> getRangePostId(PostCacheFlag type, long start, long end);

    /**
     * 지원하는 캐시 타입을 반환한다.
     */
    List<PostCacheFlag> getSupportedTypes();

    /**
     * 특정 해시의 모든 게시글 ID를 반환한다.
     */
    List<Long> getAllPostId(PostCacheFlag type);
}
