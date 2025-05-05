package jaeik.growfarm.controller;

import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.dto.board.PostReqDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * 게시판 관련 API
 * 게시판 조회
 * 게시글 검색
 * 게시글 조회
 * 게시글 작성
 * 게시글 수정
 * 게시글 삭제
 * 게시글 추천 / 추천 취소
 * 게시글 신고
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class PostController {

    private final PostService postService;

    /*
     * 게시판 조회 API
     * param int page: 페이지 번호
     * param int size: 페이지 사이즈
     * return: ResponseEntity<Page<SimplePostDTO>> 게시글 리스트
     * 수정일 : 2025-05-05
     */
    @GetMapping
    public ResponseEntity<Page<SimplePostDTO>> getBoard(@RequestParam int page, @RequestParam int size) {
        Page<SimplePostDTO> postList = postService.getBoard(page, size);
        return ResponseEntity.ok(postList);
    }

    /*
     * 실시간 인기글 목록 조회 API
     * return : ResponseEntity<List<SimplePostDTO>> 실시간 인기글 목록
     * 수정일 : 2025-05-05
     */
    @GetMapping("/realtime")
    public ResponseEntity<List<SimplePostDTO>> getRealtimeBoard() {
        List<SimplePostDTO> realtimePopularPostList = postService.getRealtimePopularPosts();
        return ResponseEntity.ok(realtimePopularPostList);
    }

    /*
     * 게시글 검색 API
     * param String type: 검색 타입 (제목, 제목 + 내용, 작성자)
     * param String query: 검색어
     * param int page: 페이지 번호
     * param int size: 페이지 사이즈
     * return: ResponseEntity<Page<SimplePostDTO>> 검색 결과 리스트
     * 수정일 : 2025-05-03
     */
    @GetMapping("/search")
    public ResponseEntity<Page<SimplePostDTO>> searchPost(@RequestParam String type, // 제목, 제목 + 내용, 작성자 검색
            @RequestParam String query, // 검색어
            @RequestParam int page,
            @RequestParam int size) {

        Page<SimplePostDTO> searchList = postService.searchPost(type, query, page, size);
        return ResponseEntity.ok(searchList);
    }

    /*
     * 게시글 조회 API
     * param Long postId: 게시글 ID
     * param Long userId: 유저 ID (optional)
     * return: ResponseEntity<PostDTO> 게시글 DTO
     * 게시글 조회 시 쿠키 사용 중복 조회 방지.
     * 수정일 : 2025-05-03
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

    /*
     * 게시글 작성 API
     * param PostReqDTO postReqDTO: 게시글 작성 DTO
     * return: ResponseEntity<PostDTO> 작성된 게시글 DTO
     * 수정일 : 2025-05-03
     */
    @PostMapping("/write")
    public ResponseEntity<PostDTO> writePost(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             @RequestBody PostReqDTO postReqDTO) {
        PostDTO postDTO = postService.writePost(userDetails, postReqDTO);
        return ResponseEntity.ok(postDTO);
    }

    /*
     * 게시글 수정 API
     * param Long postId: 게시글 ID
     * param Long userId: 유저 ID
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * param PostDTO postDTO: 게시글 수정 DTO
     * return: ResponseEntity<PostDTO> 수정된 게시글 DTO
     * 수정일 : 2025-05-03
     */
    @PostMapping("/{postId}")
    public ResponseEntity<PostDTO> updatePost(@PathVariable Long postId,
            @RequestParam Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PostDTO postDTO) {
        PostDTO updatedPostDTO = postService.updatePost(postId, userDetails, postDTO);
        return ResponseEntity.ok(updatedPostDTO);
    }

    /*
     * 게시글 삭제 API
     * param Long postId: 게시글 ID
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: ResponseEntity<String> 게시글 삭제 완료 메시지
     * 수정일 : 2025-05-03
     */
    @PostMapping("/{postId}/delete")
    public ResponseEntity<String> deletePost(@PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(postId, userDetails);
        return ResponseEntity.ok("게시글 삭제 완료");
    }

    /*
     * 게시글 추천 / 추천 취소 API
     * param Long postId: 게시글 ID
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: ResponseEntity<String> 게시글 추천 완료 메시지
     * 수정일 : 2025-05-03
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(@PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.likePost(postId, userDetails);
        return ResponseEntity.ok("게시글 추천 완료");
    }

    /*
     * 게시글 신고 API
     * param Long postId: 게시글 ID
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * param String content: 신고 사유
     * return: ResponseEntity<String> 게시글 신고 완료 메시지
     * 수정일 : 2025-05-03
     */
    @PostMapping("/{postId}/report")
    public ResponseEntity<String> reportPost(@PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody String content) {
        postService.reportPost(postId, userDetails, content);
        return ResponseEntity.ok("게시글 신고 완료");
    }

}
