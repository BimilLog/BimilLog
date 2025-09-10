package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.admin.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.application.service.CommentCommandService;

/**
 * <h2>댓글 삭제 포트</h2>
 * <p>댓글 도메인의 삭제 작업을 담당하는 포트입니다.</p>
 * <p>댓글 하드 삭제: 자손 댓글이 없는 경우 완전 제거</p>
 * <p>댓글 소프트 삭제: 자손 댓글이 있는 경우 내용만 익명화</p>
 * <p>댓글 계층 구조 유지: Closure Table을 활용한 댓글 트리 구조 보존</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentDeletePort {

    /**
     * <h3>댓글 삭제</h3>
     * <p>댓글의 자손 여부에 따라 하드/소프트 삭제를 실행합니다.</p>
     * <p>자손이 없는 경우: 하드 삭제 (댓글과 관련 클로저 완전 제거)</p>
     * <p>자손이 있는 경우: 소프트 삭제 (내용 익명화, 구조 보존)</p>
     * <p>{@link CommentCommandService}에서 개별 댓글 삭제 시 호출됩니다.</p>
     *
     * @param commentId 삭제 처리할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteComment(Long commentId);

    /**
     * <h3>특정 사용자의 댓글 삭제</h3>
     * <p>회원 탈퇴, 강제 탈퇴 시에 댓글을 삭제합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제로 내용만 익명화</p>
     * <p>자손이 없는 댓글: 하드 삭제로 완전 제거</p>
     * <p>{@link UserWithdrawnEvent}, {@link AdminWithdrawEvent} 이벤트 발생시 회원 탈퇴로 인한 댓글 삭제 흐름에서 호출됩니다.</p>
     *
     * @param userId 탈퇴 처리할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void processUserCommentsOnWithdrawal(Long userId);
}