package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>캐시 갱신 실행기</h2>
 * <p>글 작성/수정/삭제 시 모든 JSON LIST 캐시를 갱신합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheUpdateSync {
    private final PostQueryRepository postQueryRepository;
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;

    private static final List<String> ALL_JSON_KEYS = List.of(
            RedisKey.FIRST_PAGE_JSON_KEY,
            RedisKey.POST_WEEKLY_JSON_KEY,
            RedisKey.POST_LEGEND_JSON_KEY,
            RedisKey.POST_NOTICE_JSON_KEY,
            RedisKey.POST_REALTIME_JSON_KEY
    );

    /**
     * <h3>새 글 작성 캐시 반영</h3>
     * <p>첫 페이지 JSON LIST에 새 글을 추가합니다. (LPUSH + LTRIM)</p>
     */
    @Async("cacheRefreshPool")
    public void asyncAddNewPost(PostSimpleDetail post) {
        redisPostJsonListAdapter.addNewPost(RedisKey.FIRST_PAGE_JSON_KEY, post, RedisKey.FIRST_PAGE_SIZE);
    }

    /**
     * <h3>글 수정 캐시 반영</h3>
     * <p>모든 JSON LIST에서 해당 글의 제목을 업데이트합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncUpdatePost(Long postId, PostSimpleDetail updatedPost) {
        for (String key : ALL_JSON_KEYS) {
            redisPostJsonListAdapter.updateTitle(key, postId, updatedPost.getTitle());
        }
    }

    /**
     * <h3>글 삭제 캐시 반영</h3>
     * <p>실시간 ZSET + 모든 JSON LIST를 정리합니다.</p>
     * <p>첫 페이지만 보충합니다. (주간/레전드/공지/실시간은 스케줄러가 재구축)</p>
     */
    @Async("cacheRefreshPool")
    public void asyncDeletePost(Long postId) {
        redisRealTimePostAdapter.removePostIdFromRealtimeScore(postId);

        // 주간/레전드/공지/실시간 JSON LIST에서 삭제 (보충 불필요)
        redisPostJsonListAdapter.removePost(RedisKey.POST_WEEKLY_JSON_KEY, postId);
        redisPostJsonListAdapter.removePost(RedisKey.POST_LEGEND_JSON_KEY, postId);
        redisPostJsonListAdapter.removePost(RedisKey.POST_NOTICE_JSON_KEY, postId);
        redisPostJsonListAdapter.removePost(RedisKey.POST_REALTIME_JSON_KEY, postId);

        // 첫 페이지 JSON LIST에서 삭제 + 보충
        Long lastPostId = redisPostJsonListAdapter.removePost(RedisKey.FIRST_PAGE_JSON_KEY, postId);
        if (lastPostId != null) {
            List<PostSimpleDetail> nextPosts = postQueryRepository.findBoardPostsByCursor(lastPostId, 1);
            if (!nextPosts.isEmpty()) {
                redisPostJsonListAdapter.appendPost(RedisKey.FIRST_PAGE_JSON_KEY, nextPosts.getFirst(), RedisKey.FIRST_PAGE_SIZE);
            }
        }
    }
}
