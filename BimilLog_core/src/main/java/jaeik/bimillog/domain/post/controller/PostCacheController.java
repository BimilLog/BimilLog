package jaeik.bimillog.domain.post.controller;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.service.FeaturedPostCacheService;
import jaeik.bimillog.domain.post.service.RealtimePostCacheService;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>게시글 캐시 컨트롤러</h2>
 * <p>Post 도메인의 캐시 기반 조회를 담당하는 웹 어댑터입니다.</p>
 * <p>실시간, 주간, 레전드, 공지사항 카테고리별 조회 기능을 분리하여 제공합니다.</p>
 * <p>실시간, 주간 인기글 조회</p>
 * <p>레전드 인기글 조회</p>
 * <p>공지사항 조회</p>
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
    private final FeaturedPostCacheService featuredPostCacheService;

    /**
     * <h3>실시간 인기글 조회 API</h3>
     * <p>실시간 인기글로 선정된 게시글 목록을 페이징으로 조회한다.</p>
     *
     * @param pageable 페이지 정보
     * @return 실시간 인기글 페이지
     */
    @GetMapping("/realtime")
    public ResponseEntity<Page<PostSimpleDetail>> getRealtimePopularPosts(Pageable pageable) {
        Page<PostSimpleDetail> realtimePosts = realtimePostCacheService.getRealtimePosts(pageable);
        return ResponseEntity.ok(realtimePosts);
    }

    /**
     * <h3>주간 인기글 조회 API</h3>
     * <p>주간 인기글로 선정된 게시글 목록을 페이징으로 조회한다.</p>
     *
     * @param pageable 페이지 정보
     * @return 주간 인기글 페이지
     */
    @GetMapping("/weekly")
    public ResponseEntity<Page<PostSimpleDetail>> getWeeklyPopularPosts(Pageable pageable) {
        Page<PostSimpleDetail> weeklyPosts = featuredPostCacheService.getWeeklyPosts(pageable);
        return ResponseEntity.ok(weeklyPosts);
    }

    /**
     * <h3>레전드 인기글 조회 API (페이징)</h3>
     *
     * <p>
     * 레전드 인기글로 선정된 게시글 목록을 페이지네이션으로 조회한다.
     * </p>
     *
     * @param pageable 페이지 정보
     * @since 2.0.0
     * @author Jaeik
     * @return 레전드 게시글 목록 페이지
     */
    @GetMapping("/legend")
    public ResponseEntity<Page<PostSimpleDetail>> getLegendBoard(Pageable pageable) {
        Page<PostSimpleDetail> legendPopularPosts = featuredPostCacheService.getPopularPostLegend(pageable);
        return ResponseEntity.ok(legendPopularPosts);
    }

    /**
     * <h3>공지사항 조회 API</h3>
     * <p>공지사항으로 등록된 게시글 목록을 페이징으로 조회한다.</p>
     *
     * @param pageable 페이지 정보
     * @return 공지사항 게시글 페이지
     */
    @GetMapping("/notice")
    public ResponseEntity<Page<PostSimpleDetail>> getNoticeBoard(Pageable pageable) {
        Page<PostSimpleDetail> noticePosts = featuredPostCacheService.getNoticePosts(pageable);
        return ResponseEntity.ok(noticePosts);
    }
}
