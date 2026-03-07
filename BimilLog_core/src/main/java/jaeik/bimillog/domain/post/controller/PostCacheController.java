package jaeik.bimillog.domain.post.controller;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.service.PostPopularService;
import jaeik.bimillog.domain.post.service.RealtimePostCacheService;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>게시글 캐시 컨트롤러</h2>
 * <p>Post 도메인의 캐시 기반 조회를 담당하는 어댑터입니다.</p>
 * <p>실시간, 주간, 레전드, 공지사항 목록 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostCacheController {
    private final RealtimePostCacheService realtimePostCacheService;
    private final PostPopularService postPopularService;

    /**
     * <h3>실시간 인기글 조회 API</h3>
     * <p>실시간 인기글로 선정된 게시글 목록을 조회한다.</p>
     *
     * @return 실시간 인기글 페이지
     */
    @GetMapping("/realtime")
    public ResponseEntity<Page<PostSimpleDetail>> getRealtimePopularPosts() {
        Page<PostSimpleDetail> realtimePosts = realtimePostCacheService.getRealtimePosts();
        return ResponseEntity.ok(realtimePosts);
    }

    /**
     * <h3>주간 인기글 조회 API</h3>
     * <p>주간 인기글로 선정된 게시글 목록을 조회한다.</p>
     *
     * @return 주간 인기글 페이지
     */
    @GetMapping("/weekly")
    public ResponseEntity<Page<PostSimpleDetail>> getWeeklyPopularPosts() {
        Page<PostSimpleDetail> weeklyPosts = postPopularService.getPopularPosts(RedisKey.POST_WEEKLY_JSON_KEY, PostQueryType.WEEKLY);
        return ResponseEntity.ok(weeklyPosts);
    }

    /**
     * <h3>레전드 인기글 조회 API</h3>
     * <p>레전드 인기글로 선정된 게시글 목록을 조회한다.</p>
     *
     * @return 레전드 게시글 목록 페이지
     */
    @GetMapping("/legend")
    public ResponseEntity<Page<PostSimpleDetail>> getLegendBoard() {
        Page<PostSimpleDetail> legendPopularPosts = postPopularService.getPopularPosts(RedisKey.POST_LEGEND_JSON_KEY, PostQueryType.LEGEND);
        return ResponseEntity.ok(legendPopularPosts);
    }

    /**
     * <h3>공지사항 조회 API</h3>
     * <p>공지사항으로 등록된 게시글 목록을 조회한다.</p>
     *
     * @return 공지사항 게시글 페이지
     */
    @GetMapping("/notice")
    public ResponseEntity<Page<PostSimpleDetail>> getNoticeBoard() {
        Page<PostSimpleDetail> noticePosts = postPopularService.getPopularPosts(RedisKey.POST_NOTICE_JSON_KEY, PostQueryType.NOTICE);
        return ResponseEntity.ok(noticePosts);
    }
}
