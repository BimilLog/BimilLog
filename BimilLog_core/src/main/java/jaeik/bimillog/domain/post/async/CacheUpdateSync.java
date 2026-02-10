package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>캐시 갱신 실행기</h2>
 * <p>글 작성/수정/삭제 시 글 단위 Hash, List 인덱스, 첫 페이지 캐시를 갱신합니다.</p>
 * <p>첫 페이지 List에는 ID만 저장하고, 데이터는 글 단위 Hash에서 관리합니다.</p>
 *
 * @author Jaeik
 * @version 3.2.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheUpdateSync {
    private final PostQueryRepository postQueryRepository;
    private final RedisPostHashAdapter redisPostHashAdapter;
    private final RedisPostIndexAdapter redisPostIndexAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;

    /**
     * <h3>새 글 작성 캐시 반영</h3>
     * <p>Hash를 먼저 생성하고, 첫 페이지 List에 ID를 추가합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncAddNewPost(PostSimpleDetail post) {
        redisPostHashAdapter.createPostHash(post);
        redisPostIndexAdapter.addToIndexWithTrim(RedisKey.FIRST_PAGE_LIST_KEY, post.getId(), RedisKey.FIRST_PAGE_SIZE);
    }

    /**
     * <h3>글 수정 캐시 반영</h3>
     * <p>글 단위 Hash의 제목만 업데이트합니다.</p>
     * <p>List에는 ID만 저장하므로 List 갱신은 불필요합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncUpdatePost(Long postId, PostSimpleDetail updatedPost) {
        redisPostHashAdapter.updateTitle(postId, updatedPost.getTitle());
    }

    /**
     * <h3>글 삭제 삭제 반영</h3>
     * <p>실시간 ZSET + 글 단위 Hash + List 인덱스(weekly/legend/notice) + 첫 페이지 캐시를 정리합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncDeletePost(Long postId) {
        redisRealTimePostAdapter.removePostIdFromRealtimeScore(postId);

        // 모든 List 인덱스에서 제거
        redisPostIndexAdapter.removeFromIndex(RedisKey.POST_WEEKLY_IDS_KEY, postId);
        redisPostIndexAdapter.removeFromIndex(RedisKey.POST_LEGEND_IDS_KEY, postId);
        redisPostIndexAdapter.removeFromIndex(RedisKey.POST_NOTICE_IDS_KEY, postId);
        redisPostHashAdapter.deletePostHash(postId);

        Long lastPostId = redisPostIndexAdapter.removeFromIndexAndGetLast(RedisKey.FIRST_PAGE_LIST_KEY, postId);
        if (lastPostId != null) {
            List<PostSimpleDetail> nextPosts = postQueryRepository.findBoardPostsByCursor(lastPostId, 1);
            if (!nextPosts.isEmpty()) {
                PostSimpleDetail nextPost = nextPosts.getFirst();
                redisPostHashAdapter.createPostHash(nextPost);
                redisPostIndexAdapter.appendToIndex(RedisKey.FIRST_PAGE_LIST_KEY, nextPost.getId(), RedisKey.FIRST_PAGE_SIZE);
            }
        }
    }
}
