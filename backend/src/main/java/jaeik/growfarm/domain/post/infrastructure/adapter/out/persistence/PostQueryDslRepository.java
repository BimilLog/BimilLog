package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.comment.entity.QComment;
import jaeik.growfarm.domain.post.entity.QPost;
import jaeik.growfarm.domain.post.entity.QPostLike;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostQueryDslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<SimplePostResDTO> findSimplePost(Pageable pageable) {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QComment comment = QComment.comment;
        QPostLike postLike = QPostLike.postLike;

        List<SimplePostResDTO> content = jpaQueryFactory
                .select(Projections.constructor(SimplePostResDTO.class,
                        post.id,
                        post.title,
                        post.views.coalesce(0),
                        post.isNotice,
                        post.postCacheFlag,
                        post.createdAt,
                        user.id,
                        user.userName,
                        comment.countDistinct().intValue(),
                        postLike.countDistinct().intValue()))
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(comment).on(post.id.eq(comment.post.id))
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(post.isNotice.isFalse())
                .groupBy(post.id, user.id)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .where(post.isNotice.isFalse())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    public Page<SimplePostResDTO> searchPosts(String query, String type, Pageable pageable) {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QComment comment = QComment.comment;
        QPostLike postLike = QPostLike.postLike;

        BooleanExpression searchCondition = getSearchCondition(post, user, type, query);

        List<SimplePostResDTO> content = jpaQueryFactory
                .select(Projections.constructor(SimplePostResDTO.class,
                        post.id,
                        post.title,
                        post.views.coalesce(0),
                        post.isNotice,
                        post.postCacheFlag,
                        post.createdAt,
                        user.id,
                        user.userName,
                        comment.countDistinct().intValue(),
                        postLike.countDistinct().intValue()))
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(comment).on(post.id.eq(comment.post.id))
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(searchCondition)
                .groupBy(post.id, user.id)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.user, user)
                .where(searchCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    public Page<SimplePostResDTO> findPostsByUserId(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QComment comment = QComment.comment;
        QPostLike postLike = QPostLike.postLike;

        List<SimplePostResDTO> content = jpaQueryFactory
                .select(Projections.constructor(SimplePostResDTO.class,
                        post.id,
                        post.title,
                        post.views.coalesce(0),
                        post.isNotice,
                        post.postCacheFlag,
                        post.createdAt,
                        user.id,
                        user.userName,
                        comment.countDistinct().intValue(),
                        postLike.countDistinct().intValue()))
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(comment).on(post.id.eq(comment.post.id))
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(user.id.eq(userId))
                .groupBy(post.id, user.id)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .where(post.user.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    public Page<SimplePostResDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QComment comment = QComment.comment;
        QPostLike postLike = QPostLike.postLike;
        QPostLike userPostLike = new QPostLike("userPostLike");

        List<SimplePostResDTO> content = jpaQueryFactory
                .select(Projections.constructor(SimplePostResDTO.class,
                        post.id,
                        post.title,
                        post.views.coalesce(0),
                        post.isNotice,
                        post.postCacheFlag,
                        post.createdAt,
                        user.id,
                        user.userName,
                        comment.countDistinct().intValue(),
                        postLike.countDistinct().intValue()))
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(comment).on(post.id.eq(comment.post.id))
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .join(userPostLike).on(post.id.eq(userPostLike.post.id).and(userPostLike.user.id.eq(userId)))
                .groupBy(post.id, user.id)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(post.countDistinct())
                .from(post)
                .join(userPostLike).on(post.id.eq(userPostLike.post.id).and(userPostLike.user.id.eq(userId)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression getSearchCondition(QPost post, QUser user, String type, String query) {
        return switch (type) {
            case "title" -> post.title.containsIgnoreCase(query);
            case "content" -> post.content.containsIgnoreCase(query);
            case "titleContent" -> post.title.containsIgnoreCase(query).or(post.content.containsIgnoreCase(query));
            case "writer" -> user.userName.containsIgnoreCase(query);
            default -> post.title.containsIgnoreCase(query).or(post.content.containsIgnoreCase(query));
        };
    }
}
