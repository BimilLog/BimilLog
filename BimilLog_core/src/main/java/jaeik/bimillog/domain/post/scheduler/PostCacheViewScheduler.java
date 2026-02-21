package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.entity.jpa.QPost;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * <h2>카운트 플러시 스케줄러</h2>
 * <p>Redis 버퍼에 쌓인 조회수를 1분마다 DB + JSON LIST 캐시에 일괄 반영합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCacheViewScheduler {
    private final PostQueryRepository postQueryRepository;
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;
    private final RedisPostViewAdapter redisPostViewAdapter;

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void flushAllCounts() {
        try {
            Map<Long, Long> counts = redisPostViewAdapter.getAndClearViewCounts();
            if (counts.isEmpty()) return;

            postQueryRepository.bulkIncrementCount(counts, QPost.post.views);
            counts.forEach((postId, viewCount) ->
                    redisPostListUpdateAdapter.incrementCounterInAllLists(postId, "viewCount", viewCount));
            log.info("viewCount 플러시 완료: {}개 게시글 반영", counts.size());
        } catch (Exception e) {
            log.error("viewCount 플러시 실패", e);
        }
    }
}
