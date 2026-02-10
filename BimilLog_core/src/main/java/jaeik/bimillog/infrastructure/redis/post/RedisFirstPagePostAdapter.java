package jaeik.bimillog.infrastructure.redis.post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jaeik.bimillog.infrastructure.redis.RedisKey;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <h2>첫 페이지 게시글 Redis List 캐시 어댑터</h2>
 * <p>게시판 첫 페이지(20개)를 Redis List로 캐싱합니다.</p>
 * <p>List에는 게시글 ID(Long)만 저장하고, 실제 데이터는 글 단위 Hash에서 조회합니다.</p>
 * <p>캐시 갱신은 24시간 스케줄러가 담당하며, 글 작성/삭제 시 즉시 반영됩니다.</p>
 *
 * @author Jaeik
 * @version 3.2.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisFirstPagePostAdapter {

    private final RedisTemplate<String, Long> longRedisTemplate;

    private static final String FIRST_PAGE_LIST_KEY = RedisKey.FIRST_PAGE_LIST_KEY;
    private static final int FIRST_PAGE_SIZE = RedisKey.FIRST_PAGE_SIZE;

    // ===================== 조회 메서드 =====================

    /**
     * <h3>첫 페이지 게시글 ID 목록 조회</h3>
     * <p>Redis List에서 20개 게시글 ID를 조회합니다.</p>
     *
     * @return 게시글 ID 목록 (캐시 미스 시 빈 리스트)
     */
    public List<Long> getFirstPageIds() {
        try {
            List<Long> ids = longRedisTemplate.opsForList().range(FIRST_PAGE_LIST_KEY, 0, FIRST_PAGE_SIZE - 1);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }
            return ids;
        } catch (Exception e) {
            log.warn("[FIRST_PAGE_CACHE] 캐시 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ===================== CRUD 메서드 =====================

    /**
     * <h3>새 게시글 ID 추가</h3>
     * <p>List 맨 앞에 게시글 ID 추가 후 20개 유지</p>
     *
     * @param postId 추가할 게시글 ID
     */
    public void addNewPostId(Long postId) {
        try {
            longRedisTemplate.opsForList().leftPush(FIRST_PAGE_LIST_KEY, postId);
            longRedisTemplate.opsForList().trim(FIRST_PAGE_LIST_KEY, 0, FIRST_PAGE_SIZE - 1);
        } catch (Exception e) {
            log.warn("[FIRST_PAGE_CACHE] 게시글 추가 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>LREM으로 ID를 직접 제거합니다.</p>
     * <p>첫 페이지에 없는 글이면 아무 동작 안 함</p>
     *
     * @param postId 삭제할 게시글 ID
     * @return 삭제 후 리스트의 마지막 게시글 ID (삭제되지 않았거나 리스트가 비면 null)
     */
    public Long deletePost(Long postId) {
        try {
            Long removed = longRedisTemplate.opsForList().remove(FIRST_PAGE_LIST_KEY, 1, postId);
            if (removed == null || removed == 0) {
                return null;
            }
            log.debug("[FIRST_PAGE_CACHE] 게시글 삭제: postId={}", postId);

            // 삭제 후 마지막 게시글 ID 반환 (다음 글 보충용)
            return longRedisTemplate.opsForList().index(FIRST_PAGE_LIST_KEY, -1);
        } catch (Exception e) {
            log.warn("[FIRST_PAGE_CACHE] 게시글 삭제 실패: postId={}, error={}", postId, e.getMessage());
            return null;
        }
    }

    /**
     * <h3>게시글 ID 보충 (RPUSH)</h3>
     * <p>삭제로 인해 부족해진 첫 페이지 캐시에 다음 게시글 ID를 추가합니다.</p>
     *
     * @param postId 보충할 게시글 ID
     */
    public void appendPostId(Long postId) {
        try {
            Long currentSize = longRedisTemplate.opsForList().size(FIRST_PAGE_LIST_KEY);
            if (currentSize != null && currentSize < FIRST_PAGE_SIZE) {
                longRedisTemplate.opsForList().rightPush(FIRST_PAGE_LIST_KEY, postId);
                log.debug("[FIRST_PAGE_CACHE] 게시글 보충: postId={}", postId);
            }
        } catch (Exception e) {
            log.warn("[FIRST_PAGE_CACHE] 게시글 보충 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    // ===================== 전체 갱신 (스케줄러용) =====================

    /**
     * <h3>캐시 전체 갱신</h3>
     * <p>기존 캐시를 삭제하고 새로운 게시글 ID 목록으로 교체 (TTL 24시간 30분)</p>
     *
     * @param postIds 갱신할 게시글 ID 목록 (최대 20개)
     */
    public void refreshCache(List<Long> postIds) {
        if (postIds.isEmpty()) {
            log.warn("[FIRST_PAGE_CACHE] 갱신할 게시글이 없습니다");
            return;
        }

        // DEL → RPUSH → EXPIRE
        longRedisTemplate.delete(FIRST_PAGE_LIST_KEY);
        longRedisTemplate.opsForList().rightPushAll(FIRST_PAGE_LIST_KEY, postIds);
        longRedisTemplate.expire(FIRST_PAGE_LIST_KEY, RedisKey.DEFAULT_CACHE_TTL.toSeconds(), TimeUnit.SECONDS);

        log.info("[FIRST_PAGE_CACHE] 캐시 갱신 완료: {}개", postIds.size());
    }
}
