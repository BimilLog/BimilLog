package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.service.PostInteractionService;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * <h2>카운트 플러시 스케줄러</h2>
 * <p>Redis Hash에 버퍼링된 조회수/좋아요/댓글수를 1분마다 DB + 모든 JSON LIST에 일괄 반영합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostUpdateScheduler {
    private final RedisPostUpdateAdapter redisPostUpdateAdapter;
    private final PostInteractionService postInteractionService;
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;

    private static final List<String> ALL_JSON_KEYS = List.of(
            RedisKey.FIRST_PAGE_JSON_KEY,
            RedisKey.POST_WEEKLY_JSON_KEY,
            RedisKey.POST_LEGEND_JSON_KEY,
            RedisKey.POST_NOTICE_JSON_KEY,
            RedisKey.POST_REALTIME_JSON_KEY
    );

    /**
     * <h3>전체 카운트 일괄 반영</h3>
     * <p>1분마다 Redis에서 누적된 조회수/좋아요/댓글수를 가져와 DB + 모든 JSON LIST에 벌크 반영합니다.</p>
     */
    @Scheduled(fixedRate = 60000) // 1분
    public void flushAllCounts() {
        flushCounts(RedisKey.FIELD_VIEW_COUNT, redisPostUpdateAdapter::getAndClearViewCounts);
        flushCounts(RedisKey.FIELD_LIKE_COUNT, redisPostUpdateAdapter::getAndClearLikeCounts);
        flushCounts(RedisKey.FIELD_COMMENT_COUNT, redisPostUpdateAdapter::getAndClearCommentCounts);
    }

    private void flushCounts(String field, Supplier<Map<Long, Long>> countsSupplier) {
        try {
            Map<Long, Long> counts = countsSupplier.get();
            if (counts.isEmpty()) return;

            postInteractionService.bulkIncrementCounts(counts, field);
            for (String key : ALL_JSON_KEYS) {
                redisPostJsonListAdapter.batchIncrementCounts(key, counts, field);
            }
            log.info("{} 플러시 완료: {}개 게시글 반영", field, counts.size());
        } catch (Exception e) {
            log.error("{} 플러시 실패", field, e);
        }
    }
}
