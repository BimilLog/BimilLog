package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * <h2>PostCacheService</h2>
 * <p>인기글(공지/실시간/주간/레전드) 목록 캐시 조회 및 동기화 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>Hash 기반 캐시와 개수 비교를 통한 캐시 미스 감지, Redis TTL 기반 PER을 지원합니다.</p>
 * <p>목록 캐시만 관리하며, 상세 캐시는 PostQueryService에서 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisTier2PostAdapter redisTier2PostAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final PostCacheRefresh postCacheRefresh;
    private final DbFallbackGateway dbFallbackGateway;
    private final RealtimeScoreFallbackStore fallbackStore;

    /**
     * <h3>실시간 인기 게시글 조회</h3>
     * <p>Redis Sorted Set에서 postId 목록을 조회하고 Hash 캐시에서 상세 정보를 획득합니다.</p>
     * <p>폴백 순서: Redis → ConcurrentHashMap → DB</p>
     *
     * @param pageable 페이지 정보
     * @return Redis에서 조회된 실시간 인기 게시글 페이지
     */
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        try {
            // 1. 총 개수 조회
            long totalCount = redisRealTimePostAdapter.getRealtimePopularPostCount();

            // Redis가 비어있고 폴백 저장소에 데이터가 있으면 폴백 저장소 사용
            if (totalCount == 0 && fallbackStore.hasData()) {
                return getRealtimePostsFromFallbackStore(pageable);
            }

            if (totalCount == 0) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            // 2. score:realtime에서 페이징된 postId 조회 (순서 포함)
            List<Long> postIds = redisRealTimePostAdapter
                    .getRealtimePopularPostIds(pageable.getOffset(), pageable.getPageSize());

            if (postIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, totalCount);
            }

            // 3. Hash 캐시 조회 + 개수 비교 + PER 처리
            List<PostSimpleDetail> resultPosts = fetchPostsWithCache(PostCacheFlag.REALTIME, postIds);

            return new PageImpl<>(resultPosts, pageable, totalCount);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] 실시간 인기글 Redis 장애: {}", e.getMessage());

            // 폴백 순서: ConcurrentHashMap → DB
            if (fallbackStore.hasData()) {
                return getRealtimePostsFromFallbackStore(pageable);
            }

            // DB 폴백
            return dbFallbackGateway.execute(FallbackType.REALTIME, pageable,
                    () -> postQueryRepository.findRecentPopularPosts(pageable));
        }
    }

    /**
     * <h3>폴백 저장소에서 실시간 인기글 조회</h3>
     * <p>ConcurrentHashMap에 저장된 점수를 기반으로 게시글을 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 폴백 저장소 기반 실시간 인기 게시글 페이지
     */
    private Page<PostSimpleDetail> getRealtimePostsFromFallbackStore(Pageable pageable) {
        int totalSize = fallbackStore.size();

        // 페이징된 postId 목록 조회
        List<Long> topPostIds = fallbackStore.getTopPostIds(
                (int) pageable.getOffset() + pageable.getPageSize());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), topPostIds.size());

        if (start >= topPostIds.size()) {
            return new PageImpl<>(List.of(), pageable, totalSize);
        }

        List<Long> pagedPostIds = topPostIds.subList(start, end);

        // DB에서 게시글 조회
        List<PostSimpleDetail> posts = postQueryRepository.findPostSimpleDetailsByIds(pagedPostIds);

        return new PageImpl<>(posts, pageable, totalSize);
    }

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>Redis 캐시에서 주간 인기글 목록을 페이징으로 조회합니다.</p>
     * <p>Redis 장애 시 DB에서 직접 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 주간 인기 게시글 페이지
     */
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        return getTier2CachedPosts(
                PostCacheFlag.WEEKLY,
                FallbackType.WEEKLY,
                pageable,
                () -> postQueryRepository.findWeeklyPopularPosts(pageable)
        );
    }

    /**
     * <h3>레전드 인기 게시글 목록 조회</h3>
     * <p>캐시된 레전드 게시글을 페이지네이션으로 조회합니다.</p>
     * <p>Redis 장애 시 DB에서 직접 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 인기 게시글 목록 페이지
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        return getTier2CachedPosts(
                PostCacheFlag.LEGEND,
                FallbackType.LEGEND,
                pageable,
                () -> postQueryRepository.findLegendaryPosts(pageable)
        );
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>Redis에 캐시된 공지사항 목록을 페이징으로 조회합니다.</p>
     * <p>Redis 장애 시 DB에서 직접 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 공지사항 페이지
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        return getTier2CachedPosts(
                PostCacheFlag.NOTICE,
                FallbackType.NOTICE,
                pageable,
                () -> postQueryRepository.findNoticePosts(pageable)
        );
    }

    /**
     * <h3>Tier2 기반 캐시 조회 공통 로직</h3>
     * <p>Tier2(Set)에서 postId 목록을 조회하고 Tier1(Hash)에서 상세 정보를 획득합니다.</p>
     * <p>Redis 장애 시 fallbackSupplier를 통해 DB에서 직접 조회합니다.</p>
     *
     * @param cacheFlag        캐시 유형
     * @param fallbackType     폴백 유형
     * @param pageable         페이지 정보
     * @param fallbackSupplier 폴백 시 호출할 DB 조회 로직
     * @return 게시글 페이지
     */
    private Page<PostSimpleDetail> getTier2CachedPosts(
            PostCacheFlag cacheFlag,
            FallbackType fallbackType,
            Pageable pageable,
            Supplier<Page<PostSimpleDetail>> fallbackSupplier
    ) {
        try {
            // 1. Tier2에서 전체 postIds 조회
            List<Long> allPostIds = redisTier2PostAdapter.getStoredPostIds(cacheFlag);
            if (allPostIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            // 2. 페이징 처리
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allPostIds.size());

            if (start >= allPostIds.size()) {
                return new PageImpl<>(List.of(), pageable, allPostIds.size());
            }

            List<Long> pagedPostIds = allPostIds.subList(start, end);

            // 3. Hash 캐시 조회 + 개수 비교 + PER 처리
            List<PostSimpleDetail> resultPosts = fetchPostsWithCache(cacheFlag, pagedPostIds);

            return new PageImpl<>(resultPosts, pageable, allPostIds.size());
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애, DB 조회로 전환: {}", cacheFlag, e.getMessage());
            return dbFallbackGateway.execute(fallbackType, pageable, fallbackSupplier);
        }
    }

    /**
     * <h3>Hash 캐시 조회 + 개수 비교 + PER 처리</h3>
     * <p>1. HGETALL로 Hash 전체 조회</p>
     * <p>2. 개수 비교: Tier1 개수 != Tier2 개수 → 캐시 미스, 전체 갱신</p>
     * <p>3. 개수 일치 시 TTL 기반 PER 처리</p>
     * <p>4. Tier2 순서대로 결과 반환</p>
     *
     * @param type    캐시 유형
     * @param orderedPostIds Tier2에서 조회한 postId 목록 (순서 유지)
     * @return 순서가 유지된 게시글 목록
     */
    private List<PostSimpleDetail> fetchPostsWithCache(PostCacheFlag type, List<Long> orderedPostIds) {
        // 1. HGETALL로 Hash 전체 조회
        Map<Long, PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPosts(type);

        // 2. Tier2 전체 개수 조회
        int tier2Count = getTier2Count(type);

        // 3. 개수 비교: 불일치 시 캐시 미스 → 락 기반 전체 갱신
        if (cachedPosts.size() != tier2Count) {
            postCacheRefresh.asyncRefreshWithLock(type);
        } else {
            // 4. 개수 일치 시 TTL 기반 PER 처리 (락 없음, 확률 분산)
            if (redisSimplePostAdapter.shouldRefreshHash(type)) {
                postCacheRefresh.asyncRefreshAllPosts(type);
            }
        }

        // 5. Tier2 순서대로 결과 반환
        return redisSimplePostAdapter.toOrderedList(orderedPostIds, cachedPosts);
    }

    /**
     * <h3>Tier2 전체 개수 조회</h3>
     *
     * @param type 캐시 유형
     * @return Tier2에 저장된 postId 개수
     */
    private int getTier2Count(PostCacheFlag type) {
        if (type == PostCacheFlag.REALTIME) {
            return (int) redisRealTimePostAdapter.getRealtimePopularPostCount();
        }
        return redisTier2PostAdapter.getStoredPostIds(type).size();
    }
}
