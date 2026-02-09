package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>FeaturedPostScheduleExecutor</h2>
 * <p>특집 게시글 스케줄러의 DB 조회 및 featuredType 업데이트를 담당합니다.</p>
 * <p>DB 조회와 업데이트 모두 {@code @Retryable}(7회, 지수 백오프 3배)로 보호됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeaturedPostScheduleExecutor {

    private final PostQueryRepository postQueryRepository;
    private final PostRepository postRepository;

    /**
     * <h3>주간 인기 게시글 DB 조회</h3>
     * <p>지난 7일간의 조회수와 좋아요 종합 점수를 기반으로 주간 인기 게시글을 조회합니다.</p>
     * <p>DB 조회 실패 시 지수 백오프(1s→3s→9s→27s→81s→243s)로 최대 7회 시도합니다.</p>
     *
     * @return 주간 인기 게시글 목록 (최종 실패 시 빈 목록)
     */
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 7,
            backoff = @Backoff(delay = 1000, multiplier = 3)
    )
    public List<PostSimpleDetail> fetchWeeklyPosts() {
        return postQueryRepository.findWeeklyPopularPosts();
    }

    /**
     * <h3>전설 게시글 DB 조회</h3>
     * <p>전체 기간 조회수와 좋아요 종합 점수를 기반으로 전설 게시글을 조회합니다.</p>
     * <p>DB 조회 실패 시 지수 백오프(1s→3s→9s→27s→81s→243s)로 최대 7회 시도합니다.</p>
     *
     * @return 전설 게시글 목록 (최종 실패 시 빈 목록)
     */
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 7,
            backoff = @Backoff(delay = 1000, multiplier = 3)
    )
    public List<PostSimpleDetail> fetchLegendaryPosts() {
        return postQueryRepository.findLegendaryPosts();
    }

    /**
     * <h3>DB 조회 최종 실패 복구</h3>
     * <p>7회 재시도 후에도 실패 시 빈 목록을 반환합니다.</p>
     */
    @Recover
    public List<PostSimpleDetail> recoverFetchPosts(Exception e) {
        log.error("[FEATURED_SCHEDULE] DB 조회 최종 실패 (7회 시도): {}", e.getMessage(), e);
        return List.of();
    }

    /**
     * <h3>WEEKLY featuredType 업데이트</h3>
     * <p>기존 WEEKLY 초기화 후, 새로운 WEEKLY 설정 (NOTICE/LEGEND는 덮어쓰지 않음)</p>
     *
     * @param posts 주간 인기글로 선정된 게시글 목록
     */
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 7,
            backoff = @Backoff(delay = 1000, multiplier = 3)
    )
    @Transactional
    public void updateWeeklyFeaturedType(List<PostSimpleDetail> posts) {
        postRepository.clearFeaturedType(PostCacheFlag.WEEKLY);
        List<Long> ids = posts.stream().map(PostSimpleDetail::getId).toList();
        postRepository.setFeaturedType(ids, PostCacheFlag.WEEKLY);
        log.info("WEEKLY featuredType 업데이트 완료: {}개", ids.size());
    }

    /**
     * <h3>LEGEND featuredType 업데이트</h3>
     * <p>기존 LEGEND 초기화 후, 새로운 LEGEND 설정 (WEEKLY는 덮어쓰지만 NOTICE는 유지)</p>
     *
     * @param posts 레전드로 선정된 게시글 목록
     */
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 7,
            backoff = @Backoff(delay = 1000, multiplier = 3)
    )
    @Transactional
    public void updateLegendFeaturedType(List<PostSimpleDetail> posts) {
        postRepository.clearFeaturedType(PostCacheFlag.LEGEND);
        List<Long> ids = posts.stream().map(PostSimpleDetail::getId).toList();
        postRepository.setFeaturedTypeOverriding(ids, PostCacheFlag.LEGEND, PostCacheFlag.WEEKLY);
        log.info("LEGEND featuredType 업데이트 완료: {}개", ids.size());
    }

    /**
     * <h3>featuredType 업데이트 최종 실패 복구</h3>
     */
    @Recover
    public void recoverUpdateFeaturedType(Exception e, List<PostSimpleDetail> posts) {
        log.error("[FEATURED_SCHEDULE] featuredType 업데이트 최종 실패 (7회 시도): {}", e.getMessage(), e);
    }
}
