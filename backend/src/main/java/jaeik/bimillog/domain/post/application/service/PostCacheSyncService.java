package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCacheSyncPort;
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
 * <h2>게시글 캐시 동기화 서비스</h2>
 * <p>게시글 관련 캐시 데이터를 주기적으로 업데이트하고 이벤트에 따라 캐시를 무효화하는 서비스 클래스입니다.</p>
 * <p>주로 인기 게시글 및 공지사항 캐시를 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheSyncService {

    private final PostCacheCommandPort postCacheCommandPort;
    private final PostCacheSyncPort postCacheSyncPort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>실시간 인기 게시글 업데이트</h3>
     * <p>30분마다 실시간 인기 게시글을 업데이트하고 캐시합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 30) // 30분마다
    @Transactional
    public void updateRealtimePopularPosts() {
        processPopularPosts(
                PostCacheFlag.REALTIME,
                postCacheSyncPort::findRealtimePopularPosts
        );
    }

    /**
     * <h3>주간 인기 게시글 업데이트</h3>
     * <p>1일마다 주간 인기 게시글을 업데이트하고 캐시하며, 관련 사용자에게 알림 이벤트를 발행합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateWeeklyPopularPosts() {
        List<PostSearchResult> posts = postCacheSyncPort.findWeeklyPopularPosts();
        processPopularPosts(PostCacheFlag.WEEKLY, () -> posts);
        publishFeaturedEvent(posts, "주간 인기 게시글로 선정되었어요!", "주간 인기 게시글 선정",
                "회원님의 게시글 %s 이 주간 인기 게시글로 선정되었습니다.");
    }

    /**
     * <h3>전설의 게시글 업데이트</h3>
     * <p>1일마다 전설의 게시글을 업데이트하고 캐시하며, 관련 사용자에게 알림 이벤트를 발행합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateLegendaryPosts() {
        List<PostSearchResult> posts = postCacheSyncPort.findLegendaryPosts();
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
        postCacheCommandPort.resetPopularFlag(flag);
        List<PostSearchResult> posts = postFinder.get();

        if (posts.isEmpty()) {
            log.info("{}에 대한 인기 게시글이 없어 캐시 업데이트를 건너뜁니다.", flag.name());
            return;
        }

        List<Long> postIds = posts.stream().map(PostSearchResult::getId).collect(Collectors.toList());
        postCacheCommandPort.applyPopularFlag(postIds, flag);

        List<PostDetail> fullPosts = posts.stream()
                .map(post -> postCacheSyncPort.findPostDetail(post.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        postCacheCommandPort.cachePostsWithDetails(flag, fullPosts);
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
