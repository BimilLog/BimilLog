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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/board")
public class PostController {

    private final PostService postService;

    // 게시판 진입
    @GetMapping
    public ResponseEntity<Map<String, Object>> getBoard(@RequestParam int page, @RequestParam int size) {
        Page<SimplePostDTO> postList = postService.getBoard(page, size);
        List<SimplePostDTO> featuredPosts = postService.getFeaturedPosts();

        Map<String, Object> response = new HashMap<>();
        response.put("posts", postList);
        response.put("featuredPosts", featuredPosts);

        return ResponseEntity.ok(response);
    }

    // 게시글 검색
    @GetMapping("/search")
    public ResponseEntity<Page<SimplePostDTO>> searchPost(@RequestParam String type, // 제목, 제목 + 내용, 작성자 검색
            @RequestParam String query, // 검색어
            @RequestParam int page,
            @RequestParam int size) {

        Page<SimplePostDTO> searchList = postService.searchPost(type, query, page, size);
        return ResponseEntity.ok(searchList);
    }

    // 게시글 쓰기
    @PostMapping("/write")
    public ResponseEntity<PostDTO> writePost(@RequestBody PostReqDTO postReqDTO) {
        PostDTO postDTO = postService.writePost(postReqDTO);
        return ResponseEntity.ok(postDTO);
    }

    // 게시글 진입
    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPost(@PathVariable Long postId,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request,
            HttpServletResponse response) {

        postService.incrementViewCount(postId, request, response);

        PostDTO postDTO = postService.getPost(postId, userId);

        return ResponseEntity.ok(postDTO);
    }

    // 게시글 수정
    @PostMapping("/{postId}")
    public ResponseEntity<PostDTO> updatePost(@PathVariable Long postId,
            @RequestParam Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PostDTO postDTO) {
        PostDTO updatedPostDTO = postService.updatePost(postId, userDetails, postDTO);
        return ResponseEntity.ok(updatedPostDTO);
    }

    // 게시글 삭제
    @PostMapping("/{postId}/delete")
    public ResponseEntity<String> deletePost(@PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(postId, userDetails);
        return ResponseEntity.ok("게시글 삭제 완료");
    }

    // 게시글 추천, 추천 취소
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(@PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.likePost(postId, userDetails);
        return ResponseEntity.ok("게시글 추천 완료");
    }

    // 게시글 신고
    @PostMapping("/{postId}/report")
    public ResponseEntity<String> reportPost(@PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody String content) {
        postService.reportPost(postId, userDetails, content);
        return ResponseEntity.ok("게시글 신고 완료");
    }

}
