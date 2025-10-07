package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
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
import java.util.Objects;

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

    private final RedisPostCommandPort redisPostCommandPort;
    private final PostQueryPort postQueryPort;
    private final RedisPostQueryPort redisPostQueryPort;

    /**
     * <h3>실시간 인기 게시글 조회</h3>
     * <p>Redis Sorted Set에서 postId 목록을 조회하고 캐시 어사이드 패턴으로 상세 정보를 획득합니다.</p>
     * <p>이벤트 기반 점수 시스템으로 관리되는 postId 목록 조회 → 상세 캐시 활용</p>
     * <p>캐시 미스 시 DB에서 조회 후 캐시에 저장합니다.</p>
     *
     * @return Redis에서 조회된 실시간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<PostSimpleDetail> getRealtimePosts() {
        List<Long> realtimePostIds = redisPostQueryPort.getRealtimePopularPostIds();
        return realtimePostIds.stream()
                .map(postId -> {
                    // 캐시 어사이드 패턴으로 조회 (캐시 미스 시 DB 조회 후 캐시 저장)
                    PostDetail postDetail = redisPostQueryPort.getCachedPostIfExists(postId);
                    if (postDetail == null) {
                        postDetail = postQueryPort.findPostDetailWithCounts(postId, null).orElse(null);
                        if (postDetail != null) {
                            redisPostCommandPort.cachePostDetail(postDetail);
                        }
                    }
                    return postDetail;
                })
                .filter(Objects::nonNull)
                .map(PostDetail::toSearchResult)
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
     * <p>캐시된 레전드 게시글을 페이지네이션으로 조회합니다. 캐시가 없는 경우 업데이트 후 조회합니다.</p>
     * <p>Redis List 구조를 활용하여 효율적인 페이징을 제공합니다.</p>
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
        return redisPostQueryPort.getCachedPostListPaged(pageable);
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>Redis에 캐시된 공지사항 목록을 조회합니다.</p>
     * <p>캐시 미스 시 postIds 저장소에서 ID 목록을 가져와 DB 조회 후 반환합니다.</p>
     *
     * @return 공지사항 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<PostSimpleDetail> getNoticePosts() {
        List<PostSimpleDetail> cachedList = redisPostQueryPort.getCachedPostList(PostCacheFlag.NOTICE);

        // 캐시 미스 시 postIds 저장소에서 복구
        if (cachedList.isEmpty()) {
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
                .map(PostDetail::toSearchResult)
                .toList();
    }
}