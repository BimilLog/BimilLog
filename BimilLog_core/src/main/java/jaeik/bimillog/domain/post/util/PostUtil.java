package jaeik.bimillog.domain.post.util;

import jaeik.bimillog.domain.post.entity.PostCacheEntry;
import jaeik.bimillog.domain.post.entity.PostCountCache;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.redis.post.RedisPostCounterAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostUtil {
    private final RedisPostCounterAdapter redisPostCounterAdapter;

    /**
     * 페이지네이션
     */
    public Page<PostSimpleDetail> paginate(List<PostSimpleDetail> posts, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), posts.size());

        if (start >= posts.size()) {
            return new PageImpl<>(List.of(), pageable, posts.size());
        }

        return new PageImpl<>(posts.subList(start, end), pageable, posts.size());
    }

    /**
     * 캐시 엔트리 목록에 카운터를 조회하여 결합
     */
    public List<PostSimpleDetail> combineWithCounters(List<PostCacheEntry> entries) {
        List<Long> postIds = entries.stream().map(PostCacheEntry::id).toList();
        List<PostCountCache> counts = redisPostCounterAdapter.getCounters(postIds);
        return PostCacheEntry.combineAll(entries, counts);
    }
}
