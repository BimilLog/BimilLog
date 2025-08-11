package jaeik.growfarm.domain.comment.infrastructure.adapter.in.web;

import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.dto.comment.CommentDto;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>댓글 Command 컨트롤러</h2>
 * <p>댓글의 상태를 변경하는 API를 담당합니다. (생성, 수정, 삭제, 추천)</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentCommandController {

    private final CommentCommandUseCase commentCommandUseCase;

    /**
     * <h3>댓글 작성 API</h3>
     *
     * <p>
     * 게시글에 새로운 댓글을 작성한다.
     * </p>
     *
     * @param commentDto  댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 작성 성공 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/write")
    public ResponseEntity<String> writeComment(
            @Valid @RequestBody CommentDto commentDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentCommandUseCase.writeComment(userDetails, commentDto);
        return ResponseEntity.ok("댓글 작성 완료");
    }

    /**
     * <h3>댓글 수정 API</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 수정할 수 있다.
     * </p>
     *
     * @param commentDto  수정할 댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 수정 성공 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/update")
    public ResponseEntity<String> updateComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CommentDto commentDto) {
        commentCommandUseCase.updateComment(commentDto, userDetails);
        return ResponseEntity.ok("댓글 수정 완료");
    }

    /**
     * <h3>댓글 삭제 API</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 삭제할 수 있다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 삭제 성공 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CommentDto commentDto) {
        commentCommandUseCase.deleteComment(commentDto, userDetails);
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
     * @param commentDto  댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 추천 처리 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/like")
    public ResponseEntity<String> likeComment(
            @RequestBody @Valid CommentDto commentDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentCommandUseCase.likeComment(commentDto, userDetails);
        return ResponseEntity.ok("추천 처리 완료");
    }
}
