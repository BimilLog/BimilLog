package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.scheduler.PostCacheScheduler;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>PostCacheService</h2>
 * <p>주간/레전드/공지 인기글 및 첫 페이지 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>SET/List 인덱스에서 postId를 조회하고, 글 단위 Hash(post:simple:{postId})에서 데이터를 가져옵니다.</p>
 * <p>캐시 갱신은 {@link PostCacheScheduler}(주간/레전드)와 관리자 토글/글 수정(공지)이 담당합니다.</p>
 * <p>Redis 장애 시 Post 테이블에서 boolean 플래그 기반으로 DB 폴백합니다.</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class PostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final RedisPostHashAdapter redisPostHashAdapter;
    private final RedisPostIndexAdapter redisPostIndexAdapter;
    private final PostUtil postUtil;

    /**
     * 주간 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        try {
            return postUtil.paginate(getCachedPosts(RedisKey.POST_WEEKLY_IDS_KEY), pageable);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", RedisKey.POST_WEEKLY_IDS_KEY, e.getMessage());
            return postQueryRepository.findWeeklyPostsFallback(pageable);
        }
    }

    /**
     * 전설 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        try {
            return postUtil.paginate(getCachedPosts(RedisKey.POST_LEGEND_IDS_KEY), pageable);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", RedisKey.POST_LEGEND_IDS_KEY, e.getMessage());
            return postQueryRepository.findLegendPostsFallback(pageable);
        }
    }

    /**
     * 공지사항 목록 조회
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        try {
            return postUtil.paginate(getCachedPosts(RedisKey.POST_NOTICE_IDS_KEY), pageable);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", RedisKey.POST_NOTICE_IDS_KEY, e.getMessage());
            return postQueryRepository.findNoticePostsFallback(pageable);
        }
    }

    /**
     * 첫 페이지 캐시 조회 (캐시 미스/장애 시 DB 폴백)
     */
    public List<PostSimpleDetail> getFirstPagePosts() {
        try {
            return getCachedPosts(RedisKey.FIRST_PAGE_LIST_KEY);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", RedisKey.FIRST_PAGE_LIST_KEY, e.getMessage());
            return postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE);
        }
    }

    /**
     * List 인덱스 → Hash 조회 → 미스 복구 → 순서 보장 공통 파이프라인
     */
    private List<PostSimpleDetail> getCachedPosts(String indexKey) {
        List<Long> orderedIds = redisPostIndexAdapter.getIndexList(indexKey);
        if (orderedIds.isEmpty()) {
            return List.of();
        }

        List<PostSimpleDetail> cachedPosts = redisPostHashAdapter.getPostHashes(orderedIds);
        if (cachedPosts.size() < orderedIds.size()) {
            cachedPosts = postUtil.recoverMissingHashes(orderedIds, cachedPosts);
        }
        return postUtil.orderByIds(orderedIds, cachedPosts);
    }
}
