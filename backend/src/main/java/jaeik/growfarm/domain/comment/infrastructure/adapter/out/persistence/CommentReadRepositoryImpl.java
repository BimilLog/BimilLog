package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.comment.entity.QComment;
import jaeik.growfarm.domain.comment.entity.QCommentClosure;
import jaeik.growfarm.domain.comment.entity.QCommentLike;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CommentReadRepositoryImpl implements CommentReadRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<CommentDTO> findCommentsWithLatestOrder(Long postId, Pageable pageable, List<Long> likedCommentIds) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;
        QCommentClosure closure = QCommentClosure.commentClosure;
        QUser user = QUser.user;

        List<CommentDTO> content = jpaQueryFactory
                .select(Projections.constructor(CommentDTO.class,
                        comment.id,
                        comment.post.id,
                        comment.user.id,
                        user.userName,
                        comment.content,
                        comment.deleted,
                        comment.password,
                        comment.createdAt,
                        closure.ancestor.id
                ))
                .from(comment)
                .leftJoin(comment.user, user)
                .leftJoin(closure).on(comment.id.eq(closure.descendant.id))
                .where(comment.post.id.eq(postId))
                .groupBy(comment.id, user.userName, closure.ancestor.id)
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        content.forEach(dto -> {
            dto.setUserLike(likedCommentIds.contains(dto.getId()));
            // like count는 별도로 조회해야 함
        });

        Long total = countRootCommentsByPostId(postId);

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public List<CommentDTO> findPopularComments(Long postId, List<Long> likedCommentIds) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;
        QCommentClosure closure = QCommentClosure.commentClosure;
        QUser user = QUser.user;

        List<CommentDTO> popularComments = jpaQueryFactory
                .select(Projections.constructor(CommentDTO.class,
                        comment.id,
                        comment.post.id,
                        comment.user.id,
                        user.userName,
                        comment.content,
                        comment.deleted,
                        comment.password,
                        comment.createdAt,
                        closure.ancestor.id
                ))
                .from(comment)
                .leftJoin(comment.user, user)
                .leftJoin(commentLike).on(comment.id.eq(commentLike.comment.id))
                .leftJoin(closure).on(comment.id.eq(closure.descendant.id).and(closure.depth.eq(0)))
                .where(comment.post.id.eq(postId))
                .groupBy(comment.id, user.userName, closure.ancestor.id)
                .having(commentLike.countDistinct().goe(5)) // 좋아요 5개 이상
                .orderBy(commentLike.countDistinct().desc())
                .limit(5)
                .fetch();

        popularComments.forEach(dto -> {
            dto.setUserLike(likedCommentIds.contains(dto.getId()));
            dto.setPopular(true);
            // like count는 별도로 조회해야 함
        });
        return popularComments;
    }

    @Override
    public Long countRootCommentsByPostId(Long postId) {
        QComment comment = QComment.comment;
        QCommentClosure closure = QCommentClosure.commentClosure;

        return jpaQueryFactory
                .select(comment.countDistinct())
                .from(comment)
                .join(closure).on(comment.id.eq(closure.descendant.id))
                .where(comment.post.id.eq(postId).and(closure.depth.eq(0)))
                .fetchOne();
    }

    @Override
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        QComment comment = QComment.comment;

        List<Tuple> results = jpaQueryFactory
                .select(comment.post.id, comment.count())
                .from(comment)
                .where(comment.post.id.in(postIds))
                .groupBy(comment.post.id)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(comment.post.id),
                        tuple -> tuple.get(comment.count()).intValue()
                ));
    }

    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;
        QUser user = QUser.user;

        List<SimpleCommentDTO> content = jpaQueryFactory
                .select(Projections.constructor(SimpleCommentDTO.class,
                        comment.id,
                        comment.content,
                        user.userName,
                        comment.createdAt,
                        comment.post.id,
                        comment.post.title))
                .from(comment)
                .join(commentLike).on(comment.id.eq(commentLike.comment.id))
                .leftJoin(comment.user, user)
                .where(commentLike.user.id.eq(userId))
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(comment.countDistinct())
                .from(comment)
                .join(commentLike).on(comment.id.eq(commentLike.comment.id))
                .where(commentLike.user.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable) {
        QComment comment = QComment.comment;
        QUser user = QUser.user;

        List<SimpleCommentDTO> content = jpaQueryFactory
                .select(Projections.constructor(SimpleCommentDTO.class,
                        comment.id,
                        comment.content,
                        user.userName,
                        comment.createdAt,
                        comment.post.id,
                        comment.post.title))
                .from(comment)
                .leftJoin(comment.user, user)
                .where(comment.user.id.eq(userId))
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.user.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}