package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostCounterAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <h2>실시간 인기글 점수 업데이트</h2>
 * <p>게시글 조회, 댓글 작성, 추천 이벤트를 수신하여 실시간 인기글 점수를 업데이트합니다.</p>
 * <p>조회: +2점, 댓글: +3점/-3점, 추천: +4점/-4점</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "실시간 인기글 점수")
@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimePostSync {
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;
    private final RedisPostCounterAdapter redisPostCounterAdapter;
    private final PostQueryRepository postQueryRepository;

    private static final double COMMENT_SCORE = 3.0;
    private static final double VIEW_SCORE = 2.0;
    private static final int REALTIME_TOP_N = 5;

    /**
     * <h3>실시간 인기글 점수 업데이트</h3>
     */
    @Async("realtimeEventExecutor")
    public void updateRealtimeScore(Long postId, double score) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(postId, score);
    }

    /**
     * <h3>게시글 상세 조회 — 조회수 + 실시간 점수 상승</h3>
     */
    @Async("realtimeEventExecutor")
    public void postDetailCheck(Long postId, String viewerKey) {
        try {
            redisRealTimePostAdapter.incrementRealtimePopularScore(postId, VIEW_SCORE);
            redisPostCounterAdapter.markViewedAndIncrement(postId, viewerKey);
        } catch (Exception e) {
            log.warn("상세글 조회 점수 상승 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * <h3>댓글 작성 이벤트 처리</h3>
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleCommentCreated(CommentCreatedEvent event) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(event.getPostId(), COMMENT_SCORE);
    }

    /**
     * <h3>댓글 삭제 이벤트 처리</h3>
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleCommentDeleted(CommentDeletedEvent event) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(event.postId(), -COMMENT_SCORE);
    }

    /**
     * <h3>실시간 인기글 JSON LIST 비동기 갱신</h3>
     * <p>ZSet과 LIST의 ID 순서가 불일치할 때 호출됩니다.</p>
     * <p>현재 LIST에서 oldIds를 추출하여 새로 들어온 글과 빠진 글을 판별합니다.</p>
     *
     * @param zsetTopIds ZSet에서 조회한 인기글 ID 목록 (점수 내림차순)
     */
    @Async("cacheRefreshExecutor")
    public void asyncRebuildRealtimeCache(List<Long> zsetTopIds) {
        try {
            // 현재 LIST에서 이전 ID 집합 추출
            Set<Long> oldIds = new HashSet<>(
                    redisPostJsonListAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY)
                            .stream().map(PostSimpleDetail::getId).toList()
            );

            List<PostSimpleDetail> dbPosts = postQueryRepository
                    .findPostSimpleDetailsByIds(zsetTopIds, PageRequest.of(0, REALTIME_TOP_N))
                    .getContent();
            if (dbPosts.isEmpty()) return;

            redisPostJsonListAdapter.replaceAll(RedisKey.POST_REALTIME_JSON_KEY, dbPosts, RedisKey.DEFAULT_CACHE_TTL);

            log.debug("[REALTIME] 비동기 캐시 갱신 완료: {}개 (신규: {}개)", dbPosts.size(),
                    dbPosts.stream().filter(p -> !oldIds.contains(p.getId())).count());
        } catch (Exception e) {
            log.warn("[REALTIME] 비동기 캐시 갱신 실패: {}", e.getMessage());
        }
    }
}
