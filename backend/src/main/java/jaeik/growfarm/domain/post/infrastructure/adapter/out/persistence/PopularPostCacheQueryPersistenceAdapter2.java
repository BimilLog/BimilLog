package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.post.application.port.out.PostCacheQueryPort2;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.QPost;
import jaeik.growfarm.domain.comment.entity.QComment;
import jaeik.growfarm.domain.post.entity.QPostLike;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

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
public class PopularPostCacheQueryPersistenceAdapter2 implements PostCacheQueryPort2 {

    private final JPAQueryFactory jpaQueryFactory;
    private final PostJpaRepository postJpaRepository;

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
        return findPopularPostsByDays(1, PostCacheFlag.REALTIME);
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
        return findPopularPostsByDays(7, PostCacheFlag.WEEKLY);
    }

    /**
     * <h3>전설의 게시글 조회</h3>
     * <p>좋아요 수가 20개 이상인 게시글 중 가장 좋아요 수가 많은 상위 50개 게시글을 조회합니다.</p>
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
     * <h3>공지사항 게시글 조회</h3>
     * <p>공지사항으로 설정된 게시글 목록을 최신순으로 조회합니다.</p>
     *
     * @return 공지사항 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
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
                        post.content,
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
    
    /**
     * <h3>기간별 인기 게시글 조회</h3>
     * <p>주어진 기간(일) 내에 좋아요 수가 많은 게시글을 조회합니다. 결과는 5개로 제한됩니다.</p>
     *
     * @param days            기간(일)
     * @param postCacheFlag 캐시 플래그 (현재는 사용되지 않으나 파라미터로 존재)
     * @return 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<SimplePostResDTO> findPopularPostsByDays(int days, PostCacheFlag postCacheFlag) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;
        
        return createBasePopularPostsQuery()
                .where(post.createdAt.after(Instant.now().minus(days, ChronoUnit.DAYS)))
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
    private JPAQuery<SimplePostResDTO> createBasePopularPostsQuery() {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QPostLike postLike = QPostLike.postLike;
        QComment comment = QComment.comment;

        return jpaQueryFactory
                .select(Projections.constructor(SimplePostResDTO.class,
                        post.id,
                        post.title,
                        post.content,
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
    
    /**
     * <h3>인기 플래그 초기화</h3>
     * <p>주어진 캐시 플래그에 해당하는 게시글들의 플래그를 초기화(null로 설정)합니다.</p>
     *
     * @param postCacheFlag 초기화할 캐시 플래그
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void resetPopularFlag(PostCacheFlag postCacheFlag) {
        QPost post = QPost.post;
        jpaQueryFactory.update(post)
                .set(post.postCacheFlag, (PostCacheFlag) null)
                .where(post.postCacheFlag.eq(postCacheFlag))
                .execute();
    }
    
    /**
     * <h3>인기 플래그 적용</h3>
     * <p>주어진 게시글 ID 목록에 특정 캐시 플래그를 적용합니다.</p>
     *
     * @param postIds       캐시 플래그를 적용할 게시글 ID 목록
     * @param postCacheFlag 적용할 캐시 플래그
     * @author Jaeik
     * @since 2.0.0
     */
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

    /**
     * <h3>공지사항 게시글 목록 조회</h3>
     * <p>모든 공지사항 게시글을 조회합니다.</p>
     * <p>임시 구현: PostJpaRepository에 직접 쿼리 메서드를 추가하는 것이 더 효율적입니다.</p>
     *
     * @return 공지사항 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<SimplePostResDTO> findNoticePosts2() {
        // 임시 구현: 실제로는 PostJpaRepository에 쿼리 메서드를 추가하는 것이 좋습니다.
        return postJpaRepository.findAll().stream()
                .filter(Post::isNotice)
                .map(post -> SimplePostResDTO.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .userName(post.getUser() != null ? post.getUser().getUserName() : "익명")
                        .createdAt(post.getCreatedAt())
                        .views(post.getViews())
                        .build())
                .collect(Collectors.toList());
    }
}
