package jaeik.growfarm.controller;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.comment.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>댓글 관련 컨트롤러</h2>
 *
 * <p>
 * 댓글 작성
 * </p>
 * <p>
 * 댓글 수정
 * </p>
 * <p>
 * 댓글 삭제
 * </p>
 * <p>
 * 댓글 추천/추천 취소
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/comment")
public class CommentController {

    private final CommentService commentService;

    /**
     * <h3>댓글 조회 API</h3>
     * <p>
     * 게시글에 달린 댓글을 최신순으로 페이징하여 반환한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @param page   페이지 번호
     * @return 댓글 목록 페이지 (최신순)
     * @since 1.0.0
     * @author Jaeik
     */
    @GetMapping("/{postId}")
    public ResponseEntity<Page<CommentDTO>> getComments(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(commentService.getCommentsLatestOrder(postId, page, userDetails));
    }

    /**
     * <h3>인기댓글 조회 API</h3>
     * <p>
     * 추천수 3개 이상이며 글에서 추천수가 상위 3위이내인 댓글을 반환한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @return 인기댓글 리스트 (최대 3개)
     * @since 1.0.0
     * @author Jaeik
     */
    @GetMapping("/{postId}/popular")
    public ResponseEntity<List<CommentDTO>> getPopularComments(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getPopularComments(postId, userDetails));
    }


    /**
     * <h3>댓글 작성 API</h3>
     *
     * <p>
     * 게시글에 새로운 댓글을 작성한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param commentDTO  댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 작성 성공 메시지
     */
    @PostMapping("/write")
    public ResponseEntity<String> writeComment(@Valid @RequestBody CommentDTO commentDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.writeComment(userDetails, commentDTO);
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
     * @param commentDTO  수정할 댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 수정 성공 메시지
     */
    @PostMapping("/update")
    public ResponseEntity<String> updateComment(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CommentDTO commentDTO) {
        commentService.updateComment(commentDTO, userDetails);
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
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 삭제 성공 메시지
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteComment(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CommentDTO commentDTO) {
        commentService.deleteComment(commentDTO, userDetails);
        return ResponseEntity.ok("댓글 삭제 완료");
    }

    /**
     * <h3>댓글 추천/추천 취소 API</h3>
     *
     * <p>
     * 댓글에 추천을 하거나 취소한다.
     * </p>
     * <p>
     * 추천/추천 취소는 로그인 한 유저만 가능하다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param commentDTO  댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 추천 처리 메시지
     */
    @PostMapping("/like")
    public ResponseEntity<String> likeComment(@RequestBody @Valid CommentDTO commentDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.likeComment(commentDTO, userDetails);
        return ResponseEntity.ok("추천 처리 완료");
    }
}