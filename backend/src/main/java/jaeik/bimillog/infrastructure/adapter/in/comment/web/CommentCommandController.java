package jaeik.bimillog.infrastructure.adapter.in.comment.web;

import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
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
 * <p>
 * 헥사고날 아키텍처에서 댓글 도메인의 쓰기 작업을 처리하는 REST API 어댑터입니다.
 * </p>
 * <p>
 * 클라이언트로부터 댓글 상태 변경 요청을 받아 도메인 계층의 CommentCommandUseCase로 전달하며,
 * CQRS 패턴에서 Command 책임을 담당합니다.
 * </p>
 * <p>
 * 처리하는 HTTP 요청: POST /api/comment/write, /update, /delete, /like
 * </p>
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
     * <p>클라이언트에서 전송한 댓글 작성 POST 요청을 처리합니다.</p>
     * <p>익명/로그인 사용자 모두 댓글 작성이 가능하며, 계층형 댓글 구조를 지원합니다.</p>
     * <p>검증된 요청 데이터를 직접 CommentCommandUseCase에 전달합니다.</p>
     * <p>프론트엔드의 댓글 작성 폼에서 호출되며, 작성 완료 후 UI 갱신을 위한 응답을 반환합니다.</p>
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
        commentReqDto.setUserId(userDetails != null ? userDetails.getUserId() : null);
        commentCommandUseCase.writeComment(
                commentReqDto.getUserId(),
                commentReqDto.getPostId(),
                commentReqDto.getParentId(),
                commentReqDto.getContent(),
                commentReqDto.getPassword()
        );
        return ResponseEntity.ok("댓글 작성 완료");
    }

    /**
     * <h3>댓글 수정 API</h3>
     * <p>클라이언트에서 전송한 댓글 수정 POST 요청을 처리합니다.</p>
     * <p>작성자 본인 확인 로직을 통해 권한을 검증하며, 익명 댓글의 경우 비밀번호를 확인합니다.</p>
     * <p>검증된 수정 데이터를 직접 도메인 계층으로 전달하여 댓글 내용을 업데이트합니다.</p>
     * <p>프론트엔드의 댓글 수정 모달에서 호출되며, 수정 완료 후 UI 새로고침을 위한 응답을 반환합니다.</p>
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
        commentReqDto.setUserId(userDetails != null ? userDetails.getUserId() : null);
        commentCommandUseCase.updateComment(
                commentReqDto.getId(),
                commentReqDto.getUserId(),
                commentReqDto.getContent(),
                commentReqDto.getPassword()
        );
        return ResponseEntity.ok("댓글 수정 완료");
    }

    /**
     * <h3>댓글 삭제 API</h3>
     * <p>클라이언트에서 전송한 댓글 삭제 POST 요청을 처리합니다.</p>
     * <p>작성자 본인 확인 로직을 통해 삭제 권한을 검증하며, 익명 댓글의 경우 비밀번호를 확인합니다.</p>
     * <p>계층형 구조에서 하위 댓글이 있는 경우 내용만 삭제하고, 없는 경우 완전 삭제를 처리합니다.</p>
     * <p>프론트엔드의 댓글 삭제 확인 다이얼로그에서 호출되며, 삭제 완료 후 UI 갱신을 위한 응답을 반환합니다.</p>
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
        commentReqDto.setUserId(userDetails != null ? userDetails.getUserId() : null);
        commentCommandUseCase.deleteComment(
                commentReqDto.getId(),
                commentReqDto.getUserId(),
                commentReqDto.getPassword()
        );
        return ResponseEntity.ok("댓글 삭제 완료");
    }

    /**
     * <h3>댓글 추천/추천 취소 API</h3>
     * <p>클라이언트에서 전송한 댓글 좋아요 POST 요청을 처리합니다.</p>
     * <p>로그인한 사용자만 접근 가능하며, 이미 추천한 댓글은 추천 취소, 추천하지 않은 댓글은 추천으로 토글됩니다.</p>
     * <p>중복 추천 방지 로직과 추천 수 증감 처리를 도메인 계층에서 수행합니다.</p>
     * <p>프론트엔드의 댓글 좋아요 버튼에서 호출되며, 실시간 좋아요 수 업데이트를 위한 응답을 반환합니다.</p>
     *
     * @param commentLikeReqDto  댓글 좋아요 요청 DTO (댓글 ID 포함)
     * @param userDetails 현재 로그인한 사용자 정보 (추천자 식별용, 필수)
     * @return HTTP 응답 엔티티 (추천 처리 완료 메시지)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/like")
    public ResponseEntity<String> likeComment(
            @RequestBody @Valid CommentLikeReqDTO commentLikeReqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        commentCommandUseCase.likeComment(userId, commentLikeReqDto.getCommentId());
        return ResponseEntity.ok("추천 처리 완료");
    }

}
