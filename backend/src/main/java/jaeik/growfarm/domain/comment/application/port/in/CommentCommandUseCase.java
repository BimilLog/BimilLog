package jaeik.growfarm.domain.comment.application.port.in;

import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentReqDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;

/**
 * <h2>댓글 명령 유스케이스</h2>
 * <p>댓글 생성, 수정, 삭제, 추천 등 댓글 관련 명령 요청을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentCommandUseCase {



    /**
     * <h3>댓글 수정</h3>
     * <p>기존 댓글을 수정합니다.</p>
     *
     * @param commentReqDto  수정할 댓글 요청 DTO (비밀번호 포함)
     * @param userDetails 사용자 인증 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void updateComment(CommentReqDTO commentReqDto, CustomUserDetails userDetails);

    /**
     * <h3>댓글 삭제</h3>
     * <p>댓글을 삭제합니다.</p>
     *
     * @param commentReqDto  삭제할 댓글 요청 DTO (비밀번호 포함)
     * @param userDetails 사용자 인증 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteComment(CommentReqDTO commentReqDto, CustomUserDetails userDetails);


}
