package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.out.LoadPopularPostPort;
import jaeik.growfarm.domain.post.application.port.out.ManagePostCachePort;
import jaeik.growfarm.domain.post.domain.PostCacheFlag;
import jaeik.growfarm.dto.post.SimplePostResDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostCacheManageService {

    private final LoadPopularPostPort loadPopularPostPort;
    private final ManagePostCachePort managePostCachePort;


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
            // 각 게시글의 작성자에게 알림을 보내려면 게시글 정보를 조회해야 함
            // 현재는 간단한 형태로 구현
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
            // 각 게시글의 작성자에게 알림을 보내려면 게시글 정보를 조회해야 함
            // 현재는 간단한 형태로 구현
        }
    }

    public void deleteNoticeCache() {
        managePostCachePort.deletePopularPostsCache(PostCacheFlag.NOTICE);
    }
}
