package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.global.event.CacheCountEvent;
import jaeik.bimillog.domain.post.event.PostEvent.PostDetailViewedEvent;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <h2>게시글 카운터 캐시 증감</h2>
 * <p>추천/댓글 이벤트를 받아 모든 JSON LIST의 카운터를 비동기로 증분합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "카운터 증감")
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheCountUpdateListener {
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;
    private final RedisPostViewAdapter redisPostViewAdapter;

    /**
     * <h3>게시글 상세 조회 이벤트 처리 — 조회수 버퍼 증가</h3>
     */
    @TransactionalEventListener
    @Async("cacheCountUpdateExecutor")
    public void handlePostDetailViewed(PostDetailViewedEvent event) {
        redisPostViewAdapter.markViewedAndIncrement(event.postId(), event.viewerKey());
    }

    /**
     * <h3>카운터 증감 이벤트 처리 — 좋아요/댓글 카운터 증감</h3>
     */
    @TransactionalEventListener
    @Async("cacheCountUpdateExecutor")
    public void handleCacheCount(CacheCountEvent event) {
        redisPostListUpdateAdapter.incrementCounterInAllLists(event.postId(), event.counterField(), event.counterDelta());
    }
}
