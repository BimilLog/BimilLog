package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.FeaturedPost;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.adapter.PostToCommentAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.POST_CACHE_TTL_WEEKLY_LEGEND;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>PostScheduledService</h2>
 * <p>게시글 인기도 기반 캐시 동기화를 담당하는 스케줄링 서비스로서 </p>
 * <p>스프링 스케줄러를 통한 주기적 실행과 이벤트 발행을 통해 도메인 간 통신을 수행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostScheduledService {
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final ApplicationEventPublisher eventPublisher;
    private final PostQueryRepository postQueryRepository;
    private final PostToCommentAdapter postToCommentAdapter;
    private final FeaturedPostRepository featuredPostRepository;
    private final PostRepository postRepository;

    /**
     * <h3>실시간 인기 게시글 점수 지수감쇠 적용</h3>
     * <p>스프링 스케줄러를 통해 10분마다 실시간 인기글 점수에 0.9를 곱하고, 1점 이하 게시글을 제거합니다.</p>
     * <p>Redis와 Caffeine 폴백 저장소 모두에 감쇠를 적용합니다.</p>
     */
    @Scheduled(fixedRate = 60000 * 10) // 10분마다
    public void applyRealtimeScoreDecay() {
        try {
            redisRealTimePostAdapter.applyRealtimePopularScoreDecay();
        } catch (Exception e) {
            log.error("Redis 실시간 인기글 점수 지수감쇠 적용 실패", e);
        }

        try {
            realtimeScoreFallbackStore.applyDecay();
        } catch (Exception e) {
            log.error("Fallback 저장소 지수감쇠 적용 실패", e);
        }
    }

    /**
     * <h3>주간 인기 게시글 스케줄링 갱신 및 알림 발행</h3>
     * <p>1일마다 주간 인기 게시글을 갱신하고 PostSimpleDetail을 Redis Hash에 저장합니다.</p>
     * <p>지난 7일간의 조회수와 좋아요 종합 점수를 기반으로 주간 인기 게시글을 선정합니다.</p>
     * <p>인기 게시글로 선정된 작성자에게 PostFeaturedEvent를 발행하여 알림을 전송합니다.</p>
     * <p>선정된 인기글은 featured_post 테이블에 영속화됩니다.</p>
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateWeeklyPopularPosts() {
        List<PostSimpleDetail> posts = postQueryRepository.findWeeklyPopularPosts();
        enrichPostsCommentCount(posts);

        if (posts.isEmpty()) {
            log.info("WEEKLY에 대한 인기 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        // DB에 특집 게시글 저장 (기존 WEEKLY 삭제 후 새로 저장)
        saveFeaturedPosts(posts, PostCacheFlag.WEEKLY);

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
    @Transactional
    public void updateLegendaryPosts() {
        List<PostSimpleDetail> posts = postQueryRepository.findLegendaryPosts();
        enrichPostsCommentCount(posts);

        if (posts.isEmpty()) {
            log.info("LEGEND에 대한 인기 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        // DB에 특집 게시글 저장 (기존 LEGEND 삭제 후 새로 저장)
        saveFeaturedPosts(posts, PostCacheFlag.LEGEND);

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

    /**
     * <h3>특집 게시글 DB 저장</h3>
     * <p>기존 해당 유형의 특집 게시글을 모두 삭제하고 새로운 목록을 저장합니다.</p>
     * <p>WEEKLY/LEGEND 스케줄러에서 호출됩니다.</p>
     *
     * @param posts 특집으로 선정된 게시글 목록
     * @param type  특집 유형 (WEEKLY, LEGEND)
     */
    private void saveFeaturedPosts(List<PostSimpleDetail> posts, PostCacheFlag type) {
        // 기존 특집 게시글 전체 삭제
        featuredPostRepository.deleteAllByType(type);

        // Post ID 목록 추출
        Set<Long> postIds = posts.stream()
                .map(PostSimpleDetail::getId)
                .collect(Collectors.toSet());

        // Post 엔티티 조회
        List<Post> postEntities = postRepository.findAllById(postIds);

        // FeaturedPost 엔티티 생성 및 저장
        List<FeaturedPost> featuredPosts = postEntities.stream()
                .map(post -> {
                    if (type == PostCacheFlag.WEEKLY) {
                        return FeaturedPost.createWeekly(post);
                    } else {
                        return FeaturedPost.createLegend(post);
                    }
                })
                .toList();

        featuredPostRepository.saveAll(featuredPosts);
        log.info("{} 특집 게시글 DB 저장 완료: {}개", type, featuredPosts.size());
    }

    /**
     * <h3>게시글 목록에 댓글 수 주입</h3>
     * <p>게시글 목록의 댓글 수를 배치로 조회하여 주입합니다.</p>
     * <p>좋아요 수는 PostQueryHelper에서 이미 처리되므로, 여기서는 댓글 수만 처리합니다.</p>
     *
     * @param posts 댓글 수를 채울 게시글 목록
     */
    private void enrichPostsCommentCount(List<PostSimpleDetail> posts) {
        if (posts.isEmpty()) {
            return;
        }

        List<Long> postIds = posts.stream()
                .map(PostSimpleDetail::getId)
                .toList();

        Map<Long, Integer> commentCounts = postToCommentAdapter.findCommentCountsByPostIds(postIds);

        posts.forEach(post -> {
            post.setCommentCount(commentCounts.getOrDefault(post.getId(), 0));
        });
    }
}