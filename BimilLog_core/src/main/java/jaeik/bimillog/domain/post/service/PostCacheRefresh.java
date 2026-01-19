package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisDetailPostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <h2>글 캐시 갱신 클래스</h2>
 * <p>인기글(공지/실시간/주간/레전드) 목록 캐시의 비동기 갱신을 담당합니다.</p>
 * <p>PER(Probabilistic Early Refresh) 기반으로 목록 API에서 호출됩니다.</p>
 * <p>목록 캐시만 갱신하며, 상세 캐시는 PostQueryService에서 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Log(logResult = false, message = "캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefresh {
    private final PostQueryRepository postQueryRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisDetailPostAdapter redisDetailPostAdapter;

    /**
     * <h3>특정 게시글들의 목록 캐시 비동기 갱신 (PER 기반)</h3>
     * <p>개별 게시글 목록 캐시가 만료 임박 시 호출됩니다.</p>
     * <p>목록 캐시(PostSimpleDetail)만 갱신합니다.</p>
     * <p>백그라운드에서 실행되므로 사용자 요청은 블로킹되지 않습니다.</p>
     *
     * @param type    캐시 유형
     * @param postIds 갱신할 게시글 ID 목록
     */
    @Async("cacheRefreshExecutor")
    public void asyncRefreshPosts(PostCacheFlag type, List<Long> postIds) {
        try {
            log.info("[PER_REFRESH] 시작 - type={}, count={}, thread={}",
                    type, postIds.size(), Thread.currentThread().getName());

            // DB에서 PostDetail 조회 후 PostSimpleDetail 변환
            List<PostSimpleDetail> refreshed = postIds.stream()
                    .map(postId -> postQueryRepository.findPostDetail(postId, null).orElse(null))
                    .filter(Objects::nonNull)
                    .map(PostDetail::toSimpleDetail)
                    .toList();

            if (refreshed.isEmpty()) {
                log.warn("[PER_REFRESH] 실패 - type={}, 이유=DB 조회 결과 없음", type);
                return;
            }

            // 목록 캐시만 저장 (post:{type}:simple:{postId})
            redisSimplePostAdapter.cachePosts(type, refreshed);

            log.info("[PER_REFRESH] 완료 - type={}, count={}", type, refreshed.size());

        } catch (Exception e) {
            log.error("[PER_REFRESH] 에러 - type={}", type, e);
        }
    }

    /**
     * <h3>상세 캐시 비동기 갱신 (PER 기반)</h3>
     * <p>상세 캐시 TTL이 임박했을 때 백그라운드에서 갱신합니다.</p>
     * <p>인기글의 상세 조회 시 PER 조건 만족 시 호출됩니다.</p>
     *
     * @param postId 갱신할 게시글 ID
     */
    @Async("cacheRefreshExecutor")
    public void asyncRefreshDetailPost(Long postId) {
        try {
            log.info("[PER_DETAIL_REFRESH] 시작 - postId={}, thread={}",
                    postId, Thread.currentThread().getName());

            PostDetail postDetail = postQueryRepository.findPostDetail(postId, null).orElse(null);
            if (postDetail == null) {
                log.warn("[PER_DETAIL_REFRESH] 실패 - postId={}, 이유=DB 조회 결과 없음", postId);
                return;
            }

            redisDetailPostAdapter.saveCachePost(postDetail);
            log.info("[PER_DETAIL_REFRESH] 완료 - postId={}", postId);

        } catch (Exception e) {
            log.error("[PER_DETAIL_REFRESH] 에러 - postId={}", postId, e);
        }
    }
}
