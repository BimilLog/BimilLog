package jaeik.growfarm.infrastructure.adapter.post.in.web;

import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>게시글 캐시 컨트롤러</h2>
 * <p>
 * 캐싱을 담당 (실시간, 주간, 레전드, 공지사항)
 * </p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post/cache")
public class PostCacheController {

    private final PostQueryUseCase postQueryUseCase;

    /**
     * <h3>실시간 인기글 조회 API</h3>
     *
     * <p>
     * 실시간 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return 실시간 인기글 목록
     */
    @GetMapping("/realtime")
    public ResponseEntity<List<SimplePostResDTO>> getRealtimeBoard() {
        List<SimplePostResDTO> realtimePopularPosts = postQueryUseCase.getPopularPosts(PostCacheFlag.REALTIME);
        return ResponseEntity.ok(realtimePopularPosts);
    }

    /**
     * <h3>주간 인기글 조회 API</h3>
     *
     * <p>
     * 주간 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return 주간 인기글 목록
     */
    @GetMapping("/weekly")
    public ResponseEntity<List<SimplePostResDTO>> getWeeklyBoard() {
        List<SimplePostResDTO> weeklyPopularPosts = postQueryUseCase.getPopularPosts(PostCacheFlag.WEEKLY);
        return ResponseEntity.ok(weeklyPopularPosts);
    }

    /**
     * <h3>레전드 인기글 조회 API</h3>
     *
     * <p>
     * 레전드 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return 레전드 게시글 목록
     */
    @GetMapping("/legend")
    public ResponseEntity<List<SimplePostResDTO>> getLegendBoard() {
        List<SimplePostResDTO> legendPopularPosts = postQueryUseCase.getPopularPosts(PostCacheFlag.LEGEND);
        return ResponseEntity.ok(legendPopularPosts);
    }

    /**
     * <h3>공지사항 조회 API</h3>
     *
     * <p>
     * 공지사항으로 등록된 게시글 목록을 조회한다.
     * </p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return 공지사항 게시글 목록
     */
    @GetMapping("/notice")
    public ResponseEntity<List<SimplePostResDTO>> getNoticeBoard() {
        List<SimplePostResDTO> noticePosts = postQueryUseCase.getNoticePosts();
        return ResponseEntity.ok(noticePosts);
    }
}