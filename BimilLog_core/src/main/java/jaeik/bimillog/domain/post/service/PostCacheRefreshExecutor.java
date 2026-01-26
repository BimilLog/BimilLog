package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>캐시 갱신 실행기</h2>
 * <p>순수 Redis 캐시 저장만 담당합니다.</p>
 * <p>Redis 연결 실패 시 {@code @Retryable}로 최대 3회 재시도합니다.</p>
 * <p>DB 조회는 {@link PostCacheRefresh}에서 수행하고, 조회된 데이터를 전달받아 캐시에 저장합니다.</p>
 *
 * @author Jaeik
 * @version 2.9.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefreshExecutor {
    private final RedisSimplePostAdapter redisSimplePostAdapter;

    /**
     * <h3>게시글 목록 캐시 저장</h3>
     * <p>전달받은 PostSimpleDetail 목록을 Redis Hash 캐시에 저장합니다.</p>
     * <p>Redis 연결 실패 시 최대 3회 재시도합니다.</p>
     *
     * @param type  캐시 유형
     * @param posts 캐시에 저장할 게시글 목록
     */
    @Retryable(
            retryFor = RedisConnectionFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 1)
    )
    public void cachePosts(PostCacheFlag type, List<PostSimpleDetail> posts) {
        if (posts.isEmpty()) {
            return;
        }
        redisSimplePostAdapter.cachePosts(type, posts);
    }

    @Recover
    public void recoverCachePosts(RedisConnectionFailureException e,
                                   PostCacheFlag type, List<PostSimpleDetail> posts) {
        log.debug("목록 캐시 저장 최종 실패: type={}", type);
    }
}
