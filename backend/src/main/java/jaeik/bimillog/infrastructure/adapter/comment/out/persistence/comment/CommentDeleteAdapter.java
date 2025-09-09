package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentDeletePort;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>댓글 삭제 어댑터</h2>
 * <p>댓글 및 댓글 클로저 엔티티의 삭제 및 익명화를 처리하는 아웃바운드 어댑터 구현체</p>
 * <p>관심사별로 분리된 삭제 전용 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentDeleteAdapter implements CommentDeletePort {

    private final CommentRepository commentRepository;

    /**
     * <h3>게시글 ID로 모든 댓글 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 댓글을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllByPostId(Long postId) {
        commentRepository.deleteAllByPostId(postId);
    }

    /**
     * <h3>사용자 댓글 익명화</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 익명화 처리합니다. (사용자 탈퇴 시 호출)</p>
     * <p>일종의 소프트 삭제로 간주하여 삭제 관련 포트에 포함</p>
     *
     * @param userId 익명화할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void anonymizeUserComments(Long userId) {
        commentRepository.anonymizeUserComments(userId);
    }

    /**
     * <h3>사용자 댓글 ID 목록 조회</h3>
     * <p>특정 사용자가 작성한 모든 댓글 ID 목록을 조회합니다.</p>
     * <p>삭제 로직에서 활용하기 위해 삭제 포트에 포함</p>
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
     * <h3>사용자 탈퇴 시 댓글 처리</h3>
     * <p>사용자 탈퇴 시 해당 사용자의 모든 댓글에 대해 적절한 처리를 수행합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제 + 익명화</p>
     * <p>자손이 없는 댓글: 하드 삭제</p>
     * <p>성능 최적화: 배치 쿼리로 N+1 문제 해결</p>
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
        int hardDeletedCount = batchHardDeleteUserCommentsWithoutDescendants(userId);
        
        // 3. 소프트 삭제된 댓글들은 익명화 처리
        if (softDeletedCount > 0) {
            anonymizeUserComments(userId);
        }
    }

    /**
     * <h3>사용자 댓글 배치 소프트 삭제</h3>
     * <p>자손이 있는 사용자 댓글들을 배치로 소프트 삭제합니다.</p>
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
     * <p>자손이 없는 사용자 댓글들을 배치로 하드 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @return int 하드 삭제된 댓글 수
     * @author Jaeik
     * @since 2.0.0
     */
    private int batchHardDeleteUserCommentsWithoutDescendants(Long userId) {
        return commentRepository.batchHardDeleteUserCommentsWithoutDescendants(userId);
    }

    /**
     * <h3>조건부 소프트 삭제</h3>
     * <p>자손이 있는 댓글에 대해서만 소프트 삭제를 수행합니다.</p>
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
     * <p>자손이 없는 댓글의 모든 클로저 관계를 삭제합니다.</p>
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
     * <p>자손이 없는 댓글을 완전히 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void hardDeleteComment(Long commentId) {
        commentRepository.hardDeleteComment(commentId);
    }


    /**
     * <h3>댓글 삭제 처리 (하드/소프트 삭제)</h3>
     * <p>댓글 ID를 기반으로 자손이 있는지 확인하여 적절한 삭제 방식을 선택합니다.</p>
     * <p>자손이 없으면 하드 삭제를, 있으면 소프트 삭제를 수행합니다.</p>
     * <p>성능 최적화: 하나의 쿼리로 조건부 처리</p>
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
}