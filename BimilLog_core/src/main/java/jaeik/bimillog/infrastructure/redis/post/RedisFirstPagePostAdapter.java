package jaeik.bimillog.infrastructure.redis.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <h2>첫 페이지 게시글 Redis List 캐시 어댑터</h2>
 * <p>게시판 첫 페이지(20개)를 Redis List로 캐싱합니다.</p>
 * <p>Lua 스크립트를 사용하여 CRUD 작업을 원자적으로 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisFirstPagePostAdapter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 새 게시글 추가 스크립트: LPUSH + LTRIM 0 19
     * <p>List 앞에 새 게시글 추가 후 20개로 유지</p>
     */
    private static final RedisScript<Long> CREATE_POST_SCRIPT;

    /**
     * 게시글 수정 스크립트: List 순회 → ID 매칭 → LSET
     * <p>반환값: 1 (성공), 0 (해당 ID 없음)</p>
     */
    private static final RedisScript<Long> UPDATE_POST_SCRIPT;

    /**
     * 게시글 삭제 스크립트: List 순회 → ID 매칭 → LREM
     * <p>반환값: 삭제된 JSON 문자열 또는 nil</p>
     */
    private static final RedisScript<String> DELETE_POST_SCRIPT;

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


    static {
        // 1. 생성 스크립트: 맨 앞에 추가 후 20개 유지
        String createScript =
                "redis.call('LPUSH', KEYS[1], ARGV[1]) " +
                "redis.call('LTRIM', KEYS[1], 0, 19) " +
                "return redis.call('LLEN', KEYS[1])";

        DefaultRedisScript<Long> createPostScript = new DefaultRedisScript<>();
        createPostScript.setScriptText(createScript);
        createPostScript.setResultType(Long.class);
        CREATE_POST_SCRIPT = createPostScript;

        // 2. 수정 스크립트: ID 매칭 후 교체
        String updateScript =
                "local list = redis.call('LRANGE', KEYS[1], 0, -1) " +
                "for i, item in ipairs(list) do " +
                "    local decoded = cjson.decode(item) " +
                "    if decoded.id == tonumber(ARGV[1]) then " +
                "        redis.call('LSET', KEYS[1], i - 1, ARGV[2]) " +
                "        return 1 " +
                "    end " +
                "end " +
                "return 0";

        DefaultRedisScript<Long> updatePostScript = new DefaultRedisScript<>();
        updatePostScript.setScriptText(updateScript);
        updatePostScript.setResultType(Long.class);
        UPDATE_POST_SCRIPT = updatePostScript;

        // 3. 삭제 스크립트: ID 매칭 후 제거 및 반환
        String deleteScript =
                "local list = redis.call('LRANGE', KEYS[1], 0, -1) " +
                "for i, item in ipairs(list) do " +
                "    local decoded = cjson.decode(item) " +
                "    if decoded.id == tonumber(ARGV[1]) then " +
                "        redis.call('LREM', KEYS[1], 1, item) " +
                "        return item " +
                "    end " +
                "end " +
                "return nil";

        DefaultRedisScript<String> deletePostScript = new DefaultRedisScript<>();
        deletePostScript.setScriptText(deleteScript);
        deletePostScript.setResultType(String.class);
        DELETE_POST_SCRIPT = deletePostScript;
    }

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
                PostSimpleDetail post = deserialize(raw.toString());
                if (post != null) {
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
     *
     * @param post 추가할 게시글
     */
    public void addNewPost(PostSimpleDetail post) {
        String json = serialize(post);
        if (json == null) {
            return;
        }

        redisTemplate.execute(
                CREATE_POST_SCRIPT,
                List.of(FIRST_PAGE_LIST_KEY),
                json
        );
        log.debug("[FIRST_PAGE_CACHE] 게시글 추가: postId={}", post.getId());
    }

    /**
     * <h3>게시글 수정</h3>
     * <p>List에서 해당 ID의 게시글을 찾아 교체</p>
     *
     * @param postId 수정할 게시글 ID
     * @param post   수정된 게시글 데이터
     */
    public void updatePost(Long postId, PostSimpleDetail post) {
        String json = serialize(post);
        if (json == null) {
            return;
        }

        Long result = redisTemplate.execute(
                UPDATE_POST_SCRIPT,
                List.of(FIRST_PAGE_LIST_KEY),
                postId.toString(),
                json
        );
        boolean updated = result == 1L;
        if (updated) {
            log.debug("[FIRST_PAGE_CACHE] 게시글 수정: postId={}", postId);
        }
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>List에서 해당 ID의 게시글 제거 후, 21번째 게시글을 DB에서 가져와 추가</p>
     *
     * @param postId   삭제할 게시글 ID
     * @param fallback 21번째 게시글 조회 콜백 (삭제 후 빈 자리 채움)
     */
    public void deletePost(Long postId, Supplier<PostSimpleDetail> fallback) {
        // 1. Lua 스크립트로 삭제
        redisTemplate.execute(
                DELETE_POST_SCRIPT,
                List.of(FIRST_PAGE_LIST_KEY),
                postId.toString()
        );

        log.debug("[FIRST_PAGE_CACHE] 게시글 삭제: postId={}", postId);

        // 2. 21번째 게시글 추가 (빈 자리 채움)
        PostSimpleDetail nextPost = fallback.get();
        if (nextPost != null) {
            String nextJson = serialize(nextPost);
            if (nextJson != null) {
                redisTemplate.opsForList().rightPush(FIRST_PAGE_LIST_KEY, nextJson);
                log.debug("[FIRST_PAGE_CACHE] 21번째 게시글 추가: postId={}", nextPost.getId());
            }
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

        // 직렬화
        List<String> jsonList = new ArrayList<>(posts.size());
        for (PostSimpleDetail post : posts) {
            String json = serialize(post);
            if (json != null) {
                jsonList.add(json);
            }
        }

        if (jsonList.isEmpty()) {
            log.warn("[FIRST_PAGE_CACHE] 직렬화된 게시글이 없습니다");
            return;
        }

        // DEL → RPUSH → EXPIRE
        redisTemplate.delete(FIRST_PAGE_LIST_KEY);
        redisTemplate.opsForList().rightPushAll(FIRST_PAGE_LIST_KEY, jsonList.toArray());
        redisTemplate.expire(FIRST_PAGE_LIST_KEY, FIRST_PAGE_CACHE_TTL.toSeconds(), TimeUnit.SECONDS);

        log.info("[FIRST_PAGE_CACHE] 캐시 갱신 완료: {}개", jsonList.size());
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

    // ===================== 직렬화 유틸리티 =====================

    private String serialize(PostSimpleDetail post) {
        try {
            return objectMapper.writeValueAsString(post);
        } catch (JsonProcessingException e) {
            log.error("[FIRST_PAGE_CACHE] 직렬화 실패: postId={}, error={}", post.getId(), e.getMessage());
            return null;
        }
    }

    private PostSimpleDetail deserialize(String json) {
        try {
            return objectMapper.readValue(json, PostSimpleDetail.class);
        } catch (JsonProcessingException e) {
            log.error("[FIRST_PAGE_CACHE] 역직렬화 실패: error={}", e.getMessage());
            return null;
        }
    }
}
