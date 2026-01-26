package jaeik.bimillog.domain.post.scheduler;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.POST_CACHE_TTL_WEEKLY_LEGEND;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>PostScheduledService</h2>
 * <p>게시글 인기도 기반 캐시 동기화를 담당하는 스케줄링 서비스로서 </p>
 * <p>스프링 스케줄러를 통한 주기적 실행과 이벤트 발행을 통해 도메인 간 통신을 수행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Log(logResult = false, logExecutionTime = true, message = "스케줄 캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostScheduledService {
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final FeaturedPostScheduleExecutor featuredPostScheduleExecutor;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";

    /**
     * <h3>실시간 인기 게시글 점수 지수감쇠 적용</h3>
     * <p>스프링 스케줄러를 통해 10분마다 실시간 인기글 점수에 감쇠를 적용합니다.</p>
     * <p>서킷 닫힘(Redis 정상): Redis ZSet에 감쇠 적용</p>
     * <p>서킷 열림(Redis 장애): Caffeine 폴백 저장소에 감쇠 적용</p>
     */
    @Scheduled(fixedRate = 60000 * 10) // 10분마다
    public void applyRealtimeScoreDecay() {
        if (isRealtimeRedisCircuitOpen()) {
            try {
                realtimeScoreFallbackStore.applyDecay();
            } catch (Exception e) {
                log.error("Fallback 저장소 지수감쇠 적용 실패", e);
            }
        } else {
            try {
                redisRealTimePostAdapter.applyRealtimePopularScoreDecay();
            } catch (Exception e) {
                log.error("Redis 실시간 인기글 점수 지수감쇠 적용 실패", e);
            }
        }
    }

    private boolean isRealtimeRedisCircuitOpen() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(REALTIME_REDIS_CIRCUIT);
        CircuitBreaker.State state = cb.getState();
        return state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN;
    }

    /**
     * <h3>주간 인기 게시글 스케줄링 갱신 및 알림 발행</h3>
     * <p>1일마다 주간 인기 게시글을 갱신하고 PostSimpleDetail을 Redis Hash에 저장합니다.</p>
     * <p>지난 7일간의 조회수와 좋아요 종합 점수를 기반으로 주간 인기 게시글을 선정합니다.</p>
     * <p>인기 게시글로 선정된 작성자에게 PostFeaturedEvent를 발행하여 알림을 전송합니다.</p>
     * <p>선정된 인기글은 featured_post 테이블에 영속화됩니다.</p>
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    public void updateWeeklyPopularPosts() {
        List<PostSimpleDetail> posts = featuredPostScheduleExecutor.fetchWeeklyPosts();

        if (posts.isEmpty()) {
            log.info("WEEKLY에 대한 인기 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        // DB에 특집 게시글 저장 (기존 WEEKLY 삭제 후 새로 저장)
        featuredPostScheduleExecutor.saveFeaturedPosts(posts, PostCacheFlag.WEEKLY);

        try {
            // Hash 캐시에 TTL 1분으로 저장
            redisSimplePostAdapter.cachePostsWithTtl(PostCacheFlag.WEEKLY, posts, POST_CACHE_TTL_WEEKLY_LEGEND);
            log.info("WEEKLY 캐시 업데이트 완료. {}개의 게시글이 처리됨", posts.size());
        } catch (Exception e) {
            log.error("WEEKLY 캐시 업데이트 실패: {}", e.getMessage(), e);
        }

        publishFeaturedEventFromSimpleDetails(posts,
            "주간 인기 게시글로 선정되었어요!",
            NotificationType.POST_FEATURED_WEEKLY);
    }

    /**
     * <h3>전설 게시글 스케줄링 갱신 및 명예의 전당 알림 발행</h3>
     * <p>PostSimpleDetail을 Redis Hash에 저장</p>
     * <p>전설 게시글로 선정된 작성자에게 PostFeaturedEvent를 발행하여 알림을 전송합니다.</p>
     * <p>선정된 레전드는 featured_post 테이블에 영속화됩니다.</p>
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    public void updateLegendaryPosts() {
        List<PostSimpleDetail> posts = featuredPostScheduleExecutor.fetchLegendaryPosts();

        if (posts.isEmpty()) {
            log.info("LEGEND에 대한 인기 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        // DB에 특집 게시글 저장 (기존 LEGEND 삭제 후 새로 저장)
        featuredPostScheduleExecutor.saveFeaturedPosts(posts, PostCacheFlag.LEGEND);

        try {
            redisSimplePostAdapter.cachePostsWithTtl(PostCacheFlag.LEGEND, posts, POST_CACHE_TTL_WEEKLY_LEGEND);
            log.info("LEGEND 캐시 업데이트 완료. {}개의 게시글이 처리됨", posts.size());
        } catch (Exception e) {
            log.error("LEGEND 캐시 업데이트 실패: {}", e.getMessage(), e);
        }

        publishFeaturedEventFromSimpleDetails(posts,
            "명예의 전당에 등극했어요!",
            NotificationType.POST_FEATURED_LEGEND);
    }

    /**
     * <h3>인기 게시글 알림 이벤트 발행</h3>
     * <p>인기 게시글로 선정된 사용자에게 알림 이벤트를 발행합니다.</p>
     * <p>FCM 푸시 알림의 title과 body는 FcmCommandService에서 NotificationType에 따라 작성됩니다.</p>
     *
     * @param posts 알림을 보낼 게시글 목록
     * @param sseMessage SSE 알림 메시지
     * @param notificationType 인기글 유형 (WEEKLY/LEGEND/REALTIME)
     */
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
