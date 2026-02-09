package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Recover;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>캐시 갱신 실행기</h2>
 * <p>글 수정/삭제 시 글 단위 Hash, SET 인덱스, 첫 페이지 캐시를 갱신합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheRefreshExecutor {
    private final PostQueryRepository postQueryRepository;
    private final RedisFirstPagePostAdapter redisFirstPagePostAdapter;
    private final RedisPostHashAdapter redisPostHashAdapter;
    private final RedisPostIndexAdapter redisPostIndexAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;

    /**
     * 첫 페이지 캐시 크기 (20개)
     */
    public static final int FIRST_PAGE_SIZE = 20;

    /**
     * <h3>비동기 캐시 갱신 (분산 락)</h3>
     * <p>캐시 미스 또는 수정/삭제 시 호출됩니다.</p>
     * <p>분산 락을 획득하여 하나의 인스턴스만 갱신하도록 보장합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncRefreshWithLock() {
        refreshWithLock();
    }

    /**
     * <h3>비동기 게시글 추가</h3>
     * <p>새 게시글을 첫 페이지 캐시에 비동기로 추가합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncAddNewPost(PostSimpleDetail post) {
        redisFirstPagePostAdapter.addNewPost(post);
    }

    /**
     * <h3>비동기 게시글 수정 반영</h3>
     * <p>글 단위 Hash의 제목을 업데이트하고, 첫 페이지 캐시를 갱신합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncUpdatePost(Long postId, PostSimpleDetail updatedPost) {
        redisPostHashAdapter.updateTitle(postId, updatedPost.getTitle());
        redisFirstPagePostAdapter.updatePost(postId, updatedPost);
    }

    /**
     * <h3>비동기 게시글 삭제 반영</h3>
     * <p>실시간 ZSET + 글 단위 Hash + SET 인덱스(weekly/legend/notice) + 첫 페이지 캐시를 정리합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncDeletePost(Long postId) {
        redisRealTimePostAdapter.removePostIdFromRealtimeScore(postId);
        redisPostHashAdapter.deletePostHash(postId);

        // 모든 SET 인덱스에서 제거
        redisPostIndexAdapter.removeFromIndex(RedisKey.POST_WEEKLY_IDS_KEY, postId);
        redisPostIndexAdapter.removeFromIndex(RedisKey.POST_LEGEND_IDS_KEY, postId);
        redisPostIndexAdapter.removeFromIndex(RedisKey.POST_NOTICE_IDS_KEY, postId);

        Long lastPostId = redisFirstPagePostAdapter.deletePost(postId);
        if (lastPostId != null) {
            List<PostSimpleDetail> nextPosts = postQueryRepository.findBoardPostsByCursor(lastPostId, 1);
            if (!nextPosts.isEmpty()) {
                redisFirstPagePostAdapter.appendPost(nextPosts.getFirst());
            }
        }
    }

    /**
     * <h3>첫 페이지 캐시 갱신 (분산 락)</h3>
     */
    public void refreshFirstPage() {
        List<PostSimpleDetail> posts = postQueryRepository.findBoardPostsByCursor(null, FIRST_PAGE_SIZE);
        if (posts.size() > FIRST_PAGE_SIZE) {
            posts = posts.subList(0, FIRST_PAGE_SIZE);
        }
        redisFirstPagePostAdapter.refreshCache(posts);
        log.debug("[FIRST_PAGE_CACHE] 캐시 갱신 완료: {}개", posts.size());
    }

    /**
     * <h3>캐시 갱신 최종 실패 복구</h3>
     */
    @Recover
    public void recoverRefreshFirstPage(Exception e) {
        log.error("[FIRST_PAGE_CACHE] 캐시 갱신 최종 실패: {}", e.getMessage(), e);
    }

    private void refreshWithLock() {
        if (!redisFirstPagePostAdapter.tryAcquireRefreshLock()) {
            log.debug("[FIRST_PAGE_CACHE] 다른 인스턴스가 갱신 중이므로 스킵");
            return;
        }

        try {
            refreshFirstPage();
        } catch (Exception e) {
            log.warn("[FIRST_PAGE_CACHE] 비동기 캐시 갱신 실패: {}", e.getMessage());
        } finally {
            redisFirstPagePostAdapter.releaseRefreshLock();
        }
    }
}
