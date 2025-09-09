package jaeik.bimillog.domain.comment.application.port.out;

/**
 * <h2>댓글 삭제 어댑터</h2>
 * <p>댓글 및 댓글 클로저 엔티티의 삭제/익명화를 위한 아웃바운드 어댑터</p>
 * <p>관심사 기반으로 분리된 삭제 전용 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentDeletePort {

    /**
     * <h3>게시글 ID로 모든 댓글 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 댓글을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByPostId(Long postId);

    /**
     * <h3>댓글 삭제 처리 (하드/소프트 삭제)</h3>
     * <p>댓글 ID를 기반으로 자손이 있는지 확인하여 적절한 삭제 방식을 선택합니다.</p>
     * <p>자손이 없으면 하드 삭제를, 있으면 소프트 삭제를 수행합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteComment(Long commentId);

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