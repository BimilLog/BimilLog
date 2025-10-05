package jaeik.bimillog.infrastructure.adapter.out.redis;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.post.application.port.out.RedisPostSyncPort;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.post.entity.QPost;
import jaeik.bimillog.domain.post.entity.QPostLike;
import jaeik.bimillog.domain.member.entity.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * <h2>RedisPostSyncAdapter</h2>
 * <p>
 * Post 도메인의 캐시 동기화 포트를 QueryDSL 기술로 구현하는 아웃바운드 어댑터입니다.
 * </p>
 * <p>
 * PostCacheService에서 실시간, 주간, 레전드 인기글 캐싱을 위한 데이터 조회 시 호출
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class RedisPostSyncAdapter implements RedisPostSyncPort {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * <h3>실시간 인기 게시글 조회</h3>
     * <p>지난 1일간의 인기 게시글 목록을 조회합니다.</p>
     *
     * @return 실시간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<PostSearchResult> findRealtimePopularPosts() {
        return findPopularPostsByDays(1);
    }

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>지난 7일간의 인기 게시글 목록을 조회합니다.</p>
     *
     * @return 주간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<PostSearchResult> findWeeklyPopularPosts() {
        return findPopularPostsByDays(7);
    }

    /**
     * <h3>전설의 게시글 조회</h3>
     * <p>추천 수가 20개 이상인 게시글 중 가장 추천 수가 많은 상위 50개 게시글을 조회합니다.</p>
     *
     * @return 전설의 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<PostSearchResult> findLegendaryPosts() {
        QPostLike postLike = QPostLike.postLike;

        return createBasePopularPostsQuery()
                .having(postLike.countDistinct().goe(20))
                .orderBy(postLike.countDistinct().desc())
                .limit(50)
                .fetch();
    }


    /**
     * <h3>기간별 인기 게시글 조회</h3>
     * <p>주어진 기간(일) 내에 추천 수가 많은 게시글을 조회합니다. 결과는 5개로 제한됩니다.</p>
     *
     * @param days 기간(일)
     * @return 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<PostSearchResult> findPopularPostsByDays(int days) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        return createBasePopularPostsQuery()
                .where(post.createdAt.after(Instant.now().minus(days, ChronoUnit.DAYS)))
                .having(postLike.countDistinct().goe(1))
                .orderBy(postLike.countDistinct().desc())
                .limit(5)
                .fetch();
    }

    /**
     * <h3>기본 인기 게시글 쿼리 생성</h3>
     * <p>인기 게시글 조회를 위한 기본 QueryDSL 쿼리를 생성합니다.</p>
     *
     * @return 기본 JPAQuery 객체
     * @author Jaeik
     * @since 2.0.0
     */
    private JPAQuery<PostSearchResult> createBasePopularPostsQuery() {
        QPost post = QPost.post;
        QMember member = QMember.member;
        QPostLike postLike = QPostLike.postLike;
        QComment comment = QComment.comment;

        return jpaQueryFactory
                .select(Projections.constructor(PostSearchResult.class,
                        post.id,                              // 1. id (Long)
                        post.title,                           // 2. title (String)
                        post.views.coalesce(0),              // 3. viewCount (Integer)
                        postLike.countDistinct().intValue(), // 4. likeCount (Integer)
                        post.createdAt,                      // 5. createdAt (Instant)
                        member.id,                           // 6. memberId (Long)
                        member.memberName,                   // 7. memberName (String)
                        comment.countDistinct().intValue(),  // 8. commentCount (Integer)
                        post.isNotice))                      // 9. isNotice (boolean)
                .from(post)
                .leftJoin(post.member, member)
                .leftJoin(comment).on(post.id.eq(comment.post.id))
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .groupBy(post.id, member.id);
    }
}
