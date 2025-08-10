package jaeik.growfarm.repository.comment.read;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.comment.QCommentClosure;
import jaeik.growfarm.entity.comment.QCommentLike;
import jaeik.growfarm.repository.comment.CommentBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * <h2>댓글 조회 저장소 구현체</h2>
 * <p>
 * 댓글 읽기 및 조회 기능 구현
 * 기존 CommentCustomRepositoryImpl에서 읽기 관련 로직 분리
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public class CommentReadRepositoryImpl extends CommentBaseRepository implements CommentReadRepository {

    public CommentReadRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        super(jpaQueryFactory);
    }

    @Override
    public List<Tuple> findCommentsWithLatestOrder(Long postId, Pageable pageable) {
        QComment comment = QComment.comment;
        QCommentClosure closure = QCommentClosure.commentClosure;
        QCommentLike commentLike = QCommentLike.commentLike;

        NumberExpression<Long> likeCount = getLikeCountExpression();

        return jpaQueryFactory
                .select(comment, likeCount, closure.depth, closure.depth, closure.ancestor.id)
                .from(closure)
                .leftJoin(closure.descendant, comment)
                .leftJoin(commentLike).on(commentLike.comment.id.eq(comment.id))
                .where(postIdCondition(postId))
                .groupBy(comment.id, closure.depth, closure.ancestor.id)
                .orderBy(closure.ancestor.id.asc().nullsFirst(), closure.depth.asc(), comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<Tuple> findPopularComments(Long postId) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;
        QCommentClosure closure = QCommentClosure.commentClosure;

        NumberExpression<Long> likeCount = getLikeCountExpression();

        return jpaQueryFactory
                .select(comment, likeCount, closure.ancestor.id)
                .from(comment)
                .leftJoin(commentLike).on(commentLike.comment.id.eq(comment.id))
                .leftJoin(closure).on(closure.descendant.id.eq(comment.id).and(closure.depth.eq(0)))
                .where(postIdCondition(postId)
                        .and(notDeletedCondition()))
                .groupBy(comment.id, closure.ancestor.id)
                .having(likeCount.goe(POPULAR_COMMENT_MIN_LIKES))
                .orderBy(likeCount.desc(), comment.createdAt.desc())
                .limit(POPULAR_COMMENT_LIMIT)
                .fetch();
    }

    @Override
    public Long countRootCommentsByPostId(Long postId) {
        QComment comment = QComment.comment;
        QCommentClosure closure = QCommentClosure.commentClosure;

        return jpaQueryFactory
                .select(comment.count())
                .from(closure)
                .leftJoin(closure.descendant, comment)
                .where(postIdCondition(postId)
                        .and(rootCommentCondition()))
                .fetchOne();
    }

    @Override
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return getCommentCountsByPostIds(postIds);
    }
}