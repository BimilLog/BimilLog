package jaeik.growfarm.controller;

import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.dto.board.PostReqDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>게시판 관련 컨트롤러</h2>
 * <p>게시판 조회</p>
 * <p>실시간 인기글 조회</p>
 * <p>주간 인기글 조회</p>
 * <p>명예의 전당 조회</p>
 * <p>게시글 검색</p>
 * <p>게시글 CRUD</p>
 * <p>게시글 추천/추천 취소</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/board")
public class PostController {

    private final PostService postService;

    /**
     * <h3>게시판 조회 API</h3>
     *
     * <p>
     * 최신순으로 게시글 목록을 페이지네이션으로 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param page 페이지 번호
     * @param size 페이지 사이즈
     * @return 게시글 목록 페이지
     */
    @GetMapping
    public ResponseEntity<Page<SimplePostDTO>> getBoard(@RequestParam int page, @RequestParam int size) {
        Page<SimplePostDTO> postList = postService.getBoard(page, size);
        return ResponseEntity.ok(postList);
    }

    /**
     * <h3>실시간 인기글 조회 API</h3>
     *
     * <p>
     * 실시간 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 실시간 인기글 목록
     */
    @GetMapping("/realtime")
    public ResponseEntity<List<SimplePostDTO>> getRealtimeBoard() {
        List<SimplePostDTO> realtimePopularPosts = postService.getRealtimePopularPosts();
        return ResponseEntity.ok(realtimePopularPosts);
    }

    /**
     * <h3>주간 인기글 조회 API</h3>
     *
     * <p>
     * 주간 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 주간 인기글 목록
     */
    @GetMapping("/weekly")
    public ResponseEntity<List<SimplePostDTO>> getWeeklyBoard() {
        List<SimplePostDTO> weeklyPopularPosts = postService.getWeeklyPopularPosts();
        return ResponseEntity.ok(weeklyPopularPosts);
    }

    /**
     * <h3>명예의 전당 조회 API</h3>
     *
     * <p>
     * 명예의 전당에 선정된 게시글 목록을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 명예의 전당 게시글 목록
     */
    @GetMapping("/fame")
    public ResponseEntity<List<SimplePostDTO>> getHallOfFameBoard() {
        List<SimplePostDTO> hallOfFamePosts = postService.getHallOfFamePosts();
        return ResponseEntity.ok(hallOfFamePosts);
    }

    /**
     * <h3>게시글 검색 API</h3>
     *
     * <p>
     * 검색 유형과 검색어를 통해 게시글을 검색한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param type  검색 유형
     * @param query 검색어
     * @param page  페이지 번호
     * @param size  페이지 사이즈
     * @return 검색된 게시글 목록 페이지
     */
    @GetMapping("/search")
    public ResponseEntity<Page<SimplePostDTO>> searchPost(@RequestParam String type, // 제목, 제목 + 내용, 작성자 검색
            @RequestParam String query, // 검색어
            @RequestParam int page,
            @RequestParam int size) {

        Page<SimplePostDTO> searchList = postService.searchPost(type, query, page, size);
        return ResponseEntity.ok(searchList);
    }

    /**
     * <h3>게시글 상세 조회 API</h3>
     *
     * <p>
     * 게시글 ID를 통해 게시글 상세 정보를 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @param request     HTTP 요청 객체
     * @param response    HTTP 응답 객체
     * @return 게시글 상세 정보
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPost(@PathVariable Long postId,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request,
            HttpServletResponse response) {

        postService.incrementViewCount(postId, request, response);

        PostDTO postDTO = postService.getPost(postId, userId);

        return ResponseEntity.ok(postDTO);
    }

    /**
     * <h3>게시글 작성 API</h3>
     *
     * <p>
     * 새로운 게시글을 작성하고 저장한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postReqDTO  게시글 작성 요청 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성된 게시글 정보
     */
    @PostMapping("/write")
    public ResponseEntity<PostDTO> writePost(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PostReqDTO postReqDTO) {
        PostDTO postDTO = postService.writePost(userDetails, postReqDTO);
        return ResponseEntity.ok(postDTO);
    }

    /**
     * <h3>게시글 수정 API</h3>
     *
     * <p>
     * 게시글 작성자만 게시글을 수정할 수 있다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postId      게시글 ID
     * @param postDTO     수정할 게시글 정보
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 수정된 게시글 정보
     */
    @PostMapping("/{postId}")
    public ResponseEntity<PostDTO> updatePost(@PathVariable Long postId,
            @RequestParam Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PostDTO postDTO) {
        PostDTO updatedPostDTO = postService.updatePost(postId, userDetails, postDTO);
        return ResponseEntity.ok(updatedPostDTO);
    }

    /**
     * <h3>게시글 삭제 API</h3>
     *
     * <p>
     * 게시글 작성자만 게시글을 삭제할 수 있다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 삭제 성공 메시지
     */
    @PostMapping("/{postId}/delete")
    public ResponseEntity<String> deletePost(@PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(postId, userDetails);
        return ResponseEntity.ok("게시글 삭제 완료");
    }

    /**
     * <h3>게시글 좋아요/좋아요 취소 API</h3>
     *
     * <p>
     * 게시글에 좋아요를 누르거나 취소한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요 처리 결과 메시지
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(@PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.likePost(postId, userDetails);
        return ResponseEntity.ok("게시글 추천 완료");
    }
}
