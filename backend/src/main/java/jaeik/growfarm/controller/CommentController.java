package jaeik.growfarm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jaeik.growfarm.dto.comment.CommentDTO;
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
 * @version 1.0.0
 * @author Jaeik
 */
@Tag(name = "댓글", description = "댓글 관련 API")
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
    @Operation(summary = "댓글 목록 조회", description = "게시글에 달린 댓글을 최신순으로 페이징하여 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 게시글이 존재하지 않습니다."),
            @ApiResponse(responseCode = "500", description = "댓글 조회에 실패했습니다.")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<Page<CommentDTO>> getComments(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page) {
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
    @Operation(summary = "인기 댓글 조회", description = "추천수 3개 이상이며 글에서 추천수가 상위 3위 이내인 댓글을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인기 댓글 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 게시글이 존재하지 않습니다."),
            @ApiResponse(responseCode = "500", description = "인기 댓글 조회에 실패했습니다.")
    })
    @GetMapping("/{postId}/popular")
    public ResponseEntity<List<CommentDTO>> getPopularComments(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "게시글 ID") @PathVariable Long postId) {
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
    @Operation(summary = "댓글 작성", description = "게시글에 새로운 댓글을 작성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 작성 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "404", description = "해당 게시글 또는 부모 댓글이 존재하지 않습니다."),
            @ApiResponse(responseCode = "500", description = "댓글 작성에 실패했습니다.")
    })
    @PostMapping("/write")
    public ResponseEntity<String> writeComment(
            @Parameter(description = "댓글 정보") @Valid @RequestBody CommentDTO commentDTO,
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
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
    @Operation(summary = "댓글 수정", description = "댓글 작성자만 댓글을 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "403", description = "댓글 작성자만 수정할 수 있습니다."),
            @ApiResponse(responseCode = "404", description = "해당 댓글이 존재하지 않습니다.")
    })
    @PostMapping("/update")
    public ResponseEntity<String> updateComment(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "수정할 댓글 정보") @RequestBody @Valid CommentDTO commentDTO) {
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
    @Operation(summary = "댓글 삭제", description = "댓글 작성자만 댓글을 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "403", description = "댓글 작성자만 삭제할 수 있습니다."),
            @ApiResponse(responseCode = "404", description = "해당 댓글이 존재하지 않습니다."),
            @ApiResponse(responseCode = "500", description = "댓글 삭제에 실패했습니다.")
    })
    @PostMapping("/delete")
    public ResponseEntity<String> deleteComment(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "삭제할 댓글 정보") @RequestBody @Valid CommentDTO commentDTO) {
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
    @Operation(summary = "댓글 추천/추천 취소", description = "댓글에 추천을 하거나 취소합니다. 로그인한 사용자만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천/추천 취소 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PostMapping("/like")
    public ResponseEntity<String> likeComment(
            @Parameter(description = "추천/추천 취소할 댓글 정보") @RequestBody @Valid CommentDTO commentDTO,
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.likeComment(commentDTO, userDetails);
        return ResponseEntity.ok("추천 처리 완료");
    }
}