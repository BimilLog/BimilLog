package jaeik.bimillog.infrastructure.adapter.comment.in.web;

import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.comment.application.port.in.CommentLikeUseCase;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.infrastructure.adapter.comment.in.web.dto.CommentLikeReqDTO;
import jaeik.bimillog.infrastructure.adapter.comment.in.web.dto.CommentReqDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
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
    private final CommentLikeUseCase commentLikeUseCase;

    /**
     * <h3>댓글 작성 API</h3>
     *
     * <p>
     * 게시글에 새로운 댓글을 작성한다.
     * </p>
     *
     * @param commentReqDto  댓글 요청 DTO (비밀번호 포함)
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 작성 성공 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/write")
    public ResponseEntity<String> writeComment(
            @Valid @RequestBody CommentReqDTO commentReqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Comment.Request commentRequest = convertToCommentRequest(commentReqDto);
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        commentCommandUseCase.writeComment(userId, commentRequest);
        return ResponseEntity.ok("댓글 작성 완료");
    }

    /**
     * <h3>댓글 수정 API</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 수정할 수 있다.
     * </p>
     *
     * @param commentReqDto  수정할 댓글 요청 DTO (비밀번호 포함)
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 수정 성공 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/update")
    public ResponseEntity<String> updateComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CommentReqDTO commentReqDto) {
        Comment.Request commentRequest = convertToCommentRequest(commentReqDto);
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        commentCommandUseCase.updateComment(userId, commentRequest);
        return ResponseEntity.ok("댓글 수정 완료");
    }

    /**
     * <h3>댓글 삭제 API</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 삭제할 수 있다.
     * </p>
     *
     * @param commentReqDto  삭제할 댓글 요청 DTO (비밀번호 포함)
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 삭제 성공 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CommentReqDTO commentReqDto) {
        Comment.Request commentRequest = convertToCommentRequest(commentReqDto);
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        commentCommandUseCase.deleteComment(userId, commentRequest);
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
     * @param commentLikeReqDto  댓글 좋아요 요청 DTO (댓글 ID만 포함)
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 추천 처리 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/like")
    public ResponseEntity<String> likeComment(
            @RequestBody @Valid CommentLikeReqDTO commentLikeReqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        commentLikeUseCase.likeComment(userId, commentLikeReqDto.getCommentId());
        return ResponseEntity.ok("추천 처리 완료");
    }

    /**
     * <h3>DTO를 도메인 객체로 변환</h3>
     * <p>CommentReqDTO를 Comment.Request(도메인)로 변환합니다.</p>
     * <p>헥사고날 아키텍처에서 인프라스트럭처 계층과 도메인 계층을 분리하기 위한 변환 로직</p>
     *
     * @param commentReqDto DTO 댓글 요청
     * @return Comment.Request 도메인 댓글 요청
     * @author Jaeik
     * @since 2.0.0
     */
    private Comment.Request convertToCommentRequest(CommentReqDTO commentReqDto) {
        return Comment.Request.builder()
                .id(commentReqDto.getId())
                .parentId(commentReqDto.getParentId())
                .postId(commentReqDto.getPostId())
                .userId(commentReqDto.getUserId())
                .content(commentReqDto.getContent())
                .password(commentReqDto.getPassword())
                .build();
    }
}
