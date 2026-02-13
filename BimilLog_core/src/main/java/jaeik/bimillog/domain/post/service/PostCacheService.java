package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostCounterAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>PostCacheService</h2>
 * <p>주간/레전드/공지 인기글 및 첫 페이지 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>모든 캐시가 JSON LIST 단일 구조로 통일되어 getAll(key) → 바로 반환합니다.</p>
 * <p>Redis 장애 시 DB 폴백합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class PostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;
    private final RedisPostCounterAdapter redisPostCounterAdapter;
    private final PostUtil postUtil;

    /**
     * 주간 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        try {
            List<PostSimpleDetail> posts = redisPostJsonListAdapter.getAll(RedisKey.POST_WEEKLY_JSON_KEY);
            if (!posts.isEmpty()) {
                redisPostCounterAdapter.mergeCounters(posts);
                return postUtil.paginate(posts, pageable);
            }
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", RedisKey.POST_WEEKLY_JSON_KEY, e.getMessage());
        }
        return postQueryRepository.findWeeklyPostsFallback(pageable);
    }

    /**
     * 전설 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        try {
            List<PostSimpleDetail> posts = redisPostJsonListAdapter.getAll(RedisKey.POST_LEGEND_JSON_KEY);
            if (!posts.isEmpty()) {
                redisPostCounterAdapter.mergeCounters(posts);
                return postUtil.paginate(posts, pageable);
            }
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", RedisKey.POST_LEGEND_JSON_KEY, e.getMessage());
        }
        return postQueryRepository.findLegendPostsFallback(pageable);
    }

    /**
     * 공지사항 목록 조회
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        try {
            List<PostSimpleDetail> posts = redisPostJsonListAdapter.getAll(RedisKey.POST_NOTICE_JSON_KEY);
            if (!posts.isEmpty()) {
                redisPostCounterAdapter.mergeCounters(posts);
                return postUtil.paginate(posts, pageable);
            }
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", RedisKey.POST_NOTICE_JSON_KEY, e.getMessage());
        }
        return postQueryRepository.findNoticePostsFallback(pageable);
    }

    /**
     * 첫 페이지 캐시 조회 — JSON LIST 방식 (캐시 미스/장애 시 DB 폴백)
     */
    public List<PostSimpleDetail> getFirstPagePosts() {
        try {
            List<PostSimpleDetail> posts = redisPostJsonListAdapter.getAll(RedisKey.FIRST_PAGE_JSON_KEY);
            if (!posts.isEmpty()) {
                redisPostCounterAdapter.mergeCounters(posts);
                return posts;
            }
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", RedisKey.FIRST_PAGE_JSON_KEY, e.getMessage());
        }
        return postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE);
    }
}
