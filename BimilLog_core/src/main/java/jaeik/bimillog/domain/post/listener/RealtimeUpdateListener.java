package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.global.event.RealtimeScoreEvent;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostEvent.RealtimeCacheRebuildEvent;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeUpdateListener {
    private final RedisPostRealTimeAdapter redisPostRealTimeAdapter;
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;
    private final PostRepository postRepository;

    /**
     * <h3>실시간 인기글 점수 변동</h3>
     * <p>게시글 추천/추천취소/조회, 댓글 작성/삭제 이벤트를 수신하여 실시간 점수를 변동합니다.</p>
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleRealtimeScore(RealtimeScoreEvent event) {
        redisPostRealTimeAdapter.incrementRealtimePopularScore(event.postId(), event.realtimeScore());
    }

    /**
     * <h3>실시간 인기글 JSON LIST 비동기 갱신</h3>
     * <p>ZSet과 LIST의 ID 순서가 불일치할 때 이벤트를 수신하여 갱신합니다.</p>
     * <p>호출측(RealtimePostCacheService)에 트랜잭션이 없으므로 @EventListener 사용</p>
     */
    @EventListener
    @Async("realtimeEventExecutor")
    public void handleRealtimeCacheRebuild(RealtimeCacheRebuildEvent event) {
        List<PostSimpleDetail> dbPosts = postRepository.findAllByIds(event.postIds()).stream().map(PostSimpleDetail::from).toList();
        if (dbPosts.isEmpty()) return;
        redisPostListUpdateAdapter.replaceList(RedisKey.POST_REALTIME_JSON_KEY, dbPosts, RedisKey.DEFAULT_CACHE_TTL);
    }
}
