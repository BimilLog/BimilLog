package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.out.PostQueryRepository;
import jaeik.bimillog.domain.post.out.PostToCommentAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier1PostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostStoreAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
    private final RedisTier1PostStoreAdapter redisTier1PostStoreAdapter;
    private final RedisTier2PostStoreAdapter redisTier2PostStoreAdapter;
    private final RedisRealTimePostStoreAdapter redisRealTimePostStoreAdapter;
    private final ApplicationEventPublisher eventPublisher;
    private final PostQueryRepository postQueryRepository;
    private final PostToCommentAdapter postToCommentAdapter;

    /**
     * <h3>실시간 인기 게시글 점수 지수감쇠 적용</h3>
     * <p>스프링 스케줄러를 통해 5분마다 실시간 인기글 점수에 0.97를 곱하고, 1점 이하 게시글을 제거합니다.</p>
     */
    @Scheduled(fixedRate = 60000 * 10) // 10분마다
    public void applyRealtimeScoreDecay() {
        try {
            redisRealTimePostStoreAdapter.applyRealtimePopularScoreDecay();
            log.info("실시간 인기글 점수 지수감쇠 적용 완료 (0.97 곱하기, 1점 이하 제거)");
        } catch (Exception e) {
            log.error("실시간 인기글 점수 지수감쇠 적용 실패", e);
        }
    }

    /**
     * <h3>주간 인기 게시글 스케줄링 갱신 및 알림 발행</h3>
     * <p>1일마다 주간 인기 게시글을 갱신하고 PostSimpleDetail을 Redis Hash에 저장합니다.</p>
     * <p>지난 7일간의 조회수와 좋아요 종합 점수를 기반으로 주간 인기 게시글을 선정합니다.</p>
     * <p>인기 게시글로 선정된 작성자에게 PostFeaturedEvent를 발행하여 알림을 전송합니다.</p>
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

        List<Long> postIds = posts.stream().map(PostSimpleDetail::getId).toList();

        try {
            redisTier1PostStoreAdapter.clearPostListCache(PostCacheFlag.WEEKLY);
            redisTier2PostStoreAdapter.cachePostIdsOnly(PostCacheFlag.WEEKLY, postIds);
            redisTier1PostStoreAdapter.cachePostList(PostCacheFlag.WEEKLY, posts);
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

        List<Long> postIds = posts.stream().map(PostSimpleDetail::getId).toList();

        try {
            redisTier1PostStoreAdapter.clearPostListCache(PostCacheFlag.LEGEND);
            redisTier2PostStoreAdapter.cachePostIdsOnly(PostCacheFlag.LEGEND, postIds);
            redisTier1PostStoreAdapter.cachePostList(PostCacheFlag.LEGEND, posts);
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
