package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.hotkey.HotKeyMonitor;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>FeaturedPostCacheService</h2>
 * <p>주간/레전드/공지 인기글 목록 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>Hash 캐시에서 직접 조회하며, 캐시 미스 시 SET NX 락 기반으로 비동기 캐시 갱신을 트리거합니다.</p>
 * <p>featured_post 테이블에서 DB 폴백합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class FeaturedPostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final FeaturedPostRepository featuredPostRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final PostCacheRefresh postCacheRefresh;
    private final DbFallbackGateway dbFallbackGateway;

    /**
     * 주간 인기글 목록 조회
     */
    @HotKeyMonitor(PostCacheFlag.WEEKLY)
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        return getFeaturedCachedPosts(PostCacheFlag.WEEKLY, FallbackType.WEEKLY, pageable);
    }

    /**
     * 전설 인기글 목록 조회
     */
    @HotKeyMonitor(PostCacheFlag.LEGEND)
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        return getFeaturedCachedPosts(PostCacheFlag.LEGEND, FallbackType.LEGEND, pageable);
    }

    /**
     * 공지사항 목록 조회
     */
    @HotKeyMonitor(PostCacheFlag.NOTICE)
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        return getFeaturedCachedPosts(PostCacheFlag.NOTICE, FallbackType.NOTICE, pageable);
    }

    // ========== 캐시 조회 메서드 ==========

    /**
     * <h3>주간/레전드/공지 캐시 조회</h3>
     * <p>Hash 캐시에서 직접 조회합니다.</p>
     * <p>캐시 미스 시 SET NX 락 기반 비동기 갱신 트리거 + DB 폴백합니다.</p>
     *
     * @param type         캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @param fallbackType 폴백 유형
     * @param pageable     페이징 정보
     * @return 페이징된 게시글 목록
     */
    private Page<PostSimpleDetail> getFeaturedCachedPosts(PostCacheFlag type, FallbackType fallbackType,
                                                         Pageable pageable) {
        try {
            List<PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPostsList(type);

            if (cachedPosts.isEmpty()) {
                triggerAsyncFeaturedCacheRefresh(type);
                log.info("[CACHE_MISS] {} 캐시 미스 - DB 폴백, 비동기 갱신 트리거", type);
                return dbFallbackGateway.execute(fallbackType, pageable,
                        () -> findFeaturedPostsByType(type, pageable));
            }

            return paginate(cachedPosts, pageable);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", type, e.getMessage());
            return dbFallbackGateway.execute(fallbackType, pageable,
                    () -> findFeaturedPostsByType(type, pageable));
        }
    }

    // ========== DB 조회 메서드 ==========

    /**
     * <h3>featured_post 테이블에서 조회 (DB 폴백용)</h3>
     * <p>featured_post 테이블에서 postId 목록을 조회 후 PostSimpleDetail로 변환합니다.</p>
     *
     * @param type     특집 유형 (WEEKLY, LEGEND, NOTICE)
     * @param pageable 페이징 정보
     * @return PostSimpleDetail 페이지
     */
    private Page<PostSimpleDetail> findFeaturedPostsByType(PostCacheFlag type, Pageable pageable) {
        List<Long> allPostIds = featuredPostRepository.findPostIdsByType(type);

        if (allPostIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allPostIds.size());

        if (start >= allPostIds.size()) {
            return new PageImpl<>(List.of(), pageable, allPostIds.size());
        }

        List<Long> pagedPostIds = allPostIds.subList(start, end);
        List<PostSimpleDetail> posts = postQueryRepository.findPostSimpleDetailsByIds(pagedPostIds);

        return new PageImpl<>(posts, pageable, allPostIds.size());
    }

    // ========== 유틸리티 메서드 ==========

    private Page<PostSimpleDetail> paginate(List<PostSimpleDetail> posts, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), posts.size());

        if (start >= posts.size()) {
            return new PageImpl<>(List.of(), pageable, posts.size());
        }

        return new PageImpl<>(posts.subList(start, end), pageable, posts.size());
    }

    // ========== 캐시 갱신 트리거 ==========

    /**
     * <h3>주간/레전드/공지 캐시 비동기 갱신 트리거 (캐시 미스용)</h3>
     * <p>캐시 미스 시 비동기로 SET NX 락 획득 -> featured_post -> DB -> 캐시 갱신을 트리거합니다.</p>
     *
     * @param type 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     */
    private void triggerAsyncFeaturedCacheRefresh(PostCacheFlag type) {
        postCacheRefresh.asyncRefreshFeaturedWithLock(type);
    }
}
