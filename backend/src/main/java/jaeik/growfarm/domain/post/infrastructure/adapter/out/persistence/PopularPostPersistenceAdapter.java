package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.post.application.port.out.LoadPopularPostPort;
import jaeik.growfarm.domain.post.domain.PostCacheFlag;
import jaeik.growfarm.domain.post.domain.QPost;
import jaeik.growfarm.domain.comment.domain.QComment;
import jaeik.growfarm.domain.post.domain.QPostLike;
import jaeik.growfarm.domain.user.domain.QUser;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PopularPostPersistenceAdapter implements LoadPopularPostPort {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    @Transactional(readOnly = true)
    public List<SimplePostResDTO> findRealtimePopularPosts() {
        return findPopularPostsByDays(1, PostCacheFlag.REALTIME);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimplePostResDTO> findWeeklyPopularPosts() {
        return findPopularPostsByDays(7, PostCacheFlag.WEEKLY);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimplePostResDTO> findLegendaryPosts() {
        QPostLike postLike = QPostLike.postLike;
        
        List<SimplePostResDTO> legendPosts = createBasePopularPostsQuery()
                .having(postLike.countDistinct().goe(20))
                .orderBy(postLike.countDistinct().desc())
                .limit(50)
                .fetch();

        return legendPosts;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SimplePostResDTO> findNoticePosts() {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QComment comment = QComment.comment;
        QPostLike postLike = QPostLike.postLike;

        return jpaQueryFactory
                .select(Projections.constructor(SimplePostResDTO.class,
                        post.id,
                        post.title,
                        post.views,
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
                .where(post.isNotice.isTrue())
                .groupBy(post.id, user.id)
                .orderBy(post.createdAt.desc())
                .fetch();
    }
    
    private List<SimplePostResDTO> findPopularPostsByDays(int days, PostCacheFlag postCacheFlag) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;
        
        return createBasePopularPostsQuery()
                .where(post.createdAt.after(Instant.now().minus(days, ChronoUnit.DAYS)))
                .orderBy(postLike.countDistinct().desc())
                .limit(5)
                .fetch();
    }

    private JPAQuery<SimplePostResDTO> createBasePopularPostsQuery() {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QPostLike postLike = QPostLike.postLike;
        QComment comment = QComment.comment;

        return jpaQueryFactory
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
                .join(postLike).on(post.id.eq(postLike.post.id))
                .groupBy(post.id, user.id);
    }
    
    @Override
    @Transactional
    public void resetPopularFlag(PostCacheFlag postCacheFlag) {
        QPost post = QPost.post;
        jpaQueryFactory.update(post)
                .set(post.postCacheFlag, (PostCacheFlag) null)
                .where(post.postCacheFlag.eq(postCacheFlag))
                .execute();
    }
    
    @Override
    @Transactional
    public void applyPopularFlag(List<Long> postIds, PostCacheFlag postCacheFlag) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        QPost post = QPost.post;
        jpaQueryFactory.update(post)
                .set(post.postCacheFlag, postCacheFlag)
                .where(post.id.in(postIds))
                .execute();
    }
}
