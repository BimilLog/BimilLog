package jaeik.growfarm.repository.post.delete;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostCacheFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.service.redis.RedisPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 삭제 관리 구현체</h2>
 * <p>
 * 게시글 삭제를 담당한다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PostDeleteRepositoryImpl implements PostDeleteRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final RedisPostService redisPostService;

    /**
     * <h3>게시글 삭제 및 Redis 캐시 동기화</h3>
     * <p>
     * 게시글을 삭제하고, 삭제된 게시글이 캐시된 글일 경우 관련 Redis 캐시를 즉시 삭제한다.
     * </p>
     *
     * @param postId 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deletePostWithCacheSync(Long postId) {
        QPost post = QPost.post;

        try {
            // 1. 게시글 엔티티 조회
            Post postToDelete = jpaQueryFactory
                    .selectFrom(post)
                    .where(post.id.eq(postId))
                    .fetchOne();

            if (postToDelete == null) {
                throw new CustomException(ErrorCode.POST_NOT_FOUND);
            }

            PostCacheFlag postCacheFlag = postToDelete.getPostCacheFlag();
            boolean isNotice = postToDelete.isNotice();

            // 2. 게시글 삭제
            jpaQueryFactory
                    .delete(post)
                    .where(post.id.eq(postId))
                    .execute();
            
            // 3. 관련 캐시 모두 삭제
            // 상세 정보 캐시 삭제
            redisPostService.deleteFullPostCache(postId);

            // 인기글 목록 캐시 삭제
            if (postCacheFlag != null) {
                deleteRelatedRedisCache(postCacheFlag);
            }
            // 공지사항 목록 캐시 삭제
            if (isNotice) {
                redisPostService.deleteNoticePostsCache();
                log.info("공지사항 Redis 캐시 삭제 완료");
            }

        } catch (Exception e) {
            log.error("게시글 삭제 중 오류 발생. postId: {}", postId, e);
            throw new CustomException(ErrorCode.POST_DELETE_FAILED, e);
        }
    }

    /**
     * <h3>관련 Redis 캐시 삭제</h3>
     *
     * <p>
     * 삭제된 게시글이 캐시된 글 경우, 해당 글 목록의 Redis 캐시를 삭제한다.
     * </p>
     *
     * @param postCacheFlag 삭제된 게시글의 인기글 플래그
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteRelatedRedisCache(PostCacheFlag postCacheFlag) {
        try {
            switch (postCacheFlag) {
                case REALTIME -> {
                    redisPostService.deletePopularPostsCache(PostCacheFlag.REALTIME);
                    log.info("실시간 인기글 Redis 캐시 삭제 완료");
                }
                case WEEKLY -> {
                    redisPostService.deletePopularPostsCache(PostCacheFlag.WEEKLY);
                    log.info("주간 인기글 Redis 캐시 삭제 완료");
                }
                case LEGEND -> {
                    redisPostService.deletePopularPostsCache(PostCacheFlag.LEGEND);
                    log.info("레전드 게시글 Redis 캐시 삭제 완료");
                }
                default -> log.debug("PopularFlag가 없는 게시글이므로 캐시 삭제 불필요");
            }
        } catch (Exception e) {
            log.error("Redis 캐시 삭제 중 오류 발생. popularFlag: {}", postCacheFlag, e);
            throw new CustomException(ErrorCode.REDIS_DELETE_ERROR, e);
        }
    }
}

