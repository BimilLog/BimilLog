package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <h2>게시글 조회수 증가 리스너</h2>
 * <p>게시글 조회 이벤트를 수신하여 중복 조회 방지 후 조회수를 증가시킵니다.</p>
 * <p>Redis SET으로 24시간 중복 조회를 방지하고, Redis Hash에 조회수를 버퍼링합니다.</p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "조회수 증가")
@Component
@RequiredArgsConstructor
@Slf4j
public class PostViewCountSync {
    private final RedisPostViewAdapter redisPostViewAdapter;

    /**
     * <h3>게시글 조회 이벤트 처리</h3>
     * <p>중복 조회가 아닌 경우 조회수를 1 증가시킵니다.</p>
     *
     * @param event 게시글 조회 이벤트
     */
    @Async("realtimeEventExecutor")
    @Transactional
    public void handlePostViewed(PostViewedEvent event) {
        try {
            if (!redisPostViewAdapter.hasViewed(event.postId(), event.viewerKey())) {
                redisPostViewAdapter.markViewed(event.postId(), event.viewerKey());
                redisPostViewAdapter.incrementViewCount(event.postId());
            }
        } catch (Exception e) {
            log.warn("조회수 처리 실패: postId={}, error={}", event.postId(), e.getMessage());
        }
    }
}
