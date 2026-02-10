package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.scheduler.FeaturedPostScheduler;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <h2>FeaturedPostCacheService</h2>
 * <p>주간/레전드/공지 인기글 목록 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>SET 인덱스에서 postId를 조회하고, 글 단위 Hash(post:simple:{postId})에서 데이터를 가져옵니다.</p>
 * <p>캐시 갱신은 {@link FeaturedPostScheduler}(주간/레전드)와 관리자 토글/글 수정(공지)이 담당합니다.</p>
 * <p>Redis 장애 시 Post 테이블에서 boolean 플래그 기반으로 DB 폴백합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class FeaturedPostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final RedisPostHashAdapter redisPostHashAdapter;
    private final RedisPostIndexAdapter redisPostIndexAdapter;
    private final PostUtil postUtil;

    /**
     * 주간 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        return getFeaturedCachedPosts(RedisKey.POST_WEEKLY_IDS_KEY, postQueryRepository::findWeeklyPostsFallback, pageable);
    }

    /**
     * 전설 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        return getFeaturedCachedPosts(RedisKey.POST_LEGEND_IDS_KEY, postQueryRepository::findLegendPostsFallback, pageable);
    }

    /**
     * 공지사항 목록 조회
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        return getFeaturedCachedPosts(RedisKey.POST_NOTICE_IDS_KEY, postQueryRepository::findNoticePostsFallback, pageable);
    }

    /**
     * <h3>주간/레전드/공지 캐시 조회</h3>
     * <p>SET 인덱스에서 postId를 조회하고, 글 단위 Hash에서 pipeline으로 데이터를 가져옵니다.</p>
     * <p>ID 역순 정렬 후 반환합니다.</p>
     * <p>Redis 장애 시 Post 테이블에서 boolean 플래그 기반으로 DB 폴백합니다.</p>
     *
     * @param indexKey         Redis SET 인덱스 키
     * @param dbFallback       DB 폴백 함수
     * @param pageable         페이징 정보
     * @return 페이징된 게시글 목록
     */
    private Page<PostSimpleDetail> getFeaturedCachedPosts(String indexKey, Function<Pageable, Page<PostSimpleDetail>> dbFallback, Pageable pageable) {
        try {
            List<Long> orderedIds = redisPostIndexAdapter.getIndexList(indexKey);

            if (orderedIds.isEmpty()) {
                CacheMetricsLogger.miss(log, indexKey, "index", "empty");
                return new PageImpl<>(List.of(), pageable, 0);
            }

            List<PostSimpleDetail> cachedPosts = redisPostHashAdapter.getPostHashes(orderedIds);

            // 누락된 글이 있으면 DB에서 조회하여 Hash 생성
            if (cachedPosts.size() < orderedIds.size()) {
                List<Long> cachedIds = cachedPosts.stream().map(PostSimpleDetail::getId).toList();
                List<Long> missingIds = orderedIds.stream()
                        .filter(id -> !cachedIds.contains(id))
                        .toList();

                if (!missingIds.isEmpty()) {
                    List<PostSimpleDetail> dbPosts = missingIds.stream()
                            .map(id -> postQueryRepository.findPostDetail(id, null).orElse(null))
                            .filter(Objects::nonNull)
                            .map(PostDetail::toSimpleDetail)
                            .toList();

                    dbPosts.forEach(redisPostHashAdapter::createPostHash);

                    cachedPosts = new ArrayList<>(cachedPosts);
                    cachedPosts.addAll(dbPosts);
                }
            }

            if (cachedPosts.isEmpty()) {
                CacheMetricsLogger.miss(log, indexKey, "simple", "empty");
                return new PageImpl<>(List.of(), pageable, 0);
            }

            // List 인덱스 순서(주간/레전드: 인기순, 공지: 최신순)대로 정렬
            Map<Long, PostSimpleDetail> postMap = cachedPosts.stream()
                    .collect(Collectors.toMap(PostSimpleDetail::getId, p -> p, (a, b) -> a));
            List<PostSimpleDetail> orderedPosts = orderedIds.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            CacheMetricsLogger.hit(log, indexKey, "simple", orderedPosts.size());
            return postUtil.paginate(orderedPosts, pageable);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", indexKey, e.getMessage());
            return dbFallback.apply(pageable);
        }
    }
}
