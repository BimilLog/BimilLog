package jaeik.growfarm.controller;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/board")
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @PostMapping("/{postId}/comment")
    public ResponseEntity<String> writeComment(@PathVariable Long postId, @RequestBody CommentDTO commentDTO) {
        commentService.writeComment(postId, commentDTO);
        return ResponseEntity.ok("댓글 작성 완료");
    }

    // 댓글 수정
    @PostMapping("/{postId}/{commentId}")
    public ResponseEntity<String> updateComment(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @PathVariable Long postId,
                                                @PathVariable Long commentId,
                                                @RequestBody CommentDTO commentDTO) {
        commentService.updateComment(commentId, commentDTO, userDetails);
        return ResponseEntity.ok("댓글 수정 완료");
    }

    // 댓글 삭제
    @PostMapping("/{postId}/{commentId}/delete")
    public ResponseEntity<String> deleteComment(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @PathVariable Long postId,
                                                @PathVariable Long commentId) {
        commentService.deleteComment(commentId, userDetails);
        return ResponseEntity.ok("댓글 삭제 완료");
    }

    // 댓글 추천, 추천 취소
    @PostMapping("/{postId}/{commentId}/like")
    public ResponseEntity<String> likeComment(@PathVariable Long postId, @PathVariable Long commentId,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.likeComment(postId, commentId, userDetails);
        return ResponseEntity.ok("댓글 추천 완료");
    }

    // 댓글 신고
    @PostMapping("/{postId}/{commentId}/report")
    public ResponseEntity<String> reportComment(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @PathVariable Long postId,
                                                @PathVariable Long commentId,
                                                @RequestBody String content) {
        commentService.reportComment(postId, commentId, userDetails, content);
        return ResponseEntity.ok("댓글 신고 완료");
    }
}
