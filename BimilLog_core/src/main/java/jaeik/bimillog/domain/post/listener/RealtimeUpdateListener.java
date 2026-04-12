package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.global.event.RealtimeScoreEvent;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeUpdateListener {
    private final RedisPostRealTimeAdapter redisPostRealTimeAdapter;

    /**
     * <h3>실시간 인기글 점수 변동</h3>
     * <p>게시글 추천/추천취소/조회, 댓글 작성/삭제 이벤트를 수신하여 실시간 점수를 변동합니다.</p>
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleRealtimeScore(RealtimeScoreEvent event) {
        redisPostRealTimeAdapter.incrementRealtimePopularScore(event.postId(), event.realtimeScore());
    }
}
