package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostSavePort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostQueryPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h2>PostCacheService</h2>
 * <p>게시글 캐시 관리 관련 UseCase 인터페이스의 구현체로서 캐시 조회 및 동기화 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>실시간, 주간, 레전드 인기글 조회</p>
 * <p>공지사항 조회 및 상태 변경에 따른 Redis 캐시 동기화와 데이터 무결성 보장을 위한 비즈니스 규칙을 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheService implements PostCacheUseCase {

    private final RedisPostSavePort redisPostSavePort;
    private final PostQueryPort postQueryPort;
    private final RedisPostQueryPort redisPostQueryPort;

    /**
     * <h3>실시간 인기 게시글 조회</h3>
     * <p>Redis Sorted Set에서 postId 목록을 조회하고 posts:realtime Hash에서 상세 정보를 획득합니다.</p>
     * <p>이벤트 기반 점수 시스템으로 관리되는 postId 목록 조회 → 목록 캐시 활용</p>
     * <p>캐시 미스 시 DB에서 조회 후 posts:realtime에 저장합니다.</p>
     *
     * @return Redis에서 조회된 실시간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<PostSimpleDetail> getRealtimePosts() {
        // 1. score:realtime에서 상위 5개 postId 조회
        List<Long> realtimePostIds = redisPostQueryPort.getRealtimePopularPostIds();
        if (realtimePostIds.isEmpty()) {
            return List.of();
        }

        // 2. posts:realtime Hash에서 PostSimpleDetail 조회
        List<PostSimpleDetail> cachedList = redisPostQueryPort.getCachedPostList(PostCacheFlag.REALTIME);

        // 3. 캐시된 데이터를 Map으로 변환 (빠른 조회)
        Map<Long, PostSimpleDetail> cachedMap = cachedList.stream()
                .collect(Collectors.toMap(PostSimpleDetail::getId, detail -> detail));

        // 4. postId 순서대로 조회하며 캐시 미스 시 DB 조회 후 추가
        return realtimePostIds.stream()
                .map(postId -> {
                    PostSimpleDetail cached = cachedMap.get(postId);
                    if (cached != null) {
                        return cached;
                    }

                    // 캐시 미스: DB 조회 후 posts:realtime에 추가
                    PostDetail postDetail = postQueryPort.findPostDetailWithCounts(postId, null).orElse(null);
                    if (postDetail == null) {
                        return null;
                    }

                    PostSimpleDetail simpleDetail = postDetail.toSimpleDetail();
                    redisPostSavePort.cachePostList(PostCacheFlag.REALTIME, List.of(simpleDetail));
                    return simpleDetail;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>Redis 캐시에서 주간 인기글 목록을 조회합니다.</p>
     * <p>캐시 미스 시 postIds 저장소에서 ID 목록을 가져와 DB 조회 후 반환합니다.</p>
     *
     * @return Redis에서 조회된 주간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<PostSimpleDetail> getWeeklyPosts() {
        List<PostSimpleDetail> cachedList = redisPostQueryPort.getCachedPostList(PostCacheFlag.WEEKLY);

        // 캐시 미스 시 postIds 저장소에서 복구
        if (cachedList.isEmpty()) {
            return recoverFromStoredPostIds(PostCacheFlag.WEEKLY);
        }

        return cachedList;
    }

    /**
     * <h3>레전드 인기 게시글 목록 조회 (페이징)</h3>
     * <p>캐시된 레전드 게시글을 페이지네이션으로 조회합니다.</p>
     * <p>캐시 미스 시 postIds 저장소에서 ID 목록을 가져와 DB 조회 후 캐시에 저장합니다.</p>
     * <p>Redis Hash 구조를 활용하여 효율적인 페이징을 제공합니다.</p>
     *
     * @param type 조회할 인기 게시글 유형 (PostCacheFlag.LEGEND만 지원)
     * @param pageable 페이지 정보
     * @return 인기 게시글 목록 페이지
     * @throws PostCustomException 유효하지 않은 캐시 유형인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSimpleDetail> getPopularPostLegend(PostCacheFlag type, Pageable pageable) {
        Page<PostSimpleDetail> cachedPage = redisPostQueryPort.getCachedPostListPaged(pageable);

        // 캐시 미스 시 postIds 저장소에서 복구 후 재조회
        if (cachedPage.isEmpty()) {
            List<PostSimpleDetail> recovered = recoverFromStoredPostIds(PostCacheFlag.LEGEND);
            if (!recovered.isEmpty()) {
                redisPostSavePort.cachePostList(PostCacheFlag.LEGEND, recovered);
                cachedPage = redisPostQueryPort.getCachedPostListPaged(pageable);
            }
        }

        return cachedPage;
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>Redis에 캐시된 공지사항 목록을 조회합니다.</p>
     * <p>postIds 저장소 ID 개수와 캐시 목록 개수를 비교하여 정합성을 검증합니다.</p>
     * <p>개수 불일치 시 캐시 미스로 판단하고 postIds 저장소에서 ID 목록을 가져와 DB 조회 후 반환합니다.</p>
     *
     * @return 공지사항 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<PostSimpleDetail> getNoticePosts() {
        // 1. postIds:notice Set에서 실제 공지 ID 목록 조회
        List<Long> storedPostIds = redisPostQueryPort.getStoredPostIds(PostCacheFlag.NOTICE);

        // 2. posts:notice Hash에서 캐시된 목록 조회
        List<PostSimpleDetail> cachedList = redisPostQueryPort.getCachedPostList(PostCacheFlag.NOTICE);

        // 3. 개수 비교: 저장소 ID 개수 != 캐시 목록 개수 → 캐시 미스
        if (cachedList.size() != storedPostIds.size()) {
            return recoverFromStoredPostIds(PostCacheFlag.NOTICE);
        }

        return cachedList;
    }

    /**
     * <h3>postIds 저장소에서 캐시 복구</h3>
     * <p>목록 캐시 TTL 만료 시 postIds 저장소에서 ID 목록을 가져와 DB 조회 후 목록을 재구성합니다.</p>
     *
     * @param type 복구할 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @return 복구된 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<PostSimpleDetail> recoverFromStoredPostIds(PostCacheFlag type) {
        List<Long> storedPostIds = redisPostQueryPort.getStoredPostIds(type);
        if (storedPostIds.isEmpty()) {
            return List.of();
        }

        // DB에서 PostDetail 조회 후 PostSimpleDetail 변환
        return storedPostIds.stream()
                .map(postId -> postQueryPort.findPostDetailWithCounts(postId, null).orElse(null))
                .filter(Objects::nonNull)
                .map(PostDetail::toSimpleDetail)
                .toList();
    }
}