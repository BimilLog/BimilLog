package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.service.PostInteractionService;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <h2>카운트 플러시 스케줄러</h2>
 * <p>Redis Hash에 버퍼링된 조회수/좋아요/댓글수를 1분마다 DB + 글 단위 Hash에 일괄 반영합니다.</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostViewCountFlushScheduler {
    private final RedisPostUpdateAdapter redisPostUpdateAdapter;
    private final PostInteractionService postInteractionService;
    private final RedisPostHashAdapter redisPostHashAdapter;

    /**
     * <h3>전체 카운트 일괄 반영</h3>
     * <p>1분마다 Redis에서 누적된 조회수/좋아요/댓글수를 가져와 DB + Hash에 벌크 반영합니다.</p>
     */
    @Scheduled(fixedRate = 60000) // 1분
    public void flushAllCounts() {
        flushViewCounts();
        flushLikeCounts();
        flushCommentCounts();
    }

    private void flushViewCounts() {
        try {
            Map<Long, Long> counts = redisPostUpdateAdapter.getAndClearViewCounts();
            if (counts.isEmpty()) return;

            postInteractionService.bulkIncrementViewCounts(counts);
            redisPostHashAdapter.batchIncrementCounts(counts, RedisPostHashAdapter.FIELD_VIEW_COUNT);
            log.info("조회수 플러시 완료: {}개 게시글 반영", counts.size());
        } catch (Exception e) {
            log.error("조회수 플러시 실패", e);
        }
    }

    private void flushLikeCounts() {
        try {
            Map<Long, Long> counts = redisPostUpdateAdapter.getAndClearLikeCounts();
            if (counts.isEmpty()) return;

            postInteractionService.bulkIncrementLikeCounts(counts);
            redisPostHashAdapter.batchIncrementCounts(counts, RedisPostHashAdapter.FIELD_LIKE_COUNT);
            log.info("좋아요 플러시 완료: {}개 게시글 반영", counts.size());
        } catch (Exception e) {
            log.error("좋아요 플러시 실패", e);
        }
    }

    private void flushCommentCounts() {
        try {
            Map<Long, Long> counts = redisPostUpdateAdapter.getAndClearCommentCounts();
            if (counts.isEmpty()) return;

            postInteractionService.bulkIncrementCommentCounts(counts);
            redisPostHashAdapter.batchIncrementCounts(counts, RedisPostHashAdapter.FIELD_COMMENT_COUNT);
            log.info("댓글수 플러시 완료: {}개 게시글 반영", counts.size());
        } catch (Exception e) {
            log.error("댓글수 플러시 실패", e);
        }
    }
}
