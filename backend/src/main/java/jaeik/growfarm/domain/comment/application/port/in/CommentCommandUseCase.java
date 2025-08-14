package jaeik.growfarm.domain.comment.application.port.in;

import jaeik.growfarm.dto.comment.CommentDTO;
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
     * <h3>댓글 작성</h3>
     * <p>새로운 댓글을 작성합니다.</p>
     *
     * @param userDetails 사용자 인증 정보
     * @param commentDto  댓글 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    void writeComment(CustomUserDetails userDetails, CommentDTO commentDto);

    /**
     * <h3>댓글 수정</h3>
     * <p>기존 댓글을 수정합니다.</p>
     *
     * @param commentDto  수정할 댓글 DTO
     * @param userDetails 사용자 인증 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void updateComment(CommentDTO commentDto, CustomUserDetails userDetails);

    /**
     * <h3>댓글 삭제</h3>
     * <p>댓글을 삭제합니다.</p>
     *
     * @param commentDto  삭제할 댓글 DTO
     * @param userDetails 사용자 인증 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteComment(CommentDTO commentDto, CustomUserDetails userDetails);

    /**
     * <h3>댓글 추천/취소</h3>
     * <p>댓글에 추천를 누르거나 취소합니다.</p>
     *
     * @param commentDto  추천/취소할 댓글 DTO
     * @param userDetails 사용자 인증 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void likeComment(CommentDTO commentDto, CustomUserDetails userDetails);
}
