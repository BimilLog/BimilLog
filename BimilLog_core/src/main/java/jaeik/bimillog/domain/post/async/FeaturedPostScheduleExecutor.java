package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.FeaturedPost;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
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

import java.util.ArrayList;
import java.util.List;

/**
 * <h2>FeaturedPostScheduleExecutor</h2>
 * <p>특집 게시글 스케줄러의 DB 조회 및 저장을 담당합니다.</p>
 * <p>DB 조회와 저장 모두 {@code @Retryable}(7회, 지수 백오프 3배)로 보호되며, 저장은 별도 트랜잭션으로 실행됩니다.</p>
 *
 * <h3>재시도 전략 (조회/저장 동일):</h3>
 * <ul>
 *     <li>최대 7회 시도 (초기 1회 + 재시도 6회)</li>
 *     <li>지수 백오프: 1s → 3s → 9s → 27s → 81s → 243s</li>
 * </ul>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeaturedPostScheduleExecutor {

    private final PostQueryRepository postQueryRepository;
    private final FeaturedPostRepository featuredPostRepository;

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
     * <p>기존 featured_post 테이블 데이터가 유지되므로 이전 데이터로 서비스됩니다.</p>
     */
    @Recover
    public List<PostSimpleDetail> recoverFetchPosts(Exception e) {
        log.error("[FEATURED_SCHEDULE] DB 조회 최종 실패 (7회 시도): {}", e.getMessage(), e);
        return List.of();
    }

    /**
     * <h3>특집 게시글 DB 저장</h3>
     * <p>기존 해당 유형의 특집 게시글을 모두 삭제하고 새로운 목록을 저장합니다.</p>
     * <p>DB 저장 실패 시 지수 백오프(1s→3s→9s→27s→81s→243s)로 최대 7회 시도합니다.</p>
     *
     * @param posts 특집으로 선정된 게시글 목록
     * @param type  특집 유형 (WEEKLY, LEGEND)
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
    public void saveFeaturedPosts(List<PostSimpleDetail> posts, PostCacheFlag type) {
        featuredPostRepository.deleteAllByType(type);
        List<FeaturedPost> featuredPostList = new ArrayList<>();

        for (PostSimpleDetail post : posts) {
            featuredPostList.add(FeaturedPost.createFeaturedPost(post, type));
        }

        featuredPostRepository.saveAll(featuredPostList);
        log.info("{} 특집 게시글 DB 저장 완료: {}개", type, featuredPostList.size());
    }

    /**
     * <h3>DB 저장 최종 실패 복구</h3>
     * <p>7회 재시도 후에도 실패 시 로그를 남기고 종료합니다.</p>
     * <p>기존 featured_post 테이블 데이터가 유지되므로 이전 데이터로 서비스됩니다.</p>
     */
    @Recover
    public void recoverSaveFeaturedPosts(Exception e, List<PostSimpleDetail> posts, PostCacheFlag type) {
        log.error("[FEATURED_SCHEDULE] {} DB 저장 최종 실패 (7회 시도): {}", type, e.getMessage(), e);
    }

}
