package jaeik.growfarm.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.service.redis.RedisPostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>사용자별 게시글 조회 및 관리 구현체</h2>
 * <p>
 * 사용자가 작성한 글, 추천한 글 조회 기능과 게시글 삭제 및 캐시 동기화 기능을 담당하는 레포지터리
 * </p>
 *
 * @author Jaeik
 * @version 1.0.15
 */
@Slf4j
@Repository
public class PostCustomUserRepositoryImpl extends PostCustomBaseRepository {

    private final RedisPostService redisPostService;

    public PostCustomUserRepositoryImpl(JPAQueryFactory jpaQueryFactory,
                                        CommentRepository commentRepository,
                                        RedisPostService redisPostService) {
        super(jpaQueryFactory, commentRepository);
        this.redisPostService = redisPostService;
    }

    /**
     * <h3>사용자 작성 글 목록 조회</h3>
     * <p>
     * 사용자 ID를 기준으로 해당 사용자가 작성한 글 목록을 조회한다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 사용자가 작성한 글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> findPostsByUserId(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;

        BooleanExpression userCondition = post.user.id.eq(userId);
        BooleanExpression baseCondition = post.isNotice.eq(false);
        BooleanExpression finalCondition = baseCondition.and(userCondition);

        List<Tuple> postTuples = fetchPosts(post, user, finalCondition, pageable);
        Long total = fetchTotalCount(post, user, finalCondition);

        return processPostTuples(postTuples, post, user, pageable, false, total);
    }

    /**
     * <h3>사용자가 추천한 글 목록 조회</h3>
     * <p>
     * 사용자 ID를 기준으로 해당 사용자가 추천한 글 목록을 조회한다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 사용자가 추천한 글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;
        QPostLike postLike = QPostLike.postLike;

        List<Tuple> postTuples = jpaQueryFactory
                .select(
                        post.id,
                        post.title,
                        post.views.coalesce(0),
                        post.isNotice,
                        post.popularFlag,
                        post.createdAt,
                        post.user.id,
                        user.userName)
                .from(post)
                .join(post.user, user)
                .join(postLike).on(postLike.post.id.eq(post.id))
                .where(postLike.user.id.eq(userId))
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .join(postLike).on(postLike.post.id.eq(post.id))
                .where(postLike.user.id.eq(userId))
                .fetchOne();

        return processPostTuples(postTuples, post, user, pageable, true, total);
    }

    /**
     * <h3>게시글 삭제 및 Redis 캐시 동기화</h3>
     * <p>
     * 게시글을 삭제하고, 해당 게시글이 인기글(실시간/주간/레전드)인 경우
     * Redis 캐시에서도 즉시 해당 인기글 목록을 삭제한다.
     * </p>
     *
     * @param postId 삭제할 게시글 ID
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void deletePostWithCacheSync(Long postId) {
        QPost post = QPost.post;

        try {
            PopularFlag popularFlag = jpaQueryFactory
                    .select(post.popularFlag)
                    .from(post)
                    .where(post.id.eq(postId))
                    .fetchOne();

            long deletedCount = jpaQueryFactory
                    .delete(post)
                    .where(post.id.eq(postId))
                    .execute();

            if (deletedCount > 0 && popularFlag != null) {
                deleteRelatedRedisCache(popularFlag);
            }

        } catch (Exception e) {
            log.error("게시글 삭제 중 오류 발생. postId: {}", postId, e);
            throw new CustomException(ErrorCode.POST_DELETE_FAILED, e);
        }
    }

    /**
     * <h3>관련 Redis 캐시 삭제</h3>
     * <p>
     * PopularFlag에 따라 해당하는 Redis 캐시를 삭제한다.
     * </p>
     *
     * @param popularFlag 삭제된 게시글의 PopularFlag
     * @author Jaeik
     * @since 1.0.0
     */
    private void deleteRelatedRedisCache(PopularFlag popularFlag) {
        try {
            switch (popularFlag) {
                case REALTIME:
                    redisPostService.deletePopularPostsCache(
                            RedisPostService.PopularPostType.REALTIME);
                    log.info("실시간 인기글 Redis 캐시 삭제 완료");
                    break;
                case WEEKLY:
                    redisPostService.deletePopularPostsCache(
                            RedisPostService.PopularPostType.WEEKLY);
                    log.info("주간 인기글 Redis 캐시 삭제 완료");
                    break;
                case LEGEND:
                    redisPostService.deletePopularPostsCache(
                            RedisPostService.PopularPostType.LEGEND);
                    log.info("레전드 게시글 Redis 캐시 삭제 완료");
                    break;
                default:
                    log.debug("PopularFlag가 없는 게시글이므로 캐시 삭제 불필요");
                    break;
            }
        } catch (Exception e) {
            log.error("Redis 캐시 삭제 중 오류 발생. popularFlag: {}", popularFlag, e);
            throw new CustomException(ErrorCode.REDIS_DELETE_ERROR, e);
        }
    }
}