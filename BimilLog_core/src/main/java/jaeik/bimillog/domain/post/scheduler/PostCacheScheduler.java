package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
 * <p>게시글 인기도 기반 캐시 동기화를 담당하는 스케줄링 서비스</p>
 * <p>DB 조회 → 플래그 업데이트 → 글 단위 Hash 생성 → List 인덱스 교체를 통째로 재시도합니다.</p>
 * <p>첫 페이지 캐시도 1일마다 초기화 후 재생성하여 오염 데이터를 방지합니다.</p>
 * <p>이벤트 발행은 재시도 범위에서 제외됩니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Log(logResult = false, logExecutionTime = true, message = "스케줄 캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheScheduler {
    private final RedisPostHashAdapter redisPostHashAdapter;
    private final RedisPostIndexAdapter redisPostIndexAdapter;
    private final PostQueryRepository postQueryRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRate = 60000 * 1440)
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    @Transactional
    public void updateWeeklyPopularPosts() {
        refreshCache("WEEKLY", postQueryRepository::findWeeklyPopularPosts, RedisKey.POST_WEEKLY_IDS_KEY,
                postRepository::clearWeeklyFlag, postRepository::setWeeklyFlag,
                "주간 인기 게시글로 선정되었어요!", NotificationType.POST_FEATURED_WEEKLY);
    }

    @Scheduled(fixedRate = 60000 * 1440)
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    @Transactional
    public void updateLegendaryPosts() {
        refreshCache("LEGEND", postQueryRepository::findLegendaryPosts, RedisKey.POST_LEGEND_IDS_KEY,
                postRepository::clearLegendFlag, postRepository::setLegendFlag,
                "명예의 전당에 등극했어요!", NotificationType.POST_FEATURED_LEGEND);
    }

    @Scheduled(fixedRate = 60000 * 1440)
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    public void refreshNoticePosts() {
        refreshCache("NOTICE",
                () -> postQueryRepository.findNoticePostsForScheduler().stream()
                        .sorted(Comparator.comparingLong(PostSimpleDetail::getId).reversed()).toList(),
                RedisKey.POST_NOTICE_IDS_KEY, null, null, null, null);
    }

    @Scheduled(fixedRate = 60000 * 1440)
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    public void refreshFirstPageCache() {
        refreshCache("첫 페이지",
                () -> {
                    List<PostSimpleDetail> posts = postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE);
                    return posts.size() > RedisKey.FIRST_PAGE_SIZE ? posts.subList(0, RedisKey.FIRST_PAGE_SIZE) : posts;
                },
                RedisKey.FIRST_PAGE_LIST_KEY, null, null, null, null);
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
     * DB 조회 → (선택) 플래그 업데이트 → Hash 생성 + Index 교체 → (선택) 이벤트 발행
     */
    private void refreshCache(String type, Supplier<List<PostSimpleDetail>> queryFn, String redisKey,
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

        posts.forEach(redisPostHashAdapter::createPostHash);
        redisPostIndexAdapter.replaceIndex(redisKey, ids, RedisKey.DEFAULT_CACHE_TTL);
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
