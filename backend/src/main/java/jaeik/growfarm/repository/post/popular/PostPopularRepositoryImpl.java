package jaeik.growfarm.repository.post.popular;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.post.PostCacheFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostBaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>인기글 관리 구현체</h2>
 * <p>
 * 인기글 선정 및 관리 기능을 담당하는 레포지터리
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Repository
public class PostPopularRepositoryImpl extends PostBaseRepository implements PostPopularRepository {

    public PostPopularRepositoryImpl(JPAQueryFactory jpaQueryFactory,
                                     CommentRepository commentRepository) {
        super(jpaQueryFactory, commentRepository);
    }

    /**
     * <h3>실시간 인기글 선정</h3>
     * <p>
     * 1일 이내의 글 중 추천 수가 가장 높은 상위 5개를 실시간 인기글로 등록한다.
     * </p>
     *
     * @return 실시간 인기글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public List<SimplePostResDTO> updateRealtimePopularPosts() {
        return updatePopularPosts(1, PostCacheFlag.REALTIME);
    }

    /**
     * <h3>주간 인기글 선정</h3>
     * <p>
     * 7일 이내의 글 중 추천 수가 가장 높은 상위 5개를 주간 인기글로 등록한다.
     * </p>
     *
     * @return 주간 인기글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public List<SimplePostResDTO> updateWeeklyPopularPosts() {
        return updatePopularPosts(7, PostCacheFlag.WEEKLY);
    }

    /**
     * <h3>레전드 게시글 선정</h3>
     * <p>
     * 추천 수가 20개 이상인 모든 게시글을 레전드 게시글로 등록한다.
     * </p>
     *
     * @return 레전드 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public List<SimplePostResDTO> updateLegendPosts() {
        QPostLike postLike = QPostLike.postLike;

        resetPopularFlag(PostCacheFlag.LEGEND);

        List<SimplePostResDTO> legendPosts = createBasePopularPostsQuery()
                .having(postLike.countDistinct().goe(20))
                .orderBy(postLike.countDistinct().desc())
                .limit(50)
                .fetch();

        if (legendPosts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> legendPostIds = legendPosts.stream()
                .map(SimplePostResDTO::getPostId)
                .collect(Collectors.toList());

        applyPopularFlag(legendPostIds, PostCacheFlag.LEGEND);

        return legendPosts;
    }

    /**
     * <h3>인기글 선정 공통 로직</h3>
     * <p>
     * 지정된 기간 이내의 글 중 추천 수가 가장 높은 상위 5개를 인기글로 등록한다.
     * </p>
     *
     * @param days        조회 기간 (일 단위)
     * @param postCacheFlag 설정할 인기글 플래그
     * @return 인기글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<SimplePostResDTO> updatePopularPosts(int days, PostCacheFlag postCacheFlag) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        resetPopularFlag(postCacheFlag);

        List<SimplePostResDTO> popularPosts = createBasePopularPostsQuery()
                .where(post.createdAt.after(Instant.now().minus(days, ChronoUnit.DAYS)))
                .orderBy(postLike.countDistinct().desc())
                .limit(5)
                .fetch();

        if (popularPosts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> popularPostIds = popularPosts.stream()
                .map(SimplePostResDTO::getPostId)
                .collect(Collectors.toList());

        applyPopularFlag(popularPostIds, postCacheFlag);

        return popularPosts;
    }

    /**
     * <h3>인기글 공통 쿼리 빌더</h3>
     * <p>
     * 인기글 조회에 필요한 공통 SELECT, FROM, JOIN, GROUP BY 절을 구성한다.
     * 이 메서드는 {@link #updatePopularPosts(int, PostCacheFlag)}와
     * {@link #updateLegendPosts()}에서 재사용된다.
     * </p>
     *
     * @return {@link SimplePostResDTO}를 반환하는 JPAQuery 객체
     * @author Jaeik
     * @since 2.0.0
     */
    private JPAQuery<SimplePostResDTO> createBasePopularPostsQuery() {
        QPost post = QPost.post;
        QUsers user = QUsers.users;
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
                        post.user.id,
                        user.userName,
                        comment.countDistinct().intValue(),
                        postLike.countDistinct().intValue()))
                .from(post)
                .leftJoin(post.user, user)
                .join(postLike).on(post.id.eq(postLike.post.id))
                .leftJoin(comment).on(post.id.eq(comment.post.id))
                .groupBy(
                        post.id,
                        post.title,
                        post.views,
                        post.isNotice,
                        post.postCacheFlag,
                        post.createdAt,
                        post.user.id,
                        user.userName
                );
    }
    
    /**
     * <h3>인기글 플래그 초기화</h3>
     * <p>
     * 특정 인기글 플래그를 가진 게시글들의 플래그를 null로 초기화한다.
     * </p>
     *
     * @param postCacheFlag 초기화할 인기글 플래그
     * @author Jaeik
     * @since 2.0.0
     */
    private void resetPopularFlag(PostCacheFlag postCacheFlag) {
        QPost post = QPost.post;
        jpaQueryFactory.update(post)
                .set(post.postCacheFlag, (PostCacheFlag) null)
                .where(post.postCacheFlag.eq(postCacheFlag))
                .execute();
    }
    
    /**
     * <h3>인기글 플래그 설정 </h3>
     * <p>
     * 지정된 게시글들에 인기글 플래그를 설정한다.
     * </p>
     *
     * @param postIds     게시글 ID 목록
     * @param postCacheFlag 설정할 인기글 플래그
     * @author Jaeik
     * @since 2.0.0
     */
    private void applyPopularFlag(List<Long> postIds, PostCacheFlag postCacheFlag) {
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
