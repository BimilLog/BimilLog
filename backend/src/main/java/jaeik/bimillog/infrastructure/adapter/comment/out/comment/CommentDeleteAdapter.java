package jaeik.bimillog.infrastructure.adapter.comment.out.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentDeletePort;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>댓글 삭제 어댑터</h2>
 * <p>
 * 헥사고날 아키텍처의 Secondary Adapter로서 CommentDeletePort 인터페이스를 구현합니다.
 * </p>
 * <p>
 * JPA Repository를 사용하여 댓글과 댓글 클로저 테이블의 삭제 작업을 수행합니다.
 * 계층 구조 댓글 시스템에서 자손 유무에 따른 소프트/하드 삭제 로직을 처리합니다.
 * </p>
 * <p>
 * 이 어댑터가 존재하는 이유: 댓글 삭제는 비즈니스 규칙이 복잡하여 (자손 존재 시 소프트 삭제, 미존재 시 하드 삭제)
 * 별도의 전용 어댑터로 분리하여 단일 책임 원칙을 준수합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentDeleteAdapter implements CommentDeletePort {

    private final CommentRepository commentRepository;

    /**
     * <h3>댓글 삭제 처리 (하드/소프트 삭제)</h3>
     * <p>댓글 ID를 기반으로 자손이 있는지 확인하여 적절한 삭제 방식을 선택합니다.</p>
     * <p>자손이 없으면 하드 삭제를, 있으면 소프트 삭제를 JPA로 수행합니다.</p>
     * <p>CommentDeleteUseCase가 개별 댓글 삭제 요청 시 호출합니다.</p>
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
     * <h3>사용자 탈퇴 시 댓글 처리</h3>
     * <p>사용자 탈퇴 시 해당 사용자의 모든 댓글에 대해 적절한 처리를 JPA로 수행합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제 + 익명화</p>
     * <p>자손이 없는 댓글: 하드 삭제</p>
     * <p>UserDeleteUseCase가 회원탈퇴 시 호출하여 댓글 정리 작업을 수행합니다.</p>
     *
     * @param userId 탈퇴하는 사용자 ID
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
     * <p>특정 사용자가 작성한 모든 댓글 ID 목록을 JPA로 조회합니다.</p>
     * <p>삭제 로직에서 활용하기 위해 삭제 포트에 포함되었습니다.</p>
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
     * <p>특정 사용자가 작성한 모든 댓글을 JPA로 익명화 처리합니다.</p>
     * <p>사용자 탈퇴 시 호출되며, 일종의 소프트 삭제로 간주하여 삭제 관련 포트에 포함되었습니다.</p>
     *
     * @param userId 익명화할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void anonymizeUserComments(Long userId) {
        commentRepository.anonymizeUserComments(userId);
    }



    /**
     * <h3>사용자 댓글 배치 소프트 삭제</h3>
     * <p>자손이 있는 사용자 댓글들을 JPA로 배치 소프트 삭제합니다.</p>
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
     * <h3>사용자 댓글 배치 하드 삭제</h3>
     * <p>자손이 없는 사용자 댓글들을 JPA로 배치 하드 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void batchHardDeleteUserCommentsWithoutDescendants(Long userId) {
        commentRepository.batchHardDeleteUserCommentsWithoutDescendants(userId);
    }

    /**
     * <h3>조건부 소프트 삭제</h3>
     * <p>자손이 있는 댓글에 대해서만 JPA로 소프트 삭제를 수행합니다.</p>
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
     * <h3>클로저 테이블에서 자손 관계 삭제</h3>
     * <p>자손이 없는 댓글의 모든 클로저 관계를 JPA로 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteClosuresByDescendantId(Long commentId) {
        commentRepository.deleteClosuresByDescendantId(commentId);
    }

    /**
     * <h3>댓글 하드 삭제</h3>
     * <p>자손이 없는 댓글을 JPA로 완전히 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void hardDeleteComment(Long commentId) {
        commentRepository.hardDeleteComment(commentId);
    }
}