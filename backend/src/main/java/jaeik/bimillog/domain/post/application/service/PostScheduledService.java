package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.entity.PopularPostInfo;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>PostScheduledService</h2>
 * <p>게시글 인기도 기반 캐시 동기화를 담당하는 스케줄링 서비스로서 시간 기반 캐시 갱신 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>스프링 스케줄러를 통한 주기적 실행과 이벤트 발행을 통해 도메인 간 통신을 수행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostScheduledService {

    private final RedisPostCommandPort redisPostCommandPort;
    private final ApplicationEventPublisher eventPublisher;
    private final PostQueryPort postQueryPort;

    /**
     * <h3>실시간 인기 게시글 점수 지수감쇠 적용</h3>
     * <p>스프링 스케줄러를 통해 5분마다 실시간 인기글 점수에 0.9를 곱하고, 1점 이하 게시글을 제거합니다.</p>
     * <p>이벤트 기반 점수 시스템으로 전환됨에 따라 RDB 조회 대신 Redis 점수 감쇠만 수행합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 5) // 5분마다
    public void applyRealtimeScoreDecay() {
        try {
            redisPostCommandPort.applyRealtimePopularScoreDecay();
            log.info("실시간 인기글 점수 지수감쇠 적용 완료 (0.9 곱하기, 1점 이하 제거)");
        } catch (Exception e) {
            log.error("실시간 인기글 점수 지수감쇠 적용 실패", e);
        }
    }

    /**
     * <h3>주간 인기 게시글 스케줄링 갱신 및 알림 발행</h3>
     * <p>1일마다 주간 인기 게시글을 갱신하고 postId를 Redis 캐시에 저장합니다.</p>
     * <p>지난 7일간의 조회수와 좋아요 종합 점수를 기반으로 주간 인기 게시글을 선정합니다.</p>
     * <p>인기 게시글로 선정된 작성자에게 PostFeaturedEvent를 발행하여 알림을 전송합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateWeeklyPopularPosts() {
        List<PopularPostInfo> posts = postQueryPort.findWeeklyPopularPosts();
        if (posts.isEmpty()) {
            log.info("WEEKLY에 대한 인기 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        // postId 목록만 캐시 (메모리 효율 향상)
        List<Long> postIds = posts.stream().map(PopularPostInfo::postId).toList();
        redisPostCommandPort.cachePostIds(PostCacheFlag.WEEKLY, postIds);
        log.info("WEEKLY 캐시 업데이트 완료. {}개의 게시글 ID가 처리됨", posts.size());

        // 알림 발행
        publishFeaturedEvent(posts, "주간 인기 게시글로 선정되었어요!", "주간 인기 게시글 선정",
                "회원님의 게시글 %s 이 주간 인기 게시글로 선정되었습니다.");
    }

    /**
     * <h3>전설 게시글 스케줄링 갱신 및 명예의 전당 알림 발행</h3>
     * <p>postId 목록만 Redis에 저장</p>
     * <p>전설 게시글로 선정된 작성자에게 PostFeaturedEvent를 발행하여 알림을 전송합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateLegendaryPosts() {
        List<PopularPostInfo> posts = postQueryPort.findLegendaryPosts();
        if (posts.isEmpty()) {
            log.info("LEGEND에 대한 인기 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        // postId 목록만 캐시 (메모리 효율 향상)
        List<Long> postIds = posts.stream().map(PopularPostInfo::postId).toList();
        redisPostCommandPort.cachePostIds(PostCacheFlag.LEGEND, postIds);
        log.info("LEGEND 캐시 업데이트 완료. {}개의 게시글 ID가 처리됨", posts.size());

        // 알림 발행
        publishFeaturedEvent(posts, "명예의 전당에 등극했어요!", "명예의 전당 등극",
                "회원님의 게시글 %s 이 명예의 전당에 등극했습니다.");
    }

    /**
     * <h3>인기 게시글 알림 이벤트 발행</h3>
     * <p>인기 게시글로 선정된 사용자에게 알림 이벤트를 발행합니다.</p>
     *
     * @param posts 알림을 보낼 게시글 목록
     * @param notiTitle 알림 제목
     * @param eventTitle 이벤트 제목
     * @param eventBodyFormat 이벤트 본문 포맷 문자열 (게시글 제목을 %s로 사용)
     */
    private void publishFeaturedEvent(List<PopularPostInfo> posts, String notiTitle, String eventTitle, String eventBodyFormat) {
        posts.stream()
                .filter(post -> post.memberId() != null)
                .forEach(post -> {
                    String eventBody = String.format(eventBodyFormat, post.title());
                    eventPublisher.publishEvent(new PostFeaturedEvent(
                            post.memberId(),
                            notiTitle,
                            post.postId(),
                            eventTitle,
                            eventBody
                    ));
                    log.info("게시글 ID {}에 대한 인기글 알림 이벤트 발행: 회원 ID={}", post.postId(), post.memberId());
                });
    }
}
