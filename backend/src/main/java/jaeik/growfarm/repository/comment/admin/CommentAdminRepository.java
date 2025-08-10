package jaeik.growfarm.repository.comment.admin;

import org.springframework.stereotype.Repository;

/**
 * <h2>댓글 관리자 저장소</h2>
 * <p>
 * 관리자 전용 댓글 관리 기능을 담당한다.
 * Post Repository의 PostDeleteRepository 구조를 참조하여 ISP 원칙 적용
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface CommentAdminRepository {

    /**
     * <h3>회원탈퇴 시 댓글 처리</h3>
     * <p>
     * 탈퇴하는 사용자의 댓글을 적절히 처리한다.
     * </p>
     * <p>
     * 자손이 있는 댓글: 논리적 삭제 + userId null로 변경
     * </p>
     * <p>
     * 자손이 없는 댓글: 물리적 삭제
     * </p>
     *
     * @param userId 탈퇴하는 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void processUserCommentsOnWithdrawal(Long userId);

    /**
     * <h3>댓글 ID로 사용자 ID 조회</h3>
     * <p>
     * 댓글 ID로 해당 댓글을 작성한 사용자의 ID를 조회합니다.
     * 관리자가 신고 처리 시 사용
     * </p>
     *
     * @param commentId 댓글 ID
     * @return 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    Long findUserIdByCommentId(Long commentId);
}