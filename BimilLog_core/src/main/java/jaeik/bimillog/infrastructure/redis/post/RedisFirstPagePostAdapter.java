package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <h2>첫 페이지 게시글 Redis List 캐시 어댑터</h2>
 * <p>게시판 첫 페이지(20개)를 Redis List로 캐싱합니다.</p>
 * <p>RedisTemplate의 GenericJackson2JsonRedisSerializer가 자동 직렬화/역직렬화를 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisFirstPagePostAdapter {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 첫 페이지 갱신 분산 락 TTL (5분)
     * <p>스케줄러 갱신 시간보다 충분히 길게 설정</p>
     */
    public static final Duration FIRST_PAGE_REFRESH_LOCK_TTL = Duration.ofMinutes(5);

    /**
     * 첫 페이지 캐시 TTL (1시간)
     * <p>스케줄러가 30분마다 갱신하므로 정상 시 TTL 만료 안 됨</p>
     */
    public static final Duration FIRST_PAGE_CACHE_TTL = Duration.ofHours(1);

    /**
     * 첫 페이지 캐시 List 키
     * <p>Value Type: List (PostSimpleDetail JSON 직렬화)</p>
     * <p>최신 게시글 20개를 순서대로 저장</p>
     */
    public static final String FIRST_PAGE_LIST_KEY = "post:board:first-page";

    /**
     * 첫 페이지 갱신 분산 락 키
     * <p>다중 인스턴스 환경에서 하나의 스케줄러만 캐시를 갱신하도록 보장</p>
     */
    public static final String FIRST_PAGE_REFRESH_LOCK_KEY = "post:board:refresh:lock";

    /**
     * 첫 페이지 캐시 크기 (20개)
     */
    public static final int FIRST_PAGE_SIZE = 20;

    // ===================== 조회 메서드 =====================

    /**
     * <h3>첫 페이지 게시글 조회</h3>
     * <p>Redis List에서 20개 게시글을 조회합니다.</p>
     *
     * @return 게시글 목록 (캐시 미스 시 빈 리스트)
     */
    public List<PostSimpleDetail> getFirstPage() {
        try {
            List<Object> rawList = redisTemplate.opsForList().range(FIRST_PAGE_LIST_KEY, 0, FIRST_PAGE_SIZE - 1);
            if (rawList == null || rawList.isEmpty()) {
                return Collections.emptyList();
            }

            List<PostSimpleDetail> posts = new ArrayList<>(rawList.size());
            for (Object raw : rawList) {
                if (raw instanceof PostSimpleDetail post) {
                    posts.add(post);
                }
            }
            return posts;
        } catch (Exception e) {
            log.warn("[FIRST_PAGE_CACHE] 캐시 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ===================== CRUD 메서드 =====================

    /**
     * <h3>새 게시글 추가</h3>
     * <p>List 맨 앞에 게시글 추가 후 20개 유지</p>
     * <p>실패 시 캐시를 무효화하여 불완전한 상태 방지</p>
     *
     * @param post 추가할 게시글
     */
    public void addNewPost(PostSimpleDetail post) {
        try {
            redisTemplate.opsForList().leftPush(FIRST_PAGE_LIST_KEY, post);
            redisTemplate.opsForList().trim(FIRST_PAGE_LIST_KEY, 0, FIRST_PAGE_SIZE - 1);
        } catch (Exception e) {
            log.warn("[FIRST_PAGE_CACHE] 게시글 추가 실패, 캐시 무효화: postId={}, error={}", post.getId(), e.getMessage());
            invalidateCache();
        }
    }

    /**
     * <h3>게시글 수정</h3>
     * <p>LRANGE로 List 조회 후 postId 매칭 → LSET으로 교체</p>
     * <p>첫 페이지에 없는 글이면 아무 동작 안 함</p>
     *
     * @param postId      수정할 게시글 ID
     * @param updatedPost 수정된 게시글 데이터
     */
    public void updatePost(Long postId, PostSimpleDetail updatedPost) {
        try {
            List<Object> rawList = redisTemplate.opsForList().range(FIRST_PAGE_LIST_KEY, 0, FIRST_PAGE_SIZE - 1);
            if (rawList == null || rawList.isEmpty()) {
                return;
            }

            for (int i = 0; i < rawList.size(); i++) {
                if (rawList.get(i) instanceof PostSimpleDetail post && post.getId().equals(postId)) {
                    redisTemplate.opsForList().set(FIRST_PAGE_LIST_KEY, i, updatedPost);
                    log.debug("[FIRST_PAGE_CACHE] 게시글 수정: postId={}, index={}", postId, i);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("[FIRST_PAGE_CACHE] 게시글 수정 실패, 캐시 무효화: postId={}, error={}", postId, e.getMessage());
            invalidateCache();
        }
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>LRANGE로 List 조회 후 postId 매칭 → LREM으로 제거</p>
     * <p>첫 페이지에 없는 글이면 아무 동작 안 함</p>
     *
     * @param postId 삭제할 게시글 ID
     */
    public void deletePost(Long postId) {
        try {
            List<Object> rawList = redisTemplate.opsForList().range(FIRST_PAGE_LIST_KEY, 0, FIRST_PAGE_SIZE - 1);
            if (rawList == null || rawList.isEmpty()) {
                return;
            }

            for (Object raw : rawList) {
                if (raw instanceof PostSimpleDetail post && post.getId().equals(postId)) {
                    redisTemplate.opsForList().remove(FIRST_PAGE_LIST_KEY, 1, raw);
                    log.debug("[FIRST_PAGE_CACHE] 게시글 삭제: postId={}", postId);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("[FIRST_PAGE_CACHE] 게시글 삭제 실패, 캐시 무효화: postId={}, error={}", postId, e.getMessage());
            invalidateCache();
        }
    }

    // ===================== 전체 갱신 (스케줄러용) =====================

    /**
     * <h3>캐시 전체 갱신</h3>
     * <p>기존 캐시를 삭제하고 새로운 게시글 목록으로 교체</p>
     *
     * @param posts 갱신할 게시글 목록 (최대 20개)
     */
    public void refreshCache(List<PostSimpleDetail> posts) {
        if (posts.isEmpty()) {
            log.warn("[FIRST_PAGE_CACHE] 갱신할 게시글이 없습니다");
            return;
        }

        // DEL → RPUSH → EXPIRE
        redisTemplate.delete(FIRST_PAGE_LIST_KEY);
        redisTemplate.opsForList().rightPushAll(FIRST_PAGE_LIST_KEY, posts.toArray());
        redisTemplate.expire(FIRST_PAGE_LIST_KEY, FIRST_PAGE_CACHE_TTL.toSeconds(), TimeUnit.SECONDS);

        log.info("[FIRST_PAGE_CACHE] 캐시 갱신 완료: {}개", posts.size());
    }

    // ===================== 분산 락 =====================

    /**
     * <h3>갱신 락 획득 시도</h3>
     *
     * @return 락 획득 성공 시 true
     */
    public boolean tryAcquireRefreshLock() {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                FIRST_PAGE_REFRESH_LOCK_KEY,
                "locked",
                FIRST_PAGE_REFRESH_LOCK_TTL.toSeconds(),
                TimeUnit.SECONDS
        );
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * <h3>갱신 락 해제</h3>
     */
    public void releaseRefreshLock() {
        redisTemplate.delete(FIRST_PAGE_REFRESH_LOCK_KEY);
    }

    // ===================== 캐시 무효화 =====================

    /**
     * <h3>캐시 무효화</h3>
     * <p>CRUD 실패 시 불완전한 캐시를 삭제하여 DB 폴백을 유도합니다.</p>
     * <p>삭제 실패 시 Redis 자체 장애이므로 조회 시에도 DB 폴백이 동작합니다.</p>
     */
    public void invalidateCache() {
        try {
            redisTemplate.delete(FIRST_PAGE_LIST_KEY);
        } catch (Exception e) {
            log.error("[FIRST_PAGE_CACHE] 캐시 삭제도 실패 (Redis 장애): {}", e.getMessage());
        }
    }
}
