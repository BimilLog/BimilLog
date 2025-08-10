package jaeik.growfarm.repository.comment.admin;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.comment.QCommentClosure;
import jaeik.growfarm.repository.comment.CommentBaseRepository;
import jaeik.growfarm.repository.comment.CommentClosureRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>댓글 관리자 저장소 구현체</h2>
 * <p>
 * 관리자 전용 댓글 관리 기능 구현
 * 기존 CommentCustomRepositoryImpl에서 관리자 관련 로직 분리
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public class CommentAdminRepositoryImpl extends CommentBaseRepository implements CommentAdminRepository {

    private final CommentClosureRepository commentClosureRepository;

    public CommentAdminRepositoryImpl(JPAQueryFactory jpaQueryFactory, CommentClosureRepository commentClosureRepository) {
        super(jpaQueryFactory);
        this.commentClosureRepository = commentClosureRepository;
    }

    @Override
    @Transactional
    public void processUserCommentsOnWithdrawal(Long userId) {
        QComment comment = QComment.comment;
        QCommentClosure closure = QCommentClosure.commentClosure;

        // 1. 사용자의 모든 댓글 ID 조회
        List<Long> allCommentIds = jpaQueryFactory
                .select(comment.id)
                .from(comment)
                .where(comment.user.id.eq(userId))
                .fetch();

        if (allCommentIds.isEmpty()) {
            return; // 처리할 댓글이 없으면 종료
        }

        // 2. 자손 댓글이 있는 댓글 ID 조회 (삭제하면 안 되는 댓글)
        List<Long> commentsWithDescendants = jpaQueryFactory
                .select(closure.ancestor.id)
                .from(closure)
                .where(closure.ancestor.id.in(allCommentIds)
                        .and(closure.depth.gt(0))) // 자손이 있는 경우 (depth > 0)
                .groupBy(closure.ancestor.id)
                .fetch();

        // 3. 자손 댓글이 없는 댓글 ID (물리적으로 삭제할 댓글)
        List<Long> commentsToDelete = allCommentIds.stream()
                .filter(id -> !commentsWithDescendants.contains(id))
                .toList();

        // 4. 자손이 있는 댓글은 내용 변경 및 작성자 정보 null 처리 (벌크 업데이트)
        if (!commentsWithDescendants.isEmpty()) {
            jpaQueryFactory
                    .update(comment)
                    .set(comment.content, DELETED_MESSAGE)
                    .setNull(comment.user)
                    .where(comment.id.in(commentsWithDescendants))
                    .execute();
        }

        // 5. 자손이 없는 댓글은 물리적 삭제 (벌크 삭제)
        if (!commentsToDelete.isEmpty()) {
            // 5-1) closure 테이블 정리
            commentClosureRepository.deleteByDescendantIds(commentsToDelete);
            // 5-2) 댓글 삭제
            jpaQueryFactory
                    .delete(comment)
                    .where(comment.id.in(commentsToDelete))
                    .execute();
        }
    }

    @Override
    public Long findUserIdByCommentId(Long commentId) {
        QComment comment = QComment.comment;

        return jpaQueryFactory
                .select(comment.user.id)
                .from(comment)
                .where(comment.id.eq(commentId))
                .fetchOne();
    }
}