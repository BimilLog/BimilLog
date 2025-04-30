package jaeik.growfarm.controller;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    // 해당 유저의 작성 글 목록 반환
    @GetMapping("/mypage/posts")
    public ResponseEntity<Page<SimplePostDTO>> getPostList(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<SimplePostDTO> postList = userService.getPostList(page, size, userDetails);
        return ResponseEntity.ok(postList);
    }

    // 해당 유저의 작성 댓글 목록 반환
    @GetMapping("/mypage/comments")
    public ResponseEntity<Page<CommentDTO>> getCommentList(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<CommentDTO> commentList = userService.getCommentList(page, size, userDetails);
        return ResponseEntity.ok(commentList);
    }

    // 해당 유저가 추천한 글 목록 반환
    @GetMapping("/mypage/likedposts")
    public ResponseEntity<Page<SimplePostDTO>> getLikedPosts(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<SimplePostDTO> likedPosts = userService.getLikedPosts(page, size, userDetails);
        return ResponseEntity.ok(likedPosts);
    }

    // 해당 유저가 추천한 댓글 목록 반환
    @GetMapping("/mypage/likedcomments")
    public ResponseEntity<Page<CommentDTO>> getLikedComments(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<CommentDTO> likedComments = userService.getLikedComments(page, size, userDetails);
        return ResponseEntity.ok(likedComments);
    }

    // 농장 이름 변경
    @GetMapping("/mypage/updatefarm")
    public ResponseEntity<String> updateFarmName(@RequestParam String farmName,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.updateFarmName(farmName, userDetails);
        return ResponseEntity.ok("농장 이름이 변경되었습니다.");
    }

    // 건의 하기
    @PostMapping("/suggestion")
    public ResponseEntity<String> suggestion(@RequestBody ReportDTO reportDTO) {
        userService.suggestion(reportDTO);
        return ResponseEntity.ok("건의가 완료되었습니다.");
    }

    // 카카오 친구 목록 가져 오기
    @PostMapping("/friendlist")
    public ResponseEntity<KakaoFriendListDTO> getFriendList(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam int offset) {
        return ResponseEntity.ok(userService.getFriendList(userDetails, offset));
    }
}
