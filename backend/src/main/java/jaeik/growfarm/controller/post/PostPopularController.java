package jaeik.growfarm.controller.post;

import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.service.redis.RedisPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>게시글 인기글 컨트롤러</h2>
 * <p>
 * 인기글 조회 기능만을 담당 (실시간, 주간, 레전드)
 * </p>
 * 
 * @author Jaeik
 * @version 1.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post/popular")
public class PostPopularController {

    private final RedisPostService redisPostService;

    /**
     * <h3>실시간 인기글 조회 API</h3>
     *
     * <p>
     * 실시간 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     *
     * @since 1.1.0
     * @author Jaeik
     * @return 실시간 인기글 목록
     */
    @GetMapping("/realtime")
    public ResponseEntity<List<SimplePostDTO>> getRealtimeBoard() {
        List<SimplePostDTO> realtimePopularPosts = redisPostService
                .getCachedPopularPosts(RedisPostService.PopularPostType.REALTIME);
        return ResponseEntity.ok(realtimePopularPosts);
    }

    /**
     * <h3>주간 인기글 조회 API</h3>
     *
     * <p>
     * 주간 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     *
     * @since 1.1.0
     * @author Jaeik
     * @return 주간 인기글 목록
     */
    @GetMapping("/weekly")
    public ResponseEntity<List<SimplePostDTO>> getWeeklyBoard() {
        List<SimplePostDTO> weeklyPopularPosts = redisPostService
                .getCachedPopularPosts(RedisPostService.PopularPostType.WEEKLY);
        return ResponseEntity.ok(weeklyPopularPosts);
    }

    /**
     * <h3>레전드 인기글 조회 API</h3>
     *
     * <p>
     * 레전드 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     *
     * @since 1.1.0
     * @author Jaeik
     * @return 레전드 게시글 목록
     */
    @GetMapping("/legend")
    public ResponseEntity<List<SimplePostDTO>> getLegendBoard() {
        List<SimplePostDTO> legendPopularPosts = redisPostService
                .getCachedPopularPosts(RedisPostService.PopularPostType.LEGEND);
        return ResponseEntity.ok(legendPopularPosts);
    }
}