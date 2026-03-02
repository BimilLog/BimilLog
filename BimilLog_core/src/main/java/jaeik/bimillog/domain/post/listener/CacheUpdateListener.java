package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostModifiedEvent;
import jaeik.bimillog.domain.post.event.PostRemovedEvent;
import jaeik.bimillog.domain.post.event.PostWrittenEvent;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListDeleteAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * <h2>캐시 갱신 실행기</h2>
 * <p>글 작성/수정/삭제 시 모든 JSON LIST 캐시를 갱신합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheUpdateListener {
    private final PostQueryRepository postQueryRepository;
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;
    private final RedisPostRealTimeAdapter redisPostRealTimeAdapter;
    private final RedisPostListDeleteAdapter redisPostListDeleteAdapter;

    /**
     * <h3>글 작성 이벤트 처리</h3>
     * <p>첫 페이지 JSON LIST에 새 글을 LPUSH + LTRIM으로 추가합니다.</p>
     */
    @TransactionalEventListener
    @Async("cacheRefreshExecutor")
    public void handlePostWritten(PostWrittenEvent event) {
        redisPostListUpdateAdapter.addPostToList(RedisKey.FIRST_PAGE_JSON_KEY, event.postDetail(),
                RedisKey.FIRST_PAGE_SIZE + 1);
    }

    /**
     * <h3>글 수정 이벤트 처리</h3>
     * <p>모든 JSON LIST에서 해당 글의 제목을 업데이트합니다.</p>
     */
    @TransactionalEventListener
    @Async("cacheRefreshExecutor")
    public void handlePostModified(PostModifiedEvent event) {
        redisPostListUpdateAdapter.updateTitle(event.postId(), event.updatedPost().getTitle());
    }

    /**
     * <h3>글 삭제 이벤트 처리</h3>
     * <p>실시간 ZSet + 모든 JSON LIST를 정리합니다.</p>
     */
    @TransactionalEventListener
    @Async("cacheRefreshExecutor")
    public void handlePostRemoved(PostRemovedEvent event) {
        redisPostRealTimeAdapter.removePostIdFromRealtimeScore(event.postId());
        redisPostListDeleteAdapter.removePostFromCacheLists(event.postId());

        Long lastPostId = redisPostListDeleteAdapter.removePostAndGetLastId(RedisKey.FIRST_PAGE_JSON_KEY, event.postId());
        if (lastPostId != null) {
            List<PostSimpleDetail> nextPosts = postQueryRepository.findBoardPostsByCursor(lastPostId, 1);
            if (!nextPosts.isEmpty()) {
                redisPostListDeleteAdapter.appendPost(
                        RedisKey.FIRST_PAGE_JSON_KEY, nextPosts.getFirst(), RedisKey.FIRST_PAGE_SIZE + 1);
            }
        }
    }
}
