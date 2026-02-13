package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.entity.jpa.QPost;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * <h2>카운트 플러시 스케줄러</h2>
 * <p>Redis Hash에 버퍼링된 조회수를 1분마다 DB + 모든 JSON LIST에 일괄 반영합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostUpdateScheduler {
    private final RedisPostUpdateAdapter redisPostUpdateAdapter;
    private final PostQueryRepository postQueryRepository;
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;

    private static final List<String> ALL_JSON_KEYS = List.of(
            RedisKey.FIRST_PAGE_JSON_KEY,
            RedisKey.POST_WEEKLY_JSON_KEY,
            RedisKey.POST_LEGEND_JSON_KEY,
            RedisKey.POST_NOTICE_JSON_KEY,
            RedisKey.POST_REALTIME_JSON_KEY
    );

    /**
     * <h3>조회수 일괄 반영</h3>
     * <p>1분마다 Redis에서 누적된 조회수를 가져와 DB + 모든 JSON LIST에 벌크 반영합니다.</p>
     */
    @Transactional
    @Scheduled(fixedRate = 60000) // 1분
    public void flushAllCounts() {
        flushViewCounts();
    }

    private void flushViewCounts() {
        try {
            Map<Long, Long> counts = redisPostUpdateAdapter.getAndClearViewCounts();
            if (counts.isEmpty()) return;

            postQueryRepository.bulkIncrementCount(counts, QPost.post.views);
            for (String key : ALL_JSON_KEYS) {
                redisPostJsonListAdapter.batchIncrementCounts(key, counts, RedisKey.FIELD_VIEW_COUNT);
            }
            log.info("{} 플러시 완료: {}개 게시글 반영", RedisKey.FIELD_VIEW_COUNT, counts.size());
        } catch (Exception e) {
            log.error("{} 플러시 실패", RedisKey.FIELD_VIEW_COUNT, e);
        }
    }
}
