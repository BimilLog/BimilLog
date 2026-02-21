package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>게시글 카운터 캐시 증감</h2>
 * <p>추천/댓글 이벤트를 받아 모든 JSON LIST의 카운터를 비동기로 증분합니다.</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "카운터 증감")
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheUpdateCountSync {
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;

    /**
     * <h3>좋아요 카운터 증감</h3>
     *
     * @param postId 게시글 ID
     * @param delta  증감값 (1: 좋아요 추가, -1: 좋아요 취소)
     */
    @Async("cacheCountUpdateExecutor")
    public void incrementLikeCounter(Long postId, long delta) {
        redisPostListUpdateAdapter.incrementCounterInAllLists(postId, "likeCount", delta);
    }

    /**
     * <h3>댓글 카운터 증감</h3>
     *
     * @param postId 게시글 ID
     * @param delta  증감값 (1: 댓글 작성, -1: 댓글 삭제)
     */
    @Async("cacheCountUpdateExecutor")
    public void incrementCommentCounter(Long postId, long delta) {
        redisPostListUpdateAdapter.incrementCounterInAllLists(postId, "commentCount", delta);
    }
}
