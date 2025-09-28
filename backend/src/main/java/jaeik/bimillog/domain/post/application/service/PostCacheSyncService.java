package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostSyncPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <h2>PostCacheSyncService</h2>
 * <p>게시글 인기도 기반 캐시 동기화를 담당하는 스케줄링 서비스로서 시간 기반 캐시 갱신 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>스프링 스케줄러를 통한 주기적 실행과 이벤트 발행을 통해 도메인 간 통신을 수행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheSyncService {

    private final RedisPostCommandPort redisPostCommandPort;
    private final RedisPostSyncPort redisPostSyncPort;
    private final ApplicationEventPublisher eventPublisher;
    private final PostQueryPort postQueryPort;

    /**
     * <h3>실시간 인기 게시글 스케줄링 갱신</h3>
     * <p>스프링 스케줄러를 통해 30분마다 실시간 인기 게시글을 갱신하고 Redis 캐시에 저장합니다.</p>
     * <p>PostQueryService에서 실시간 인기 게시글 조회 시 이 캐시를 활용합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 30) // 30분마다
    @Transactional
    public void updateRealtimePopularPosts() {
        processPopularPosts(
                PostCacheFlag.REALTIME,
                redisPostSyncPort::findRealtimePopularPosts
        );
    }

    /**
     * <h3>주간 인기 게시글 스케줄링 갱신 및 알림 발행</h3>
     * <p>스프링 스케줄러를 통해 1일마다 주간 인기 게시글을 갱신하고 Redis 캐시에 저장합니다.</p>
     * <p>지난 7일간의 조회수와 좋아요 종합 점수를 기반으로 주간 인기 게시글을 선정합니다.</p>
     * <p>인기 게시글로 선정된 작성자에게 PostFeaturedEvent를 발행하여 알림을 전송합니다.</p>
     * <p>PostQueryService에서 주간 인기 게시글 조회 시와 Notification 도메인에서 알림 발송 시 사용됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateWeeklyPopularPosts() {
        List<PostSearchResult> posts = redisPostSyncPort.findWeeklyPopularPosts();
        processPopularPosts(PostCacheFlag.WEEKLY, () -> posts);
        publishFeaturedEvent(posts, "주간 인기 게시글로 선정되었어요!", "주간 인기 게시글 선정",
                "회원님의 게시글 %s 이 주간 인기 게시글로 선정되었습니다.");
    }

    /**
     * <h3>전설 게시글 스케줄링 갱신 및 명예의 전당 알림 발행</h3>
     * <p>스프링 스케줄러를 통해 1일마다 전설 게시글을 갱신하고 Redis 캐시에 저장합니다.</p>
     * <p>역대 최고 조회수와 좋아요를 기록한 레전드급 게시글을 선정하여 명예의 전당으로 관리합니다.</p>
     * <p>전설 게시글로 선정된 작성자에게 PostFeaturedEvent를 발행하여 특별한 명예 알림을 전송합니다.</p>
     * <p>PostQueryService에서 전설 게시글 조회 시와 Notification 도메인에서 명예 알림 발송 시 사용됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateLegendaryPosts() {
        List<PostSearchResult> posts = redisPostSyncPort.findLegendaryPosts();
        processPopularPosts(PostCacheFlag.LEGEND, () -> posts);
        publishFeaturedEvent(posts, "명예의 전당에 등극했어요!", "명예의 전당 등극",
                "회원님의 게시글 %s 이 명예의 전당에 등극했습니다.");
    }

    /**
     * <h3>인기 게시글 처리 공통 로직</h3>
     * <p>인기 게시글을 찾아 플래그를 적용하고, 상세 정보를 캐시합니다.</p>
     *
     * @param flag 인기 게시글 유형 플래그
     * @param postFinder 인기 게시글 목록을 찾는 메서드
     */
    private void processPopularPosts(PostCacheFlag flag, Supplier<List<PostSearchResult>> postFinder) {
        redisPostCommandPort.resetPopularFlag(flag);
        List<PostSearchResult> posts = postFinder.get();

        if (posts.isEmpty()) {
            log.info("{}에 대한 인기 게시글이 없어 캐시 업데이트를 건너뜁니다.", flag.name());
            return;
        }

        List<Long> postIds = posts.stream().map(PostSearchResult::getId).collect(Collectors.toList());
        redisPostCommandPort.applyPopularFlag(postIds, flag);

        List<PostDetail> fullPosts = posts.stream()
                .map(post -> postQueryPort.findPostDetail(post.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        redisPostCommandPort.cachePostsWithDetails(flag, fullPosts);
        log.info("{} 캐시 업데이트 완료. {}개의 게시글이 처리됨", flag.name(), fullPosts.size());
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
    private void publishFeaturedEvent(List<PostSearchResult> posts, String notiTitle, String eventTitle, String eventBodyFormat) {
        posts.stream()
                .filter(post -> post.getUserId() != null)
                .forEach(post -> {
                    String eventBody = String.format(eventBodyFormat, post.getTitle());
                    eventPublisher.publishEvent(new PostFeaturedEvent(
                            post.getUserId(),
                            notiTitle,
                            post.getId(),
                            eventTitle,
                            eventBody
                    ));
                    log.info("게시글 ID {}에 대한 인기글 알림 이벤트 발행: 사용자 ID={}", post.getId(), post.getUserId());
                });
    }
}
