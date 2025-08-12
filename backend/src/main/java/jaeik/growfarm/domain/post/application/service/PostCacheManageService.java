package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.out.LoadPopularPostPort;
import jaeik.growfarm.domain.post.application.port.out.ManagePostCachePort;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheManageService {

    private final LoadPopularPostPort loadPopularPostPort;
    private final ManagePostCachePort managePostCachePort;
    private final ApplicationEventPublisher eventPublisher;


    @Scheduled(fixedRate = 60000 * 30) // 30분마다
    @Transactional
    public void updateRealtimePopularPosts() {
        loadPopularPostPort.resetPopularFlag(PostCacheFlag.REALTIME);
        List<SimplePostResDTO> posts = loadPopularPostPort.findRealtimePopularPosts();
        if (!posts.isEmpty()) {
            managePostCachePort.cachePosts(PostCacheFlag.REALTIME, posts);
            List<Long> postIds = posts.stream().map(SimplePostResDTO::getId).collect(Collectors.toList());
            loadPopularPostPort.applyPopularFlag(postIds, PostCacheFlag.REALTIME);
        }
    }

    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateWeeklyPopularPosts() {
        loadPopularPostPort.resetPopularFlag(PostCacheFlag.WEEKLY);
        List<SimplePostResDTO> posts = loadPopularPostPort.findWeeklyPopularPosts();
        if (!posts.isEmpty()) {
            managePostCachePort.cachePosts(PostCacheFlag.WEEKLY, posts);
            List<Long> postIds = posts.stream().map(SimplePostResDTO::getId).collect(Collectors.toList());
            loadPopularPostPort.applyPopularFlag(postIds, PostCacheFlag.WEEKLY);
            posts.forEach(post -> {
                if (post.getUserId() != null) {
                    eventPublisher.publishEvent(new PostFeaturedEvent(
                            this,
                            post.getUserId(),
                            "주간 인기 게시글로 선정되었어요!",
                            post.getId(),
                            "주간 인기 게시글 선정",
                            "회원님의 게시글 '" + post.getTitle() + "'이 주간 인기 게시글로 선정되었습니다."
                    ));
                }
            });
        }
    }
    
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateLegendaryPosts() {
        loadPopularPostPort.resetPopularFlag(PostCacheFlag.LEGEND);
        List<SimplePostResDTO> posts = loadPopularPostPort.findLegendaryPosts();
        if (!posts.isEmpty()) {
            managePostCachePort.cachePosts(PostCacheFlag.LEGEND, posts);
            List<Long> postIds = posts.stream().map(SimplePostResDTO::getId).collect(Collectors.toList());
            loadPopularPostPort.applyPopularFlag(postIds, PostCacheFlag.LEGEND);
            posts.forEach(post -> {
                if (post.getUserId() != null) {
                    eventPublisher.publishEvent(new PostFeaturedEvent(
                            this,
                            post.getUserId(),
                            "명예의 전당에 등극했어요!",
                            post.getId(),
                            "명예의 전당 등극",
                            "회원님의 게시글 '" + post.getTitle() + "'이 명예의 전당에 등극했습니다."
                    ));
                }
            });
        }
    }

    public void deleteNoticeCache() {
        managePostCachePort.deletePopularPostsCache(PostCacheFlag.NOTICE);
    }

    @Async
    @EventListener
    public void handlePostSetAsNotice(PostSetAsNoticeEvent event) {
        log.info("Post (ID: {}) set as notice event received. Deleting notice cache.", event.getPostId());
        deleteNoticeCache();
    }

    @Async
    @EventListener
    public void handlePostUnsetAsNotice(PostUnsetAsNoticeEvent event) {
        log.info("Post (ID: {}) unset as notice event received. Deleting notice cache.", event.getPostId());
        deleteNoticeCache();
    }
}
