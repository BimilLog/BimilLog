package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
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

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <h2>PostCacheScheduler</h2>
 * <p>게시글 캐시 동기화를 담당하는 스케줄링 서비스</p>
 * <p>주간/레전드/공지/첫 페이지/실시간 인기글 JSON LIST 캐시를 24시간마다 재구축합니다.</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Log(logResult = false, logExecutionTime = true, message = "스케줄 캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheScheduler {
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final PostQueryRepository postQueryRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int REALTIME_TOP_N = 5;

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    @Transactional
    public void updateWeeklyPopularPosts() {
        refreshCache("WEEKLY", postQueryRepository::findWeeklyPopularPosts,
                RedisKey.POST_WEEKLY_JSON_KEY,
                postRepository::clearWeeklyFlag, postRepository::setWeeklyFlag,
                "주간 인기 게시글로 선정되었어요!", NotificationType.POST_FEATURED_WEEKLY);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    @Transactional
    public void updateLegendaryPosts() {
        refreshCache("LEGEND", postQueryRepository::findLegendaryPosts,
                RedisKey.POST_LEGEND_JSON_KEY,
                postRepository::clearLegendFlag, postRepository::setLegendFlag,
                "명예의 전당에 등극했어요!", NotificationType.POST_FEATURED_LEGEND);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    public void refreshNoticePosts() {
        refreshCache("NOTICE",
                () -> postQueryRepository.findNoticePostsForScheduler().stream()
                        .sorted(Comparator.comparingLong(PostSimpleDetail::getId).reversed()).toList(),
                RedisKey.POST_NOTICE_JSON_KEY,
                null, null, null, null);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    public void refreshFirstPageCache() {
        List<PostSimpleDetail> posts = postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE);
        if (posts.size() > RedisKey.FIRST_PAGE_SIZE + 1) {
            posts = posts.subList(0, RedisKey.FIRST_PAGE_SIZE + 1);
        }
        if (posts.isEmpty()) {
            log.info("첫 페이지에 대한 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        redisPostJsonListAdapter.replaceAll(RedisKey.FIRST_PAGE_JSON_KEY, posts, RedisKey.DEFAULT_CACHE_TTL);
        log.info("첫 페이지 캐시 업데이트 완료. {}개의 게시글이 처리됨", posts.size());
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    public void refreshRealtimePopularPosts() {
        List<Long> topIds = redisRealTimePostAdapter.getRangePostId();
        if (topIds.isEmpty()) {
            log.info("실시간 인기글에 대한 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        List<PostSimpleDetail> posts = postQueryRepository
                .findPostSimpleDetailsByIds(topIds, PageRequest.of(0, REALTIME_TOP_N))
                .getContent();

        if (!posts.isEmpty()) {
            redisPostJsonListAdapter.replaceAll(RedisKey.POST_REALTIME_JSON_KEY, posts, RedisKey.DEFAULT_CACHE_TTL);
            log.info("실시간 인기글 캐시 업데이트 완료. {}개의 게시글이 처리됨", posts.size());
        }
    }

    @Recover
    public void recoverFeaturedUpdate(Exception e) {
        log.error("[FEATURED_SCHEDULE] 갱신 최종 실패 (5회 재시도): {}", e.getMessage(), e);
    }

    private void refreshCache(String type, Supplier<List<PostSimpleDetail>> queryFn,
                              String redisKey,
                              Runnable clearFlag, Consumer<List<Long>> setFlag,
                              String eventMessage, NotificationType notificationType) {
        List<PostSimpleDetail> posts = queryFn.get();
        if (posts.isEmpty()) {
            log.info("{}에 대한 게시글이 없어 캐시 업데이트를 건너뜁니다.", type);
            return;
        }

        if (clearFlag != null) {
            List<Long> ids = posts.stream().map(PostSimpleDetail::getId).toList();
            clearFlag.run();
            setFlag.accept(ids);
        }

        redisPostJsonListAdapter.replaceAll(redisKey, posts, RedisKey.DEFAULT_CACHE_TTL);
        log.info("{} 캐시 업데이트 완료. {}개의 게시글이 처리됨", type, posts.size());

        if (eventMessage != null) {
            try {
                publishFeaturedEventFromSimpleDetails(posts, eventMessage, notificationType);
            } catch (Exception e) {
                log.error("{} 이벤트 발행 실패: {}", type, e.getMessage());
            }
        }
    }

    private void publishFeaturedEventFromSimpleDetails(List<PostSimpleDetail> posts, String sseMessage,
                                                       NotificationType notificationType) {
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
