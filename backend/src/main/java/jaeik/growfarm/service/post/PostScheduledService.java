package jaeik.growfarm.service.post;

import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.global.event.PostFeaturedEvent;
import jaeik.growfarm.repository.post.popular.PostPopularRepository;
import jaeik.growfarm.service.redis.RedisPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>게시글 스케줄러 서비스</h2>
 * <p>
 * 인기글 선정 스케줄링 작업을 담당하는 서비스 클래스
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 * @version 1.1.0
 */
@Service
@RequiredArgsConstructor
public class PostScheduledService {

    private final ApplicationEventPublisher eventPublisher;
    private final RedisPostService redisPostService;
    private final PostPopularRepository postPopularRepository;


    /**
     * <h3>실시간 인기글 선정</h3>
     * <p>
     * 1일 이내의 글 중 추천 수가 가장 높은 상위 5개를 실시간 인기글로 등록한다.
     * </p>
     * <p>
     * redis에 캐시한다.
     * </p>
     * <p>
     * 30분마다 시행한다.
     * </p>
     *
     * @author Jaeik
     * @since 1.1.0
     */
    @Scheduled(fixedRate = 60000 * 30)
    public void updateRealtimePopularPosts() {
        List<SimplePostDTO> realtimePosts = postPopularRepository.updateRealtimePopularPosts();
        redisPostService.cachePopularPosts(RedisPostService.PopularPostType.REALTIME, realtimePosts);
    }

    /**
     * <h3>주간 인기글 선정</h3>
     * <p>
     * 7일 이내의 글 중 추천 수가 가장 높은 상위 5개를 주간 인기글로 등록한다.
     * </p>
     * <p>
     * redis에 캐시한다.
     * </p>
     * <p>
     * 1일마다 시행한다.
     * </p>
     *
     * @author Jaeik
     * @since 1.1.0
     */
    @Scheduled(fixedRate = 60000 * 1440)
    public void updateWeeklyPopularPosts() {
        List<SimplePostDTO> weeklyPosts = postPopularRepository.updateWeeklyPopularPosts();
        redisPostService.cachePopularPosts(RedisPostService.PopularPostType.WEEKLY, weeklyPosts);

        for (SimplePostDTO simplePostDTO : weeklyPosts) {
            if (simplePostDTO.getUser() != null) {
                eventPublisher.publishEvent(new PostFeaturedEvent(
                        simplePostDTO.getUserId(),
                        "🎉 회원님의 글이 주간 인기글로 선정되었습니다!",
                        simplePostDTO.getPostId(),
                        "회원님의 글이 주간 인기글로 선정되었습니다!",
                        "지금 확인해보세요!"));
            }
        }
    }

    /**
     * <h3>레전드 게시글 선정</h3>
     * <p>
     * 추천 수가 20개 이상인 글을 선정한다.
     * </p>
     *
     * <p>
     * redis에 캐시한다.
     * </p>
     * <p>
     * 1일마다 시행한다.
     * </p>
     *
     * @author Jaeik
     * @since 1.1.0
     */
    @Scheduled(fixedRate = 60000 * 1440)
    public void updateLegendPopularPosts() {
        List<SimplePostDTO> legendPosts = postPopularRepository.updateLegendPosts();
        redisPostService.cachePopularPosts(RedisPostService.PopularPostType.LEGEND, legendPosts);

        for (SimplePostDTO simplePostDTO : legendPosts) {
            eventPublisher.publishEvent(new PostFeaturedEvent(
                    simplePostDTO.getUserId(),
                    "🎉 회원님의 글이 레전드글로 선정되었습니다!",
                    simplePostDTO.getPostId(),
                    "회원님의 글이 레전드글로 선정되었습니다!",
                    "지금 확인해보세요!"));
        }
    }
}
