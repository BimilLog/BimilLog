package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostCacheEntry;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostCounterAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <h2>PostCacheScheduler</h2>
 * <p>게시글 캐시 동기화를 담당하는 스케줄링 서비스</p>
 * <p>주간/레전드/공지/첫 페이지/실시간 인기글 JSON LIST 캐시를 24시간마다 재구축합니다.</p>
 * <p>DB 조회 → 플래그 업데이트 → JSON LIST 전체 교체를 통째로 재시도합니다.</p>
 * <p>이벤트 발행은 재시도 범위에서 제외됩니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, logExecutionTime = true, message = "스케줄 캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheScheduler {
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;
    private final RedisPostCounterAdapter redisPostCounterAdapter;
    private final RedisPostIndexAdapter redisPostIndexAdapter;
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
                RedisKey.POST_WEEKLY_JSON_KEY, RedisKey.CACHED_WEEKLY_IDS_KEY,
                postRepository::clearWeeklyFlag, postRepository::setWeeklyFlag,
                "주간 인기 게시글로 선정되었어요!", NotificationType.POST_FEATURED_WEEKLY);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    @Transactional
    public void updateLegendaryPosts() {
        refreshCache("LEGEND", postQueryRepository::findLegendaryPosts,
                RedisKey.POST_LEGEND_JSON_KEY, RedisKey.CACHED_LEGEND_IDS_KEY,
                postRepository::clearLegendFlag, postRepository::setLegendFlag,
                "명예의 전당에 등극했어요!", NotificationType.POST_FEATURED_LEGEND);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    public void refreshNoticePosts() {
        refreshCache("NOTICE",
                () -> postQueryRepository.findNoticePostsForScheduler().stream()
                        .sorted(Comparator.comparingLong(PostSimpleDetail::getId).reversed()).toList(),
                RedisKey.POST_NOTICE_JSON_KEY, RedisKey.CACHED_NOTICE_IDS_KEY,
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

        List<PostCacheEntry> entries = posts.stream().map(PostCacheEntry::from).toList();
        redisPostJsonListAdapter.replaceAll(RedisKey.FIRST_PAGE_JSON_KEY, entries, RedisKey.DEFAULT_CACHE_TTL);
        redisPostCounterAdapter.batchSetCounters(posts);
        redisPostIndexAdapter.rebuildCategorySet(RedisKey.CACHED_FIRSTPAGE_IDS_KEY,
                posts.stream().map(PostSimpleDetail::getId).toList());
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
            List<Long> postIds = posts.stream().map(PostSimpleDetail::getId).toList();
            List<PostCacheEntry> entries = posts.stream().map(PostCacheEntry::from).toList();
            redisPostJsonListAdapter.replaceAll(RedisKey.POST_REALTIME_JSON_KEY, entries, RedisKey.DEFAULT_CACHE_TTL);
            redisPostCounterAdapter.batchSetCounters(posts);
            redisPostIndexAdapter.rebuildCategorySet(RedisKey.CACHED_REALTIME_IDS_KEY, postIds);
            log.info("실시간 인기글 캐시 업데이트 완료. {}개의 게시글이 처리됨", posts.size());
        }
    }

    /**
     * <h3>스케줄러 최종 실패 복구</h3>
     * <p>5회 재시도 후에도 실패 시 로그만 남깁니다.</p>
     */
    @Recover
    public void recoverFeaturedUpdate(Exception e) {
        log.error("[FEATURED_SCHEDULE] 갱신 최종 실패 (5회 재시도): {}", e.getMessage(), e);
    }

    /**
     * DB 조회 → (선택) 플래그 업데이트 → JSON LIST 전체 교체 + 카테고리 SET 재구축 → (선택) 이벤트 발행
     */
    private void refreshCache(String type, Supplier<List<PostSimpleDetail>> queryFn,
                              String redisKey, String categorySetKey,
                              Runnable clearFlag, Consumer<List<Long>> setFlag,
                              String eventMessage, NotificationType notificationType) {
        List<PostSimpleDetail> posts = queryFn.get();
        if (posts.isEmpty()) {
            log.info("{}에 대한 게시글이 없어 캐시 업데이트를 건너뜁니다.", type);
            return;
        }

        List<Long> ids = posts.stream().map(PostSimpleDetail::getId).toList();

        if (clearFlag != null) {
            clearFlag.run();
            setFlag.accept(ids);
        }

        List<PostCacheEntry> entries = posts.stream().map(PostCacheEntry::from).toList();
        redisPostJsonListAdapter.replaceAll(redisKey, entries, RedisKey.DEFAULT_CACHE_TTL);
        redisPostCounterAdapter.batchSetCounters(posts);
        redisPostIndexAdapter.rebuildCategorySet(categorySetKey, ids);
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
