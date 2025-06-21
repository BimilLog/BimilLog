package jaeik.growfarm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.post.PostService;
import jaeik.growfarm.service.redis.RedisPostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>게시판 관련 컨트롤러</h2>
 * <p>
 * 게시판 조회
 * </p>
 * <p>
 * 실시간 인기글 조회
 * </p>
 * <p>
 * 주간 인기글 조회
 * </p>
 * <p>
 * 명예의 전당 조회
 * </p>
 * <p>
 * 게시글 검색
 * </p>
 * <p>
 * 게시글 CRUD
 * </p>
 * <p>
 * 게시글 추천/추천 취소
 * </p>
 * 
 * @author Jaeik
 * @version 1.0.0
 */
@Tag(name = "게시글", description = "게시글 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {

    private final PostService postService;
    private final RedisPostService redisPostService;

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
    @Operation(summary = "게시글 목록 조회", description = "최신순으로 게시글 목록을 페이지네이션으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<Page<SimplePostDTO>> getBoard(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam int page,
            @Parameter(description = "페이지 크기") @RequestParam int size) {
        Page<SimplePostDTO> postList = postService.getBoard(page, size);
        return ResponseEntity.ok(postList);
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
    @Operation(summary = "게시글 검색", description = "검색 유형과 검색어를 통해 게시글을 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 검색 형식입니다.")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<SimplePostDTO>> searchPost(
            @Parameter(description = "검색 유형 (title, content, author)") @RequestParam String type,
            @Parameter(description = "검색어") @RequestParam String query,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam int page,
            @Parameter(description = "페이지 크기") @RequestParam int size) {
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
     * @param postId   게시글 ID
     * @param request  HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @return 게시글 상세 정보
     */
    @Operation(summary = "게시글 상세 조회", description = "게시글 ID를 통해 게시글 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 게시글이 존재하지 않습니다.")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {
        postService.incrementViewCount(postId, request, response);
        PostDTO postDTO = postService.getPost(postId, userDetails);
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
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성하고 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 작성 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다.")
    })
    @PostMapping("/write")
    public ResponseEntity<PostDTO> writePost(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "게시글 작성 정보") @RequestBody @Valid PostReqDTO postReqDTO) {
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
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postDTO     수정할 게시글 정보
     * @return 수정된 게시글 정보
     * @author Jaeik
     * @since 1.0.0
     */
    @Operation(summary = "게시글 수정", description = "게시글 작성자만 게시글을 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 수정 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "403", description = "게시글 작성자만 수정 및 삭제할 수 있습니다."),
            @ApiResponse(responseCode = "404", description = "해당 게시글이 존재하지 않습니다.")
    })
    @PostMapping("/update")
    public ResponseEntity<String> updatePost(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "수정할 게시글 정보") @RequestBody @Valid PostDTO postDTO) {
        postService.updatePost(userDetails, postDTO);
        return ResponseEntity.ok("게시글 수정 완료");
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
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 삭제 성공 메시지
     */
    @Operation(summary = "게시글 삭제", description = "게시글 작성자만 게시글을 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "403", description = "게시글 작성자만 수정 및 삭제할 수 있습니다."),
            @ApiResponse(responseCode = "404", description = "해당 게시글이 존재하지 않습니다.")
    })
    @PostMapping("/delete")
    public ResponseEntity<String> deletePost(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "삭제할 게시글 정보") @RequestBody @Valid PostDTO postDTO) {
        postService.deletePost(userDetails, postDTO);
        return ResponseEntity.ok("게시글 삭제 완료");
    }

    /**
     * <h3>게시글 추천/추천 취소 API</h3>
     *
     * <p>
     * 게시글에 추천을 하거나 취소한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postDTO     추천/추천 취소할 게시글 정보
     * @return 좋아요 처리 결과 메시지
     */
    @Operation(summary = "게시글 추천/추천 취소", description = "게시글에 추천을 하거나 취소합니다. 로그인한 사용자만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천/추천 취소 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "404", description = "해당 게시글이 존재하지 않습니다.")
    })
    @PostMapping("/like")
    public ResponseEntity<String> likePost(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "추천/추천 취소할 게시글 정보") @RequestBody @Valid PostDTO postDTO) {
        postService.likePost(postDTO, userDetails);
        return ResponseEntity.ok("추천 처리 완료");
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
    @Operation(summary = "실시간 인기글 조회", description = "실시간 인기글로 선정된 게시글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "실시간 인기글 조회 성공"),
            @ApiResponse(responseCode = "500", description = "레디스 읽기 중 오류가 발생했습니다.")
    })
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
     * @since 1.0.0
     * @author Jaeik
     * @return 주간 인기글 목록
     */
    @Operation(summary = "주간 인기글 조회", description = "주간 인기글로 선정된 게시글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주간 인기글 조회 성공"),
            @ApiResponse(responseCode = "500", description = "레디스 읽기 중 오류가 발생했습니다.")
    })
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
     * @since 1.0.0
     * @author Jaeik
     * @return 명예의 전당 게시글 목록
     */
    @Operation(summary = "레전드 인기글 조회", description = "레전드 인기글로 선정된 게시글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "레전드 인기글 조회 성공"),
            @ApiResponse(responseCode = "500", description = "레디스 읽기 중 오류가 발생했습니다.")
    })
    @GetMapping("/legend")
    public ResponseEntity<List<SimplePostDTO>> getLegendBoard() {
        List<SimplePostDTO> legendPopularPosts = redisPostService
                .getCachedPopularPosts(RedisPostService.PopularPostType.LEGEND);
        return ResponseEntity.ok(legendPopularPosts);
    }
}
