package jaeik.growfarm.controller;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * <h2>댓글 관련 컨트롤러</h2>
 * <p>
 * 댓글 작성, 수정, 삭제
 * </p>
 * <p>
 * 댓글 추천/추천 취소, 댓글 신고
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/board")
public class CommentController {

    private final CommentService commentService;

    /**
     * <h3>댓글 작성 API</h3>
     *
     * <p>게시글에 새로운 댓글을 작성한다.</p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postId      게시글 ID
     * @param commentDTO  댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 작성 성공 메시지
     * @throws IOException FCM 메시지 발송 오류 시 발생
     */
    @PostMapping("/{postId}/comment")
    public ResponseEntity<String> writeComment(@PathVariable Long postId,
            @Valid @RequestBody CommentDTO commentDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {
        commentService.writeComment(userDetails, postId, commentDTO);
        return ResponseEntity.ok("댓글 작성 완료");
    }

    /**
     * <h3>댓글 수정 API</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 수정할 수 있다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param commentId   댓글 ID
     * @param commentDTO  수정할 댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 수정 성공 메시지
     */
    @PostMapping("/{postId}/{commentId}")
    public ResponseEntity<String> updateComment(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentDTO commentDTO) {
        commentService.updateComment(commentId, commentDTO, userDetails);
        return ResponseEntity.ok("댓글 수정 완료");
    }

    /**
     * <h3>댓글 삭제 API</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 삭제할 수 있다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param commentId   댓글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 삭제 성공 메시지
     */
    @PostMapping("/{postId}/{commentId}/delete")
    public ResponseEntity<String> deleteComment(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        commentService.deleteComment(commentId, userDetails);
        return ResponseEntity.ok("댓글 삭제 완료");
    }

    /**
     * <h3>댓글 좋아요/좋아요 취소 API</h3>
     *
     * <p>
     * 댓글에 좋아요를 누르거나 취소한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postId      게시글 ID
     * @param commentId   댓글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요 처리 결과 메시지
     */
    @PostMapping("/{postId}/{commentId}/like")
    public ResponseEntity<String> likeComment(@PathVariable Long postId, @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.likeComment(postId, commentId, userDetails);
        return ResponseEntity.ok("댓글 추천 완료");
    }
}
