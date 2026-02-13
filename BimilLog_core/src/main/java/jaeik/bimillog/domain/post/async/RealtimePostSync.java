package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.entity.PostCacheEntry;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <h2>실시간 인기글 점수 업데이트</h2>
 * <p>게시글 조회, 댓글 작성, 추천 이벤트를 수신하여 실시간 인기글 점수를 업데이트합니다.</p>
 * <p>조회: +2점, 댓글: +3점/-3점, 추천: +4점/-4점</p>
 * <p>비동기 처리를 통해 이벤트 발행자와 독립적으로 실행됩니다.</p>
 * <p>ZSet과 JSON LIST 불일치 시 비동기 캐시 갱신을 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.9.0
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
    private static final int REALTIME_TOP_N = 5;

    /**
     * <h3>실시간 인기글 점수 업데이트</h3>
     * <p>게시글의 실시간 인기글 점수를 주어진 값만큼 증감시킵니다.</p>
     *
     * @param postId 게시글 ID
     * @param score 증감할 점수 (양수: 증가, 음수: 감소)
     */
    @Async("realtimeEventExecutor")
    public void updateRealtimeScore(Long postId, double score) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(postId, score);
    }

    /**
     * <h3>댓글 작성 이벤트 처리</h3>
     * <p>댓글 작성 시 해당 게시글의 실시간 인기글 점수를 3점 증가시킵니다.</p>
     *
     * @param event 댓글 작성 이벤트
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleCommentCreated(CommentCreatedEvent event) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(event.getPostId(), COMMENT_SCORE);
    }

    /**
     * <h3>댓글 삭제 이벤트 처리</h3>
     * <p>댓글 삭제 시 해당 게시글의 실시간 인기글 점수를 3점 감소시킵니다.</p>
     *
     * @param event 댓글 삭제 이벤트
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleCommentDeleted(CommentDeletedEvent event) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(event.postId(), -COMMENT_SCORE);
    }

    /**
     * <h3>실시간 인기글 JSON LIST 비동기 갱신 (diff 기반)</h3>
     * <p>ZSet과 LIST의 ID 순서가 불일치할 때 호출됩니다.</p>
     * <p>이전 실시간 SET과 비교하여 빠진 글/새 글을 판별하고,
     * 실시간 SET·카운터 Hash를 함께 갱신합니다.</p>
     * <ol>
     *   <li>이전 실시간 ID: SMEMBERS post:cached:realtime:ids</li>
     *   <li>diff 계산: removed = 이전 - 새로운, added = 새로운 - 이전</li>
     *   <li>실시간 SET 원자적 재구축</li>
     *   <li>어떤 카테고리에도 속하지 않는 빠진 ID → HDEL (카운터 Hash 정리)</li>
     *   <li>DB 조회 → JSON LIST 교체</li>
     *   <li>새로 들어온 글만 카운터 Hash 초기화</li>
     * </ol>
     *
     * @param zsetTopIds ZSet에서 조회한 인기글 ID 목록 (점수 내림차순)
     */
    @Async("cacheRefreshExecutor")
    public void asyncRebuildRealtimeCache(List<Long> zsetTopIds) {
        try {
            // 1. 이전 실시간 ID 조회
            Set<Long> oldIds = redisPostCounterAdapter.getCategorySet(RedisKey.CACHED_REALTIME_IDS_KEY);
            Set<Long> newIds = new LinkedHashSet<>(zsetTopIds);

            // 2. diff 계산
            Set<Long> removed = new HashSet<>(oldIds);
            removed.removeAll(newIds);
            Set<Long> added = new HashSet<>(newIds);
            added.removeAll(oldIds);

            // 3. 실시간 SET 원자적 재구축
            redisPostCounterAdapter.rebuildCategorySet(RedisKey.CACHED_REALTIME_IDS_KEY, newIds);

            // 4. 어떤 카테고리에도 속하지 않는 빠진 ID → 카운터 Hash 정리
            for (Long id : removed) {
                if (!redisPostCounterAdapter.isCachedPost(id)) {
                    redisPostCounterAdapter.removeCounterFields(id);
                }
            }

            // 5. DB 조회 → JSON LIST 교체
            List<PostSimpleDetail> dbPosts = postQueryRepository
                    .findPostSimpleDetailsByIds(zsetTopIds, PageRequest.of(0, REALTIME_TOP_N))
                    .getContent();
            if (!dbPosts.isEmpty()) {
                List<PostCacheEntry> entries = dbPosts.stream().map(PostCacheEntry::from).toList();
                redisPostJsonListAdapter.replaceAll(RedisKey.POST_REALTIME_JSON_KEY, entries, RedisKey.DEFAULT_CACHE_TTL);
            }

            // 6. 새로 들어온 글만 카운터 Hash 초기화 (기존 글은 증분값 유지)
            if (!added.isEmpty()) {
                List<PostSimpleDetail> addedPosts = dbPosts.stream()
                        .filter(p -> added.contains(p.getId()))
                        .toList();
                redisPostCounterAdapter.batchSetCounters(addedPosts);
            }

            log.debug("[REALTIME] 비동기 캐시 갱신 완료: {}개 (추가: {}, 제거: {})",
                    dbPosts.size(), added.size(), removed.size());
        } catch (Exception e) {
            log.warn("[REALTIME] 비동기 캐시 갱신 실패: {}", e.getMessage());
        }
    }
}
