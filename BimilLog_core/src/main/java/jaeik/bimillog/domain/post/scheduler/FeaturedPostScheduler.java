package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>FeaturedPostScheduler</h2>
 * <p>게시글 인기도 기반 캐시 동기화를 담당하는 스케줄링 서비스</p>
 * <p>DB 조회 → featuredType 업데이트 → 글 단위 Hash 생성 → SET 인덱스 교체를 통째로 재시도합니다.</p>
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
    private final PostQueryRepository postQueryRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>주간 인기 게시글 스케줄링 갱신</h3>
     * <p>1일마다 DB 조회 → featuredType 업데이트 → 글 단위 Hash 생성 → SET 인덱스 교체</p>
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

        // Post.featuredType 업데이트 (기존 WEEKLY 초기화 후 새로 설정)
        postRepository.clearFeaturedType(PostCacheFlag.WEEKLY);
        List<Long> ids = posts.stream().map(PostSimpleDetail::getId).toList();
        postRepository.setFeaturedType(ids, PostCacheFlag.WEEKLY);
        log.info("WEEKLY featuredType 업데이트 완료: {}개", ids.size());

        // 글 단위 Hash 생성 + SET 인덱스 교체
        posts.forEach(redisPostHashAdapter::createPostHash);
        Set<Long> idSet = posts.stream().map(PostSimpleDetail::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        redisPostIndexAdapter.replaceIndex(RedisKey.POST_WEEKLY_IDS_KEY, idSet, RedisKey.POST_CACHE_TTL_WEEKLY_LEGEND);
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
     * <p>1일마다 DB 조회 → featuredType 업데이트 → 글 단위 Hash 생성 → SET 인덱스 교체</p>
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

        // Post.featuredType 업데이트 (기존 LEGEND 초기화 후 새로 설정, WEEKLY 덮어쓰기)
        postRepository.clearFeaturedType(PostCacheFlag.LEGEND);
        List<Long> ids = posts.stream().map(PostSimpleDetail::getId).toList();
        postRepository.setFeaturedTypeOverriding(ids, PostCacheFlag.LEGEND, PostCacheFlag.WEEKLY);
        log.info("LEGEND featuredType 업데이트 완료: {}개", ids.size());

        // 글 단위 Hash 생성 + SET 인덱스 교체
        posts.forEach(redisPostHashAdapter::createPostHash);
        Set<Long> idSet = posts.stream().map(PostSimpleDetail::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        redisPostIndexAdapter.replaceIndex(RedisKey.POST_LEGEND_IDS_KEY, idSet, RedisKey.POST_CACHE_TTL_WEEKLY_LEGEND);
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
