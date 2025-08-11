package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.domain.comment.domain.QComment;
import jaeik.growfarm.domain.comment.domain.QCommentClosure;
import jaeik.growfarm.domain.comment.domain.QCommentLike;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CommentReadRepositoryImpl implements CommentReadRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Tuple> findCommentsWithLatestOrder(Long postId, Pageable pageable) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;
        QCommentClosure closure = QCommentClosure.commentClosure;

        return queryFactory
                .select(
                        comment,
                        commentLike.id.count(),
                        closure.ancestor.id,
                        closure.depth
                )
                .from(comment)
                .leftJoin(commentLike).on(commentLike.comment.id.eq(comment.id))
                .join(closure).on(closure.descendant.id.eq(comment.id))
                .where(comment.post.id.eq(postId))
                .groupBy(comment.id)
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<Tuple> findPopularComments(Long postId) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;
        QCommentClosure closure = QCommentClosure.commentClosure;

        return queryFactory
                .select(
                        comment,
                        commentLike.id.countDistinct(),
                        closure.ancestor.id
                )
                .from(comment)
                .join(closure).on(closure.descendant.id.eq(comment.id))
                .leftJoin(commentLike).on(commentLike.comment.id.eq(comment.id))
                .where(comment.post.id.eq(postId)
                        .and(closure.depth.eq(0))
                        .and(comment.deleted.isFalse()))
                .groupBy(comment.id)
                .orderBy(commentLike.id.countDistinct().desc())
                .limit(3)
                .fetch();
    }

    @Override
    public Long countRootCommentsByPostId(Long postId) {
        QComment comment = QComment.comment;
        QCommentClosure closure = QCommentClosure.commentClosure;

        return queryFactory
                .select(comment.id.count())
                .from(comment)
                .join(closure).on(closure.descendant.id.eq(comment.id))
                .where(comment.post.id.eq(postId)
                        .and(closure.depth.eq(0))
                        .and(comment.deleted.isFalse()))
                .fetchOne();
    }

    @Override
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return null;
    }

    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        return null;
    }
}
