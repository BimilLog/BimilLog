package jaeik.growfarm.infrastructure.adapter.post.in.web;

import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/post")
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
        List<SimplePostResDTO> realtimePopularPosts = postQueryUseCase.getPopularPosts(PostCacheFlag.REALTIME)
                .stream()
                .map(SimplePostResDTO::from)
                .toList();
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
        List<SimplePostResDTO> weeklyPopularPosts = postQueryUseCase.getPopularPosts(PostCacheFlag.WEEKLY)
                .stream()
                .map(SimplePostResDTO::from)
                .toList();
        return ResponseEntity.ok(weeklyPopularPosts);
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
    public ResponseEntity<Page<SimplePostResDTO>> getLegendBoard(Pageable pageable) {
        Page<SimplePostResDTO> legendPopularPosts = postQueryUseCase.getPopularPostLegend(PostCacheFlag.LEGEND, pageable)
                .map(SimplePostResDTO::from);
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
        List<SimplePostResDTO> noticePosts = postQueryUseCase.getNoticePosts()
                .stream()
                .map(SimplePostResDTO::from)
                .toList();
        return ResponseEntity.ok(noticePosts);
    }
}