package jaeik.bimillog.infrastructure.adapter.comment.out.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentDeletePort;
import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>댓글 삭제 어댑터</h2>
 * <p>댓글 삭제 포트의 구현체입니다.</p>
 * <p>댓글 하드 삭제, 댓글 소프트 삭제 처리</p>
 * <p>사용자 탈퇴 시 댓글 일괄 처리</p>
 * <p>계층 구조 유지를 위한 클로저 테이블 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentDeleteAdapter implements CommentDeletePort {

    private final CommentRepository commentRepository;

    /**
     * <h3>댓글 삭제</h3>
     * <p>댓글을 삭제하며, 자식 댓글 존재 여부에 따라 소프트 삭제 또는 하드 삭제를 수행합니다.</p>
     * <p>자식 댓글이 있는 경우 소프트 삭제로 내용만 숨김, 없는 경우 완전 삭제합니다.</p>
     * <p>{@link CommentCommandService}에서 댓글 삭제 처리 시 호출됩니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteComment(Long commentId) {
        // 먼저 조건부 소프트 삭제 시도
        int softDeleteCount = commentRepository.conditionalSoftDelete(commentId);

        // 소프트 삭제가 되지 않았다면 (자손이 없는 경우) 하드 삭제 수행
        if (softDeleteCount == 0) {
            commentRepository.deleteClosuresByDescendantId(commentId);
            commentRepository.hardDeleteComment(commentId);
        }
    }

    /**
     * <h3>특정 사용자의 댓글 삭제</h3>
     * <p>회원 탈퇴, 강제 탈퇴 시에 댓글을 삭제합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제로 내용만 익명화 ("삭제된 댓글입니다")</p>
     * <p>자손이 없는 댓글: 하드 삭제로 완전 제거</p>
     * <p>{@link CommentCommandService}에서 사용자 탈퇴 처리 시 호출됩니다.</p>
     *
     * @param userId 탈퇴 처리할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void processUserCommentsOnWithdrawal(Long userId) {
        // 1. 자손이 있는 댓글들을 소프트 삭제
        int softDeletedCount = commentRepository.batchSoftDeleteUserCommentsWithDescendants(userId);

        // 2. 자손이 없는 댓글들을 하드 삭제 (클로저도 함께 삭제)
        commentRepository.batchHardDeleteUserCommentsWithoutDescendants(userId);

        // 3. 소프트 삭제된 댓글들은 익명화 처리
        if (softDeletedCount > 0) {
            commentRepository.anonymizeUserComments(userId);
        }
    }

}