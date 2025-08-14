package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.growfarm.domain.post.application.port.out.PostCacheSyncPort;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.event.PostFeaturedEvent;
import jaeik.growfarm.global.event.PostSetAsNoticeEvent;
import jaeik.growfarm.global.event.PostUnsetAsNoticeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
     * <p>이전 플래그를 초기화하고 새로운 인기 게시글에 플래그를 적용합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 30) // 30분마다
    @Transactional
    public void updateRealtimePopularPosts() {
        postCacheCommandPort.resetPopularFlag(PostCacheFlag.REALTIME);
        List<SimplePostResDTO> posts = postCacheSyncPort.findRealtimePopularPosts();
        if (!posts.isEmpty()) {
            postCacheCommandPort.cachePosts(PostCacheFlag.REALTIME, posts);
            List<Long> postIds = posts.stream().map(SimplePostResDTO::getId).collect(Collectors.toList());
            postCacheCommandPort.applyPopularFlag(postIds, PostCacheFlag.REALTIME);
            
            // 인기글의 상세 정보도 함께 캐싱
            for (SimplePostResDTO post : posts) {
                FullPostResDTO fullPost = postCacheSyncPort.findPostDetail(post.getId());
                if (fullPost != null) {
                    postCacheCommandPort.cacheFullPost(fullPost);
                }
            }
        }
    }

    /**
     * <h3>주간 인기 게시글 업데이트</h3>
     * <p>1일마다 주간 인기 게시글을 업데이트하고 캐시하며, 관련 사용자에게 알림 이벤트를 발행합니다.</p>
     * <p>이전 플래그를 초기화하고 새로운 인기 게시글에 플래그를 적용합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateWeeklyPopularPosts() {
        postCacheCommandPort.resetPopularFlag(PostCacheFlag.WEEKLY);
        List<SimplePostResDTO> posts = postCacheSyncPort.findWeeklyPopularPosts();
        if (!posts.isEmpty()) {
            postCacheCommandPort.cachePosts(PostCacheFlag.WEEKLY, posts);
            List<Long> postIds = posts.stream().map(SimplePostResDTO::getId).collect(Collectors.toList());
            postCacheCommandPort.applyPopularFlag(postIds, PostCacheFlag.WEEKLY);
            
            // 인기글의 상세 정보도 함께 캐싱
            for (SimplePostResDTO post : posts) {
                FullPostResDTO fullPost = postCacheSyncPort.findPostDetail(post.getId());
                if (fullPost != null) {
                    postCacheCommandPort.cacheFullPost(fullPost);
                }
            }
            
            posts.forEach(post -> {
                if (post.getUserId() != null) {
                    eventPublisher.publishEvent(new PostFeaturedEvent(
                            this,
                            post.getUserId(),
                            "주간 인기 게시글로 선정되었어요!",
                            post.getId(),
                            "주간 인기 게시글 선정",
                            "회원님의 게시글 \'" + post.getTitle() + "\'이 주간 인기 게시글로 선정되었습니다."
                    ));
                }
            });
        }
    }
    
    /**
     * <h3>전설의 게시글 업데이트</h3>
     * <p>1일마다 전설의 게시글을 업데이트하고 캐시하며, 관련 사용자에게 알림 이벤트를 발행합니다.</p>
     * <p>이전 플래그를 초기화하고 새로운 전설의 게시글에 플래그를 적용합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateLegendaryPosts() {
        postCacheCommandPort.resetPopularFlag(PostCacheFlag.LEGEND);
        List<SimplePostResDTO> posts = postCacheSyncPort.findLegendaryPosts();
        if (!posts.isEmpty()) {
            postCacheCommandPort.cachePosts(PostCacheFlag.LEGEND, posts);
            List<Long> postIds = posts.stream().map(SimplePostResDTO::getId).collect(Collectors.toList());
            postCacheCommandPort.applyPopularFlag(postIds, PostCacheFlag.LEGEND);
            
            // 인기글의 상세 정보도 함께 캐싱
            for (SimplePostResDTO post : posts) {
                FullPostResDTO fullPost = postCacheSyncPort.findPostDetail(post.getId());
                if (fullPost != null) {
                    postCacheCommandPort.cacheFullPost(fullPost);
                }
            }
            
            posts.forEach(post -> {
                if (post.getUserId() != null) {
                    eventPublisher.publishEvent(new PostFeaturedEvent(
                            this,
                            post.getUserId(),
                            "명예의 전당에 등극했어요!",
                            post.getId(),
                            "명예의 전당 등극",
                            "회원님의 게시글 \'" + post.getTitle() + "\'이 명예의 전당에 등극했습니다."
                    ));
                }
            });
        }
    }

    /**
     * <h3>공지 캐시 삭제</h3>
     * <p>공지사항 관련 캐시를 삭제합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void deleteNoticeCache() {
        postCacheCommandPort.deletePopularPostsCache(PostCacheFlag.NOTICE);
    }

    /**
     * <h3>게시글 공지 설정 이벤트 처리</h3>
     * <p>게시글이 공지로 설정되었을 때 공지 캐시를 삭제합니다.</p>
     *
     * @param event 게시글 공지 설정 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    public void handlePostSetAsNotice(PostSetAsNoticeEvent event) {
        log.info("Post (ID: {}) set as notice event received. Deleting notice cache.", event.postId());
        deleteNoticeCache();
    }

    /**
     * <h3>게시글 공지 해제 이벤트 처리</h3>
     * <p>게시글의 공지 설정이 해제되었을 때 공지 캐시를 삭제합니다.</p>
     *
     * @param event 게시글 공지 해제 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    public void handlePostUnsetAsNotice(PostUnsetAsNoticeEvent event) {
        log.info("Post (ID: {}) unset as notice event received. Deleting notice cache.", event.postId());
        deleteNoticeCache();
    }
}
