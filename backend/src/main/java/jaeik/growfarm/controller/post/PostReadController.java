package jaeik.growfarm.controller.post;

import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.post.read.PostReadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>게시글 기본 조회 컨트롤러</h2>
 * <p>
 * 게시글의 기본 조회 기능을 담당 (목록 조회, 상세 조회)
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post/query")
public class PostReadController {

    private final PostReadService postReadService;

    /**
     * <h3>게시판 조회 API</h3>
     *
     * <p>
     * 최신순으로 게시글 목록을 페이지네이션으로 조회한다.
     * </p>
     *
     * @param page 페이지 번호
     * @param size 페이지 사이즈
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 1.1.0
     */
    @GetMapping
    public ResponseEntity<Page<SimplePostResDTO>> getBoard(@RequestParam int page, @RequestParam int size) {
        Page<SimplePostResDTO> postList = postReadService.getBoard(page, size);
        return ResponseEntity.ok(postList);
    }

    /**
     * <h3>게시글 조회 API</h3>
     *
     * <p>
     * 게시글 ID를 통해 게시글을 조회한다.
     * </p>
     *
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보 (선택)
     * @param request     HTTP 요청 객체
     * @param response    HTTP 응답 객체
     * @return 게시글 정보
     * @author Jaeik
     * @since 1.1.0
     */
    @GetMapping("/{postId}")
    public ResponseEntity<FullPostResDTO> getPost(@PathVariable Long postId,
                                                  @RequestParam(name = "count", defaultValue = "true") boolean count,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        if (count) {
            postReadService.incrementViewCount(postId, request, response);
        }
        FullPostResDTO fullPostResDTO = postReadService.getPost(postId, userDetails);
        return ResponseEntity.ok(fullPostResDTO);
    }
}