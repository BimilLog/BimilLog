package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Recover;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>첫 페이지 캐시 갱신 실행기</h2>
 * <p>DB에서 첫 페이지 게시글을 조회하여 Redis 캐시를 갱신합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheRefreshExecutor {
    private final PostQueryRepository postQueryRepository;
    private final RedisFirstPagePostAdapter redisFirstPagePostAdapter;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;

    /**
     * 첫 페이지 캐시 크기 (20개)
     */
    public static final int FIRST_PAGE_SIZE = 20;

    /**
     * <h3>비동기 캐시 갱신 (분산 락)</h3>
     * <p>캐시 미스 또는 수정/삭제 시 호출됩니다.</p>
     * <p>분산 락을 획득하여 하나의 인스턴스만 갱신하도록 보장합니다.</p>
     * <p>갱신 실패 시에도 캐시가 이미 무효화되어 DB 폴백이 동작합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncRefreshWithLock() {
        refreshWithLock();
    }

    /**
     * <h3>비동기 게시글 추가</h3>
     * <p>새 게시글을 첫 페이지 캐시에 비동기로 추가합니다.</p>
     * <p>실패 시 캐시 무효화되어 다음 조회 시 DB 폴백이 동작합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncAddNewPost(PostSimpleDetail post) {
        redisFirstPagePostAdapter.addNewPost(post);
    }

    /**
     * <h3>비동기 게시글 수정 반영</h3>
     * <p>인기글 Hash 캐시 무효화 + 첫 페이지 LSET 교체를 비동기로 처리합니다.</p>
     * <p>첫 페이지에 없는 글이면 첫 페이지 캐시는 무시됩니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncUpdatePost(Long postId, PostSimpleDetail updatedPost) {
        redisSimplePostAdapter.removePostFromCache(postId);
        redisFirstPagePostAdapter.updatePost(postId, updatedPost);
    }

    /**
     * <h3>비동기 게시글 삭제 반영</h3>
     * <p>실시간 ZSet + 인기글 Hash + 첫 페이지 List 캐시를 비동기로 정리합니다.</p>
     * <p>첫 페이지에 없는 글이면 첫 페이지 캐시는 무시됩니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncDeletePost(Long postId) {
        redisRealTimePostAdapter.removePostIdFromRealtimeScore(postId);
        redisSimplePostAdapter.removePostFromCache(postId);
        redisFirstPagePostAdapter.deletePost(postId);
    }

    /**
     * <h3>첫 페이지 캐시 갱신 (분산 락)</h3>
     * <p>DB에서 최신 20개 게시글을 조회하여 Redis 캐시를 갱신합니다.</p>
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
     * <p>재시도 후에도 실패 시 로그를 남기고 종료합니다.</p>
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
