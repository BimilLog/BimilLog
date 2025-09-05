package jaeik.bimillog.domain.comment.application.port.in;

import jaeik.bimillog.domain.comment.entity.Comment;

/**
 * <h2>댓글 명령 요구사항</h2>
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
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param commentRequest  댓글 요청 (비밀번호 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    void writeComment(Long userId, Comment.Request commentRequest);

    /**
     * <h3>댓글 수정</h3>
     * <p>기존 댓글을 수정합니다.</p>
     *
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param commentRequest  수정할 댓글 요청 (비밀번호 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    void updateComment(Long userId, Comment.Request commentRequest);

    /**
     * <h3>댓글 삭제</h3>
     * <p>댓글을 삭제합니다.</p>
     *
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param commentRequest  삭제할 댓글 요청 (비밀번호 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteComment(Long userId, Comment.Request commentRequest);

    /**
     * <h3>사용자 탈퇴 시 댓글 처리</h3>
     * <p>사용자 탈퇴 시 해당 사용자의 모든 댓글에 대해 적절한 처리를 수행합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제 + 익명화</p>
     * <p>자손이 없는 댓글: 하드 삭제</p>
     *
     * @param userId 탈퇴하는 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void processUserCommentsOnWithdrawal(Long userId);

}
