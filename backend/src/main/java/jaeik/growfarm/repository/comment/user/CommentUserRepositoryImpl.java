package jaeik.growfarm.repository.comment.user;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.comment.QCommentLike;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.repository.comment.CommentBaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>댓글 사용자별 조회 저장소 구현체</h2>
 * <p>
 * 사용자별 댓글 조회 기능 구현
 * 기존 CommentCustomRepositoryImpl에서 사용자 관련 로직 분리
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public class CommentUserRepositoryImpl extends CommentBaseRepository implements CommentUserRepository {

    public CommentUserRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        super(jpaQueryFactory);
    }

    @Override
    public Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable) {
        QComment comment = QComment.comment;
        QPost post = QPost.post;

        List<SimpleCommentDTO> comments = jpaQueryFactory
                .select(Projections.constructor(SimpleCommentDTO.class,
                        comment.id,
                        comment.content,
                        comment.createdAt,
                        post.id,
                        post.title))
                .from(comment)
                .leftJoin(comment.post, post)
                .where(userIdCondition(userId)
                        .and(notDeletedCondition()))
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(comment.count())
                .from(comment)
                .where(userIdCondition(userId)
                        .and(notDeletedCondition()))
                .fetchOne();

        return new PageImpl<>(comments, pageable, totalCount != null ? totalCount : 0L);
    }

    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;
        QPost post = QPost.post;

        List<SimpleCommentDTO> comments = jpaQueryFactory
                .select(Projections.constructor(SimpleCommentDTO.class,
                        comment.id,
                        comment.content,
                        comment.createdAt,
                        post.id,
                        post.title))
                .from(commentLike)
                .leftJoin(commentLike.comment, comment)
                .leftJoin(comment.post, post)
                .where(commentLike.user.id.eq(userId)
                        .and(notDeletedCondition()))
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(commentLike.count())
                .from(commentLike)
                .leftJoin(commentLike.comment, comment)
                .where(commentLike.user.id.eq(userId)
                        .and(notDeletedCondition()))
                .fetchOne();

        return new PageImpl<>(comments, pageable, totalCount != null ? totalCount : 0L);
    }

    @Override
    public List<Long> findUserLikedCommentIds(List<Long> commentIds, Long userId) {
        if (commentIds == null || commentIds.isEmpty()) {
            return List.of();
        }

        QCommentLike commentLike = QCommentLike.commentLike;

        return jpaQueryFactory
                .select(commentLike.comment.id)
                .from(commentLike)
                .where(commentLike.comment.id.in(commentIds)
                        .and(commentLike.user.id.eq(userId)))
                .fetch();
    }
}