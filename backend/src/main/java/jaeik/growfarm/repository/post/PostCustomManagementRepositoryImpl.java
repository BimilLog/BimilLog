package jaeik.growfarm.repository.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.service.redis.RedisPostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 관리 구현체</h2>
 * <p>
 * 게시글 삭제 및 캐시 동기화 기능을 담당하는 레포지터리
 * </p>
 *
 * @author Jaeik
 * @version 1.0
 */
@Slf4j
@Repository
public class PostCustomManagementRepositoryImpl extends PostCustomBaseRepository {

    private final RedisPostService redisPostService;

    public PostCustomManagementRepositoryImpl(JPAQueryFactory jpaQueryFactory,
                                              CommentRepository commentRepository,
                                              RedisPostService redisPostService) {
        super(jpaQueryFactory, commentRepository);
        this.redisPostService = redisPostService;
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