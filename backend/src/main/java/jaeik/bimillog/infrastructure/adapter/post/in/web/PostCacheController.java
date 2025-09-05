package jaeik.bimillog.infrastructure.adapter.post.in.web;

import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <h2>게시글 캐시 컨트롤러</h2>
 * <p>캐시된 인기 게시글 조회 관련 REST API를 제공하는 웹 어댑터입니다.</p>
 * <p>실시간, 주간, 레전드, 공지사항 게시글 조회 기능을 담당합니다.</p>
 * <p>헥사고널 아키텍처 Primary Adapter</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostCacheController {

    private final PostQueryUseCase postQueryUseCase;
    private final PostResponseMapper postResponseMapper;

    /**
     * <h3>실시간, 주간 인기글 조회 API</h3>
     *
     * <p>
     * 실시간, 주간 인기글로 선정된 게시글 목록을 조회한다.
     * 성능 최적화를 위해 한 번의 유스케이스 호출로 두 타입의 데이터를 가져온다.
     * </p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return 실시간, 주간 인기글 목록
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, List<SimplePostResDTO>>> getPopularBoard() {
        Map<String, List<PostSearchResult>> popularPosts = postQueryUseCase.getRealtimeAndWeeklyPosts();
        
        // DTO 변환
        Map<String, List<SimplePostResDTO>> result = Map.of(
            "realtime", popularPosts.get("realtime").stream()
                .map(postResponseMapper::convertToSimplePostResDTO)
                .toList(),
            "weekly", popularPosts.get("weekly").stream()
                .map(postResponseMapper::convertToSimplePostResDTO)
                .toList()
        );
        
        return ResponseEntity.ok(result);
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
        Page<PostSearchResult> legendPopularPosts = postQueryUseCase.getPopularPostLegend(PostCacheFlag.LEGEND, pageable);
        Page<SimplePostResDTO> dtoList = legendPopularPosts.map(postResponseMapper::convertToSimplePostResDTO);
        return ResponseEntity.ok(dtoList);
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
                .map(postResponseMapper::convertToSimplePostResDTO)
                .toList();
        return ResponseEntity.ok(noticePosts);
    }
}