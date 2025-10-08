package jaeik.bimillog.infrastructure.adapter.in.comment.web;

import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.infrastructure.adapter.in.comment.dto.CommentLikeReqDTO;
import jaeik.bimillog.infrastructure.adapter.in.comment.dto.CommentReqDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
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
 * <p>댓글 도메인의 쓰기 작업을 처리하는 REST API 어댑터입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentCommandController {

    private final CommentCommandUseCase commentCommandUseCase;

    /**
     * <h3>댓글 작성 API</h3>
     * <p>익명/로그인 사용자 모두 댓글 작성이 가능하며, 계층형 댓글 구조를 지원합니다.</p>
     *
     * @param commentReqDto  댓글 요청 DTO (내용, 게시글 ID, 부모 댓글 ID, 비밀번호 등)
     * @param userDetails 현재 로그인한 사용자 정보 (익명일 경우 null)
     * @return HTTP 응답 엔티티 (작성 완료 메시지)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/write")
    public ResponseEntity<String> writeComment(
            @Valid @RequestBody CommentReqDTO commentReqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentReqDto.setMemberId(userDetails != null ? userDetails.getMemberId() : null);
        commentCommandUseCase.writeComment(
                commentReqDto.getMemberId(),
                commentReqDto.getPostId(),
                commentReqDto.getParentId(),
                commentReqDto.getContent(),
                commentReqDto.getPassword()
        );
        return ResponseEntity.ok("댓글 작성 완료");
    }

    /**
     * <h3>댓글 수정 API</h3>
     * <p>작성자 본인 확인 로직을 통해 권한을 검증하며, 익명 댓글의 경우 비밀번호를 확인합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보 (권한 확인용)
     * @param commentReqDto  수정할 댓글 요청 DTO (댓글 ID, 새로운 내용, 비밀번호 등)
     * @return HTTP 응답 엔티티 (수정 완료 메시지)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/update")
    public ResponseEntity<String> updateComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CommentReqDTO commentReqDto) {
        commentReqDto.setMemberId(userDetails != null ? userDetails.getMemberId() : null);
        commentCommandUseCase.updateComment(
                commentReqDto.getId(),
                commentReqDto.getMemberId(),
                commentReqDto.getContent(),
                commentReqDto.getPassword()
        );
        return ResponseEntity.ok("댓글 수정 완료");
    }

    /**
     * <h3>댓글 삭제 API</h3>
     * <p>작성자 본인 확인 로직을 통해 삭제 권한을 검증하며, 익명 댓글의 경우 비밀번호를 확인합니다.</p>
     * <p>하위 댓글이 있는 경우 내용만 삭제하고, 없는 경우 완전 삭제를 처리합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보 (권한 확인용)
     * @param commentReqDto  삭제할 댓글 요청 DTO (댓글 ID, 비밀번호 등)
     * @return HTTP 응답 엔티티 (삭제 완료 메시지)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CommentReqDTO commentReqDto) {
        commentReqDto.setMemberId(userDetails != null ? userDetails.getMemberId() : null);
        commentCommandUseCase.deleteComment(
                commentReqDto.getId(),
                commentReqDto.getMemberId(),
                commentReqDto.getPassword()
        );
        return ResponseEntity.ok("댓글 삭제 완료");
    }

    /**
     * <h3>댓글 추천/추천 취소 API</h3>
     * <p>로그인한 사용자만 접근 가능하며, 이미 추천한 댓글은 추천 취소, 추천하지 않은 댓글은 추천으로 토글됩니다.</p>
     *
     * @param commentLikeReqDto  댓글 좋아요 요청 DTO (댓글 ID 포함)
     * @param userDetails 현재 로그인한 사용자 정보 (추천자 식별용, 필수)
     * @return HTTP 응답 엔티티 (추천 처리 완료 메시지)
     * @throws MemberCustomException 인증되지 않은 사용자가 접근할 경우 발생
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/like")
    public ResponseEntity<String> likeComment(
            @RequestBody @Valid CommentLikeReqDTO commentLikeReqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new MemberCustomException(MemberErrorCode.USER_NOT_FOUND);
        }
        Long memberId = userDetails.getMemberId();
        commentCommandUseCase.likeComment(memberId, commentLikeReqDto.getCommentId());
        return ResponseEntity.ok("추천 처리 완료");
    }

}
