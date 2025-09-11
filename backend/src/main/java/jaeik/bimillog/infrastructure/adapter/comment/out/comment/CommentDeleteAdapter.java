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
        int softDeleteCount = conditionalSoftDelete(commentId);

        // 소프트 삭제가 되지 않았다면 (자손이 없는 경우) 하드 삭제 수행
        if (softDeleteCount == 0) {
            deleteClosuresByDescendantId(commentId);
            hardDeleteComment(commentId);
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
        int softDeletedCount = batchSoftDeleteUserCommentsWithDescendants(userId);

        // 2. 자손이 없는 댓글들을 하드 삭제 (클로저도 함께 삭제)
        batchHardDeleteUserCommentsWithoutDescendants(userId);

        // 3. 소프트 삭제된 댓글들은 익명화 처리
        if (softDeletedCount > 0) {
            anonymizeUserComments(userId);
        }
    }

    /**
     * <h3>사용자 댓글 ID 목록 조회</h3>
     * <p>특정 사용자가 작성한 모든 댓글 ID 목록을 조회합니다.</p>
     * <p>processUserCommentsOnWithdrawal 메서드에서 호출되어 삭제 대상 댓글을 식별합니다.</p>
     *
     * @param userId 사용자 ID
     * @return List<Long> 사용자가 작성한 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<Long> findCommentIdsByUserId(Long userId) {
        return commentRepository.findCommentIdsByUserId(userId);
    }

    /**
     * <h3>사용자 댓글 익명화</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 익명화 처리합니다.</p>
     * <p>processUserCommentsOnWithdrawal 메서드에서 호출되어 소프트 삭제된 댓글을 익명화합니다.</p>
     *
     * @param userId 익명화할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void anonymizeUserComments(Long userId) {
        commentRepository.anonymizeUserComments(userId);
    }

    /**
     * <h3>자손이 있는 사용자 댓글 소프트 삭제</h3>
     * <p>자손이 있는 사용자 댓글들을 배치로 소프트 삭제합니다.</p>
     * <p>processUserCommentsOnWithdrawal 메서드에서 호출되어 계층 구조가 있는 댓글을 보존합니다.</p>
     *
     * @param userId 사용자 ID
     * @return int 소프트 삭제된 댓글 수
     * @author Jaeik
     * @since 2.0.0
     */
    private int batchSoftDeleteUserCommentsWithDescendants(Long userId) {
        return commentRepository.batchSoftDeleteUserCommentsWithDescendants(userId);
    }

    /**
     * <h3>자손이 없는 사용자 댓글 하드 삭제</h3>
     * <p>자손이 없는 사용자 댓글들을 배치로 하드 삭제합니다.</p>
     * <p>processUserCommentsOnWithdrawal 메서드에서 호출되어 계층 구조에 영향이 없는 댓글을 완전 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void batchHardDeleteUserCommentsWithoutDescendants(Long userId) {
        commentRepository.batchHardDeleteUserCommentsWithoutDescendants(userId);
    }

    /**
     * <h3>자손 존재 시 소프트 삭제</h3>
     * <p>자손이 있는 댓글에 대해서만 소프트 삭제를 수행합니다.</p>
     * <p>deleteComment 메서드에서 호출되어 계층 구조 보존 여부를 결정합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return int 소프트 삭제된 댓글 수 (자손이 있으면 1, 없으면 0)
     * @author Jaeik
     * @since 2.0.0
     */
    private int conditionalSoftDelete(Long commentId) {
        return commentRepository.conditionalSoftDelete(commentId);
    }

    /**
     * <h3>클로저 관계 삭제</h3>
     * <p>자손이 없는 댓글의 모든 클로저 관계를 삭제합니다.</p>
     * <p>deleteComment 메서드에서 호출되어 하드 삭제 전 계층 구조 정리를 담당합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteClosuresByDescendantId(Long commentId) {
        commentRepository.deleteClosuresByDescendantId(commentId);
    }

    /**
     * <h3>댓글 완전 삭제</h3>
     * <p>자손이 없는 댓글을 완전히 삭제합니다.</p>
     * <p>deleteComment 메서드에서 호출되어 클로저 관계 정리 후 댓글 엔티티를 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void hardDeleteComment(Long commentId) {
        commentRepository.hardDeleteComment(commentId);
    }
}