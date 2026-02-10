package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * <h2>FeaturedPostScheduler</h2>
 * <p>게시글 인기도 기반 캐시 동기화를 담당하는 스케줄링 서비스</p>
 * <p>DB 조회 → 플래그 업데이트 → 글 단위 Hash 생성 → List 인덱스 교체를 통째로 재시도합니다.</p>
 * <p>첫 페이지 캐시도 1일마다 초기화 후 재생성하여 오염 데이터를 방지합니다.</p>
 * <p>이벤트 발행은 재시도 범위에서 제외됩니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Log(logResult = false, logExecutionTime = true, message = "스케줄 캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class FeaturedPostScheduler {
    private final RedisPostHashAdapter redisPostHashAdapter;
    private final RedisPostIndexAdapter redisPostIndexAdapter;
    private final RedisFirstPagePostAdapter redisFirstPagePostAdapter;
    private final PostQueryRepository postQueryRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>주간 인기 게시글 스케줄링 갱신</h3>
     * <p>1일마다 DB 조회 → 플래그 업데이트 → 글 단위 Hash 생성 → SET 인덱스 교체</p>
     * <p>실패 시 전체를 재시도합니다. (2s→8s→32s→128s→512s, 최대 5회 재시도)</p>
     */
    @Scheduled(fixedRate = 60000 * 1440)
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 6,
            backoff = @Backoff(delay = 2000, multiplier = 4)
    )
    @Transactional
    public void updateWeeklyPopularPosts() {
        List<PostSimpleDetail> posts = postQueryRepository.findWeeklyPopularPosts();

        if (posts.isEmpty()) {
            log.info("WEEKLY에 대한 인기 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        // isWeekly 플래그 업데이트 (기존 초기화 후 새로 설정)
        postRepository.clearWeeklyFlag();
        List<Long> ids = posts.stream().map(PostSimpleDetail::getId).toList();
        postRepository.setWeeklyFlag(ids);
        log.info("WEEKLY 플래그 업데이트 완료: {}개", ids.size());

        // 글 단위 Hash 생성 + List 인덱스 교체 (DB 인기순 유지)
        posts.forEach(redisPostHashAdapter::createPostHash);
        List<Long> idList = posts.stream().map(PostSimpleDetail::getId).toList();
        redisPostIndexAdapter.replaceIndex(RedisKey.POST_WEEKLY_IDS_KEY, idList, RedisKey.DEFAULT_CACHE_TTL);
        log.info("WEEKLY 캐시 업데이트 완료. {}개의 게시글이 처리됨", posts.size());

        // 이벤트 발행 (재시도 범위 제외)
        try {
            publishFeaturedEventFromSimpleDetails(posts,
                    "주간 인기 게시글로 선정되었어요!",
                    NotificationType.POST_FEATURED_WEEKLY);
        } catch (Exception e) {
            log.error("WEEKLY 이벤트 발행 실패: {}", e.getMessage());
        }
    }

    /**
     * <h3>전설 게시글 스케줄링 갱신</h3>
     * <p>1일마다 DB 조회 → 플래그 업데이트 → 글 단위 Hash 생성 → SET 인덱스 교체</p>
     * <p>실패 시 전체를 재시도합니다. (2s→8s→32s→128s→512s, 최대 5회 재시도)</p>
     */
    @Scheduled(fixedRate = 60000 * 1440)
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 6,
            backoff = @Backoff(delay = 2000, multiplier = 4)
    )
    @Transactional
    public void updateLegendaryPosts() {
        List<PostSimpleDetail> posts = postQueryRepository.findLegendaryPosts();

        if (posts.isEmpty()) {
            log.info("LEGEND에 대한 인기 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        // isLegend 플래그 업데이트 (기존 초기화 후 새로 설정)
        postRepository.clearLegendFlag();
        List<Long> ids = posts.stream().map(PostSimpleDetail::getId).toList();
        postRepository.setLegendFlag(ids);
        log.info("LEGEND 플래그 업데이트 완료: {}개", ids.size());

        // 글 단위 Hash 생성 + List 인덱스 교체 (DB 인기순 유지)
        posts.forEach(redisPostHashAdapter::createPostHash);
        List<Long> idList = posts.stream().map(PostSimpleDetail::getId).toList();
        redisPostIndexAdapter.replaceIndex(RedisKey.POST_LEGEND_IDS_KEY, idList, RedisKey.DEFAULT_CACHE_TTL);
        log.info("LEGEND 캐시 업데이트 완료. {}개의 게시글이 처리됨", posts.size());

        // 이벤트 발행 (재시도 범위 제외)
        try {
            publishFeaturedEventFromSimpleDetails(posts,
                    "명예의 전당에 등극했어요!",
                    NotificationType.POST_FEATURED_LEGEND);
        } catch (Exception e) {
            log.error("LEGEND 이벤트 발행 실패: {}", e.getMessage());
        }
    }

    /**
     * <h3>공지사항 캐시 스케줄링 갱신</h3>
     * <p>1일마다 DB에서 featuredType=NOTICE인 게시글을 조회하여 캐시를 갱신합니다.</p>
     * <p>오류 데이터가 영구 저장되는 것을 방지하기 위해 TTL을 설정합니다.</p>
     * <p>실패 시 전체를 재시도합니다. (2s→8s→32s→128s→512s, 최대 5회 재시도)</p>
     */
    @Scheduled(fixedRate = 60000 * 1440)
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 6,
            backoff = @Backoff(delay = 2000, multiplier = 4)
    )
    public void refreshNoticePosts() {
        List<PostSimpleDetail> posts = postQueryRepository.findNoticePostsForScheduler();

        if (posts.isEmpty()) {
            log.info("NOTICE 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        // 글 단위 Hash 생성 + List 인덱스 교체 (최신순: ID 내림차순)
        List<PostSimpleDetail> sortedPosts = posts.stream()
                .sorted(Comparator.comparingLong(PostSimpleDetail::getId).reversed())
                .toList();
        sortedPosts.forEach(redisPostHashAdapter::createPostHash);
        List<Long> idList = sortedPosts.stream().map(PostSimpleDetail::getId).toList();
        redisPostIndexAdapter.replaceIndex(RedisKey.POST_NOTICE_IDS_KEY, idList, RedisKey.DEFAULT_CACHE_TTL);
        log.info("NOTICE 캐시 업데이트 완료. {}개의 게시글이 처리됨", sortedPosts.size());
    }

    /**
     * <h3>첫 페이지 캐시 스케줄링 갱신</h3>
     * <p>1일마다 DB에서 최신 글 20개를 조회하여 첫 페이지 캐시를 초기화 후 재생성합니다.</p>
     * <p>오염 데이터 방지를 위해 기존 캐시를 삭제하고 새로 생성합니다. (TTL 24시간 30분)</p>
     * <p>실패 시 전체를 재시도합니다. (2s→8s→32s→128s→512s, 최대 5회 재시도)</p>
     */
    @Scheduled(fixedRate = 60000 * 1440)
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 6,
            backoff = @Backoff(delay = 2000, multiplier = 4)
    )
    public void refreshFirstPageCache() {
        List<PostSimpleDetail> posts = postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE);
        if (posts.size() > RedisKey.FIRST_PAGE_SIZE) {
            posts = posts.subList(0, RedisKey.FIRST_PAGE_SIZE);
        }

        if (posts.isEmpty()) {
            log.info("FIRST_PAGE 게시글이 없어 캐시 업데이트를 건너뜁니다.");
            return;
        }

        posts.forEach(redisPostHashAdapter::createPostHash);
        List<Long> idList = posts.stream().map(PostSimpleDetail::getId).toList();
        redisFirstPagePostAdapter.refreshCache(idList);
        log.info("FIRST_PAGE 캐시 업데이트 완료. {}개의 게시글이 처리됨", posts.size());
    }

    /**
     * <h3>스케줄러 최종 실패 복구</h3>
     * <p>5회 재시도 후에도 실패 시 로그만 남깁니다.</p>
     */
    @Recover
    public void recoverFeaturedUpdate(Exception e) {
        log.error("[FEATURED_SCHEDULE] 갱신 최종 실패 (5회 재시도): {}", e.getMessage(), e);
    }

    private void publishFeaturedEventFromSimpleDetails(List<PostSimpleDetail> posts, String sseMessage,
                                                       NotificationType notificationType) {
        posts.stream().filter(post -> post.getMemberId() != null)
                .forEach(post -> {
                    eventPublisher.publishEvent(new PostFeaturedEvent(
                            post.getMemberId(),
                            sseMessage,
                            post.getId(),
                            notificationType,
                            post.getTitle()
                    ));
                    log.info("게시글 ID {}에 대한 {} 알림 이벤트 발행: 회원 ID={}",
                            post.getId(), notificationType, post.getMemberId());
                });
    }
}
