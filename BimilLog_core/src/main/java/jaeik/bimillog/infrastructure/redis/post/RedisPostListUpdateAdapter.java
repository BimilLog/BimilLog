package jaeik.bimillog.infrastructure.redis.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostListUpdateAdapter {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final List<String> ALL_JSON_KEYS = List.of(
            RedisKey.FIRST_PAGE_JSON_KEY,
            RedisKey.POST_WEEKLY_JSON_KEY,
            RedisKey.POST_LEGEND_JSON_KEY,
            RedisKey.POST_NOTICE_JSON_KEY,
            RedisKey.POST_REALTIME_JSON_KEY
    );

    /**
     * <h3>특정 글의 리스트 전체 교체</h3>
     * <p>Lua 스크립트로 DEL → RPUSH(JSON들) → EXPIRE를 원자적으로 수행</p>
     */
    public void replaceList(String key, List<PostSimpleDetail> posts, Duration ttl) {
        final String REPLACE_ALL_SCRIPT =
                "redis.call('DEL', KEYS[1]) " +
                        "for i = 2, #ARGV do " +
                        "    redis.call('RPUSH', KEYS[1], ARGV[i]) " +
                        "end " +
                        "if tonumber(ARGV[1]) > 0 then " +
                        "    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) " +
                        "end";

        String ttlSeconds = ttl != null ? String.valueOf(ttl.getSeconds()) : "0";
        Object[] args = new Object[posts.size() + 1];
        args[0] = ttlSeconds;
        for (int i = 0; i < posts.size(); i++) {
            args[i + 1] = toJson(posts.get(i));
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(REPLACE_ALL_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, List.of(key), args);
    }

    /**
     * <h3>공지사항, 새 글 등록시 리스트 추가</h3>
     * <p>공지사항 리스트와 첫 페이지 리스트에 추가</p>
     * <p>Lua 스크립트로 LPUSH → LTRIM을 원자적으로 수행합니다.</p>
     */
    public void addPostToList(String key, PostSimpleDetail entry, int maxSize) {
        final String ADD_NEW_POST_SCRIPT =
                "redis.call('LPUSH', KEYS[1], ARGV[1]) " +
                "redis.call('LTRIM', KEYS[1], 0, tonumber(ARGV[2]) - 1)";
        String json = toJson(entry);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ADD_NEW_POST_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, List.of(key), json, String.valueOf(maxSize));
        log.debug("[JSON_LIST] 새 글 추가 (key={}): postId={}", key, entry.getId());
    }

    /**
     * <h3>글 수정 시 모든 캐시 제목 업데이트</h3>
     * <p>전체 JSON LIST를 순회하여 id 매칭 후 제목을 교체합니다.</p>
     */
    public void updateTitle(Long postId, String title) {
        final String UPDATE_TITLE_SCRIPT =
                "local items = redis.call('LRANGE', KEYS[1], 0, -1) " +
                        "for i, item in ipairs(items) do " +
                        "    local data = cjson.decode(item) " +
                        "    if tostring(data.id) == ARGV[1] then " +
                        "        data.title = ARGV[2] " +
                        "        redis.call('LSET', KEYS[1], i-1, cjson.encode(data)) " +
                        "        return 1 " +
                        "    end " +
                        "end " +
                        "return 0";
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UPDATE_TITLE_SCRIPT, Long.class);
        String postIdStr = postId.toString();
        for (String key : ALL_JSON_KEYS) {
            stringRedisTemplate.execute(script, List.of(key), postIdStr, title);
        }
        log.debug("[JSON_LIST] 제목 업데이트: postId={}", postId);
    }

    /**
     * <h3>모든 LIST의 댓글 수 추천수 댓글수 증가</h3>
     * <p>5개 리스트 전체에서 해당 postId의 field 값을 delta만큼 증분합니다.</p>
     * <p>리스트에 없는 글은 무시됩니다.</p>
     * <p>조회수는 1분마다 스케줄러에서 호출</p>
     *
     * @param postId 게시글 ID
     * @param field  JSON 필드명 ("viewCount", "likeCount", "commentCount")
     * @param viewCount  증감값
     */
    public void incrementCounterInAllLists(Long postId, String field, long viewCount) {
        final String INCREMENT_COUNTER_SCRIPT =
                "local items = redis.call('LRANGE', KEYS[1], 0, -1) " +
                        "for i, item in ipairs(items) do " +
                        "    local data = cjson.decode(item) " +
                        "    if tostring(data.id) == ARGV[1] then " +
                        "        data[ARGV[2]] = (data[ARGV[2]] or 0) + tonumber(ARGV[3]) " +
                        "        redis.call('LSET', KEYS[1], i-1, cjson.encode(data)) " +
                        "        return 1 " +
                        "    end " +
                        "end " +
                        "return 0";
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCREMENT_COUNTER_SCRIPT, Long.class);
        String postIdStr = postId.toString();
        String viewCountStr = String.valueOf(viewCount);
        for (String key : ALL_JSON_KEYS) {
            stringRedisTemplate.execute(script, List.of(key), postIdStr, field, viewCountStr);
        }
    }

    private String toJson(PostSimpleDetail entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("[JSON_LIST] JSON 직렬화 실패: postId=" + entry.getId(), e);
        }
    }
}
