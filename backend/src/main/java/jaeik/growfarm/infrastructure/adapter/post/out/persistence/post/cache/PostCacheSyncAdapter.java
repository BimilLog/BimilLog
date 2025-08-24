package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.cache;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.comment.entity.QComment;
import jaeik.growfarm.domain.post.application.port.out.PostCacheSyncPort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.QPost;
import jaeik.growfarm.domain.post.entity.QPostLike;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.FullPostResDTO;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * <h2>인기 게시글 영속성 어댑터</h2>
 * <p>인기 게시글 및 공지사항 조회, 인기 플래그 관리 기능을 제공하는 영속성 어댑터입니다.</p>
 * <p>LoadPopularPostPort 인터페이스를 구현하며, QueryDSL을 사용하여 데이터베이스와 상호작용합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PostCacheSyncAdapter implements PostCacheSyncPort {

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
    public List<SimplePostResDTO> findRealtimePopularPosts() {
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
    public List<SimplePostResDTO> findWeeklyPopularPosts() {
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
    public List<SimplePostResDTO> findLegendaryPosts() {
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
     * <p>🔧 수정 이력: 인기 게시글 기준 명확화 - 최소 1개 이상의 좋아요가 있는 게시글만 포함</p>
     *
     * @param days 기간(일)
     * @return 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<SimplePostResDTO> findPopularPostsByDays(int days) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        return createBasePopularPostsQuery()
                .where(post.createdAt.after(Instant.now().minus(days, ChronoUnit.DAYS)))
                .having(postLike.countDistinct().goe(1)) // 🔧 최소 1개 이상의 좋아요 필요
                .orderBy(postLike.countDistinct().desc())
                .limit(5)
                .fetch();
    }

    /**
     * <h3>기본 인기 게시글 쿼리 생성</h3>
     * <p>인기 게시글 조회를 위한 기본 QueryDSL 쿼리를 생성합니다.</p>
     * <p>🔧 수정 이력: INNER JOIN 문제 해결 - 좋아요가 없는 게시글도 포함하도록 LEFT JOIN 사용</p>
     *
     * @return 기본 JPAQuery 객체
     * @author Jaeik
     * @since 2.0.0
     */
    private JPAQuery<SimplePostResDTO> createBasePopularPostsQuery() {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QPostLike postLike = QPostLike.postLike;
        QComment comment = QComment.comment;

        return jpaQueryFactory
                .select(Projections.constructor(SimplePostResDTO.class,
                        post.id,                              // 1. id (Long)
                        post.title,                           // 2. title (String)
                        post.content,                         // 3. content (String)
                        post.views.coalesce(0),              // 4. viewCount (Integer)
                        postLike.countDistinct().intValue(), // 5. likeCount (Integer)
                        post.postCacheFlag,                  // 6. postCacheFlag (PostCacheFlag)
                        post.createdAt,                      // 7. createdAt (Instant)
                        user.id,                             // 8. userId (Long)
                        user.userName,                       // 9. userName (String)
                        comment.countDistinct().intValue(),  // 10. commentCount (Integer)
                        post.isNotice))                      // 11. isNotice (boolean)
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(comment).on(post.id.eq(comment.post.id))
                .leftJoin(postLike).on(post.id.eq(postLike.post.id)) // 🔧 INNER JOIN → LEFT JOIN 변경
                .groupBy(post.id, user.id);
    }


    /**
     * <h3>게시글 상세 조회</h3>
     * <p>게시글 ID를 기준으로 게시글 상세 정보를 조회합니다.</p>
     * <p>🔧 수정 이력: null 안전성 개선 - null postId 예외 처리 추가</p>
     *
     * @param postId 게시글 ID
     * @return 게시글 상세 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public FullPostResDTO findPostDetail(Long postId) {
        // 🔧 null 안전성 검사 추가
        if (postId == null) {
            return null;
        }
        
        QPost post = QPost.post;
        QUser user = QUser.user;
        QPostLike postLike = QPostLike.postLike;

        Post entity = jpaQueryFactory
                .selectFrom(post)
                .leftJoin(post.user, user).fetchJoin()
                .where(post.id.eq(postId))
                .fetchOne();

        if (entity == null) {
            return null;
        }

        long likeCount = jpaQueryFactory
                .select(postLike.count())
                .from(postLike)
                .where(postLike.post.id.eq(postId))
                .fetchOne() != null ? jpaQueryFactory.select(postLike.count())
                .from(postLike)
                .where(postLike.post.id.eq(postId))
                .fetchOne() : 0L;

        return FullPostResDTO.from(entity, Math.toIntExact(likeCount), false);
    }
}
