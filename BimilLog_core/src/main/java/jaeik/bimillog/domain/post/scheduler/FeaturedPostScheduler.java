package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostEvent.PostFeaturedEvent;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>FeaturedPostScheduler</h2>
 * <p>주간 인기글, 전설 게시글의 플래그 갱신 + 캐시 교체 + 알림 발행을 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, logExecutionTime = true, message = "특집 게시글 스케줄 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class FeaturedPostScheduler {
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;
    private final PostQueryRepository postQueryRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    @Transactional
    public void updateWeeklyPopularPosts() {
        List<PostSimpleDetail> posts = queryAndReplaceCache("WEEKLY", PostQueryType.WEEKLY_SCHEDULER, RedisKey.POST_WEEKLY_JSON_KEY);
        if (posts.isEmpty()) return;

        List<Long> ids = posts.stream().map(PostSimpleDetail::getId).toList();
        postRepository.clearWeeklyFlag();
        postRepository.setWeeklyFlag(ids);

        publishFeaturedEvents(posts, "주간 인기 게시글로 선정되었어요!", NotificationType.POST_FEATURED_WEEKLY);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    @Transactional
    public void updateLegendaryPosts() {
        List<PostSimpleDetail> posts = queryAndReplaceCache("LEGEND", PostQueryType.LEGEND_SCHEDULER, RedisKey.POST_LEGEND_JSON_KEY);
        if (posts.isEmpty()) return;

        List<Long> ids = posts.stream().map(PostSimpleDetail::getId).toList();
        postRepository.clearLegendFlag();
        postRepository.setLegendFlag(ids);

        publishFeaturedEvents(posts, "명예의 전당에 등극했어요!", NotificationType.POST_FEATURED_LEGEND);
    }

    @Recover
    public void recoverFeaturedUpdate(Exception e) {
        log.error("[FEATURED_SCHEDULE] 갱신 최종 실패 (5회 재시도): {}", e.getMessage(), e);
    }

    List<PostSimpleDetail> queryAndReplaceCache(String type, PostQueryType queryType, String redisKey) {
        List<PostSimpleDetail> posts = postQueryRepository.selectPostSimpleDetails(
                queryType.condition(), PageRequest.of(0, queryType.getLimit()), queryType.getOrders()
        ).getContent();
        if (posts.isEmpty()) {
            log.info("{}에 대한 게시글이 없어 캐시 갱신을 건너뜁니다.", type);
            return posts;
        }
        redisPostListUpdateAdapter.replaceList(redisKey, posts, RedisKey.DEFAULT_CACHE_TTL);
        log.info("{} 캐시 갱신 완료. {}개의 게시글이 처리됨", type, posts.size());
        return posts;
    }

    private void publishFeaturedEvents(List<PostSimpleDetail> posts, String sseMessage, NotificationType notificationType) {
        posts.stream().filter(post -> post.getMemberId() != null)
                .forEach(post -> {
                    eventPublisher.publishEvent(new PostFeaturedEvent(
                            post.getMemberId(),
                            sseMessage,
                            post.getId(),
                            notificationType,
                            post.getTitle()
                    ));
                    log.info("게시글 ID {}에 대한 {} 알림 이벤트 발행: 회원 ID={}",
                            post.getId(), notificationType, post.getMemberId());
                });
    }
}
