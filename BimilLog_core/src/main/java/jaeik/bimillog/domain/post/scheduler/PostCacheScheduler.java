package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.List;

/**
 * <h2>PostCacheScheduler</h2>
 * <p>게시글 캐시 동기화를 담당하는 스케줄링 서비스</p>
 * <p>공지/첫 페이지/실시간 인기글 JSON LIST 캐시를 24시간마다 재구축합니다.</p>
 * <p>앱 기동 시 {@link PostConstruct}로 전체 캐시 워밍을 1회 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, logExecutionTime = true, message = "스케줄 캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheScheduler {
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;
    private final PostQueryRepository postQueryRepository;
    private final PostRepository postRepository;
    private final FeaturedPostScheduler featuredPostScheduler;

    @PostConstruct
    public void warmUpCaches() {
        log.info("앱 기동 시 캐시 워밍 시작");
        try { refreshFirstPageCache(); } catch (Exception e) { log.warn("첫 페이지 캐시 워밍 실패: {}", e.getMessage(), e); }
        try { refreshNoticePosts(); } catch (Exception e) { log.warn("공지사항 캐시 워밍 실패: {}", e.getMessage(), e); }
        try { featuredPostScheduler.queryAndReplaceCache("WEEKLY", PostQueryType.WEEKLY_SCHEDULER, RedisKey.POST_WEEKLY_JSON_KEY); } catch (Exception e) { log.warn("주간 인기글 캐시 워밍 실패: {}", e.getMessage(), e); }
        try { featuredPostScheduler.queryAndReplaceCache("LEGEND", PostQueryType.LEGEND_SCHEDULER, RedisKey.POST_LEGEND_JSON_KEY); } catch (Exception e) { log.warn("전설 게시글 캐시 워밍 실패: {}", e.getMessage(), e); }
        log.info("앱 기동 시 캐시 워밍 완료");
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    public void refreshNoticePosts() {
        List<PostSimpleDetail> posts = postRepository.findByIsNoticeTrueOrderByIdDesc().stream().map(PostSimpleDetail::from).toList();
        replaceIfNotEmpty("NOTICE", RedisKey.POST_NOTICE_JSON_KEY, posts);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Retryable(retryFor = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 2000, multiplier = 4))
    public void refreshFirstPageCache() {
        List<PostSimpleDetail> posts = postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE);
        replaceIfNotEmpty("첫 페이지", RedisKey.FIRST_PAGE_JSON_KEY, posts);
    }

    @Recover
    public void recoverFeaturedUpdate(Exception e) {
        log.error("[FEATURED_SCHEDULE] 갱신 최종 실패 (5회 재시도): {}", e.getMessage(), e);
    }

    private void replaceIfNotEmpty(String type, String redisKey, List<PostSimpleDetail> posts) {
        if (posts.isEmpty()) {
            log.info("{}에 대한 게시글이 없어 캐시 갱신을 건너뜁니다.", type);
            return;
        }
        redisPostListUpdateAdapter.replaceList(redisKey, posts, RedisKey.DEFAULT_CACHE_TTL);
        log.info("{} 캐시 갱신 완료. {}개의 게시글이 처리됨", type, posts.size());
    }

}
