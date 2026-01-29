package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.scheduler.PostCacheRefreshScheduler;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
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
 * <p>Hash 캐시에서 직접 조회하며, 캐시 미스 시 빈 페이지를 반환합니다.</p>
 * <p>캐시 갱신은 {@link PostCacheRefreshScheduler}가 담당합니다.</p>
 * <p>featured_post 테이블에서 DB 폴백합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class FeaturedPostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final FeaturedPostRepository featuredPostRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;

    /**
     * 주간 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        return getFeaturedCachedPosts(PostCacheFlag.WEEKLY, pageable);
    }

    /**
     * 전설 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        return getFeaturedCachedPosts(PostCacheFlag.LEGEND, pageable);
    }

    /**
     * 공지사항 목록 조회
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        return getFeaturedCachedPosts(PostCacheFlag.NOTICE, pageable);
    }

    /**
     * <h3>주간/레전드/공지 캐시 조회</h3>
     * <p>Hash 캐시에서 직접 조회합니다.</p>
     * <p>캐시 미스 시 빈 페이지를 반환합니다. (스케줄러가 갱신 예정)</p>
     *
     * @param type         캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @param pageable     페이징 정보
     * @return 페이징된 게시글 목록
     */
    private Page<PostSimpleDetail> getFeaturedCachedPosts(PostCacheFlag type, Pageable pageable) {
        try {
            List<PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPostsList(type);

            if (cachedPosts.isEmpty()) {
                CacheMetricsLogger.miss(log, type.name().toLowerCase(), "simple", "empty");
                return new PageImpl<>(List.of(), pageable, 0);
            }

            CacheMetricsLogger.hit(log, type.name().toLowerCase(), "simple", cachedPosts.size());
            return paginate(cachedPosts, pageable);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", type, e.getMessage());
            return findFeaturedPostsByType(type, pageable);
        }
    }

    /**
     * <h3>featured_post 테이블에서 페이징 조회 (DB 폴백용)</h3>
     * <p>featured_post에서 postId 목록을 조회한 후 페이징 조회합니다.</p>
     *
     * @param type     특집 유형 (WEEKLY, LEGEND, NOTICE)
     * @param pageable 페이징 정보
     * @return PostSimpleDetail 페이지
     */
    private Page<PostSimpleDetail> findFeaturedPostsByType(PostCacheFlag type, Pageable pageable) {
        List<Long> postIds = featuredPostRepository.findPostIdsByType(type);
        return postQueryRepository.findPostSimpleDetailsByIds(postIds, pageable);
    }


    private Page<PostSimpleDetail> paginate(List<PostSimpleDetail> posts, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), posts.size());

        if (start >= posts.size()) {
            return new PageImpl<>(List.of(), pageable, posts.size());
        }

        return new PageImpl<>(posts.subList(start, end), pageable, posts.size());
    }
}
