package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListDeleteAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>캐시 갱신 실행기</h2>
 * <p>글 작성/수정/삭제 시 모든 JSON LIST 캐시를 갱신합니다.</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheUpdateSync {
    private final PostQueryRepository postQueryRepository;
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;
    private final RedisPostRealTimeAdapter redisPostRealTimeAdapter;
    private final RedisPostListDeleteAdapter redisPostListDeleteAdapter;

    /**
     * <h3>새 글 작성 캐시 반영</h3>
     * <p>첫 페이지 JSON LIST에 새 글을 LPUSH + LTRIM으로 추가합니다.</p>
     */
    @Async("cacheRefreshExecutor")
    public void asyncAddNewPost(PostSimpleDetail post) {
        redisPostListUpdateAdapter.addPostToList(
                RedisKey.FIRST_PAGE_JSON_KEY, post, RedisKey.FIRST_PAGE_SIZE + 1);
    }

    /**
     * <h3>글 수정 캐시 반영</h3>
     * <p>모든 JSON LIST에서 해당 글의 제목을 업데이트합니다.</p>
     */
    @Async("cacheRefreshExecutor")
    public void asyncUpdatePost(Long postId, PostSimpleDetail updatedPost) {
        redisPostListUpdateAdapter.updateTitle(postId, updatedPost.getTitle());
    }

    /**
     * <h3>글 삭제 캐시 반영</h3>
     * <p>실시간 ZSet + 모든 JSON LIST를 정리합니다.</p>
     * <p>첫 페이지만 보충합니다. (주간/레전드/공지/실시간은 스케줄러가 재구축)</p>
     */
    @Async("cacheRefreshExecutor")
    public void asyncDeletePost(Long postId) {
        redisPostRealTimeAdapter.removePostIdFromRealtimeScore(postId);
        redisPostListDeleteAdapter.removePostFromCacheLists(postId);

        Long lastPostId = redisPostListDeleteAdapter.removePostAndGetLastId(RedisKey.FIRST_PAGE_JSON_KEY, postId);
        if (lastPostId != null) {
            List<PostSimpleDetail> nextPosts = postQueryRepository.findBoardPostsByCursor(lastPostId, 1);
            if (!nextPosts.isEmpty()) {
                redisPostListDeleteAdapter.appendPost(
                        RedisKey.FIRST_PAGE_JSON_KEY, nextPosts.getFirst(), RedisKey.FIRST_PAGE_SIZE + 1);
            }
        }
    }
}
