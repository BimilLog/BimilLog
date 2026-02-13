package jaeik.bimillog.infrastructure.redis.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jaeik.bimillog.domain.post.entity.PostCacheEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <h2>JSON LIST 범용 Redis 어댑터</h2>
 * <p>게시글 캐시를 LIST에 JSON 문자열로 직접 저장합니다.</p>
 * <p>카운트 필드(조회수/추천수/댓글수)를 제외한 정적 필드만 저장합니다.</p>
 * <p>모든 메서드에 key 파라미터를 받아 다양한 캐시(첫페이지, 주간, 레전드, 공지, 실시간)에 범용적으로 사용합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Component
@Slf4j
public class RedisPostJsonListAdapter {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Lua #1 — 제목 업데이트: LIST를 순회하여 id 매칭 → 제목 교체 → LSET
     */
    private static final String UPDATE_TITLE_SCRIPT =
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

    /**
     * Lua #2 — 삭제 + 마지막 요소 ID 반환: LIST에서 id 매칭 → LREM → 마지막 요소 id 반환
     */
    private static final String REMOVE_POST_SCRIPT =
            "local items = redis.call('LRANGE', KEYS[1], 0, -1) " +
            "for i, item in ipairs(items) do " +
            "    local data = cjson.decode(item) " +
            "    if tostring(data.id) == ARGV[1] then " +
            "        redis.call('LREM', KEYS[1], 1, item) " +
            "        local last = redis.call('LINDEX', KEYS[1], -1) " +
            "        if last then " +
            "            local lastData = cjson.decode(last) " +
            "            return tostring(lastData.id) " +
            "        end " +
            "        return nil " +
            "    end " +
            "end " +
            "return nil";

    /**
     * Lua — 새 글 추가: LLEN ≥ maxSize면 마지막 요소 ID 반환 → LPUSH → LTRIM
     */
    private static final String ADD_NEW_POST_SCRIPT =
            "local maxSize = tonumber(ARGV[2]) " +
            "local removedId = nil " +
            "if redis.call('LLEN', KEYS[1]) >= maxSize then " +
            "    local json = redis.call('LINDEX', KEYS[1], -1) " +
            "    if json then " +
            "        removedId = tostring(cjson.decode(json).id) " +
            "    end " +
            "end " +
            "redis.call('LPUSH', KEYS[1], ARGV[1]) " +
            "redis.call('LTRIM', KEYS[1], 0, maxSize - 1) " +
            "return removedId";

    /**
     * Lua — 전체 교체: DEL → RPUSH(JSON들) → EXPIRE
     */
    private static final String REPLACE_ALL_SCRIPT =
            "redis.call('DEL', KEYS[1]) " +
            "for i = 1, #ARGV - 1 do " +
            "    redis.call('RPUSH', KEYS[1], ARGV[i]) " +
            "end " +
            "if tonumber(ARGV[#ARGV]) > 0 then " +
            "    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[#ARGV])) " +
            "end " +
            "return redis.call('LLEN', KEYS[1])";

    public RedisPostJsonListAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * <h3>전체 조회</h3>
     * <p>LRANGE 0 -1 → JSON 파싱하여 PostCacheEntry 리스트 반환</p>
     */
    public List<PostCacheEntry> getAll(String key) {
        List<String> jsonList = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (jsonList == null || jsonList.isEmpty()) {
            return Collections.emptyList();
        }

        List<PostCacheEntry> result = new ArrayList<>(jsonList.size());
        for (String json : jsonList) {
            try {
                result.add(objectMapper.readValue(json, PostCacheEntry.class));
            } catch (JsonProcessingException e) {
                log.warn("[JSON_LIST] JSON 파싱 실패 (key={}): {}", key, e.getMessage());
            }
        }
        return result;
    }

    /**
     * <h3>전체 교체 (스케줄러용)</h3>
     * <p>Lua 스크립트로 DEL → RPUSH(JSON들) → EXPIRE를 원자적으로 수행</p>
     */
    public void replaceAll(String key, List<PostCacheEntry> entries, Duration ttl) {
        if (entries == null || entries.isEmpty()) {
            stringRedisTemplate.delete(key);
            return;
        }

        String[] args = new String[entries.size() + 1];
        for (int i = 0; i < entries.size(); i++) {
            args[i] = toJson(entries.get(i));
        }
        args[entries.size()] = ttl != null ? String.valueOf(ttl.getSeconds()) : "0";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(REPLACE_ALL_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, List.of(key), (Object[]) args);

        log.debug("[JSON_LIST] 전체 교체 (key={}): {}개", key, entries.size());
    }

    /**
     * <h3>새 글 추가 (원자적)</h3>
     * <p>Lua 스크립트로 LLEN → LINDEX(마지막) → LPUSH → LTRIM을 원자적으로 수행합니다.</p>
     * <p>LIST가 maxSize 이상이면 잘려나간 글의 ID를 반환합니다.</p>
     *
     * @return 잘려나간 글의 postId (없으면 null)
     */
    public Long addNewPost(String key, PostCacheEntry entry, int maxSize) {
        String json = toJson(entry);
        DefaultRedisScript<String> script = new DefaultRedisScript<>(ADD_NEW_POST_SCRIPT, String.class);
        String removedId = stringRedisTemplate.execute(script, List.of(key), json, String.valueOf(maxSize));

        log.debug("[JSON_LIST] 새 글 추가 (key={}): postId={}, removedId={}", key, entry.id(), removedId);
        return Long.parseLong(removedId);
    }

    /**
     * <h3>제목 업데이트</h3>
     * <p>Lua #1: LIST를 순회하여 id 매칭 후 제목 교체</p>
     */
    public void updateTitle(String key, Long postId, String title) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UPDATE_TITLE_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, List.of(key), postId.toString(), title);

        log.debug("[JSON_LIST] 제목 업데이트 (key={}): postId={}", key, postId);
    }

    /**
     * <h3>글 삭제</h3>
     * <p>Lua #2: LIST에서 id 매칭 → LREM 후 마지막 요소의 id 반환 (보충용)</p>
     *
     * @return 삭제 후 마지막 요소의 postId (보충이 필요한 경우), 없으면 null
     */
    public Long removePost(String key, Long postId) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>(REMOVE_POST_SCRIPT, String.class);
        String result = stringRedisTemplate.execute(script, List.of(key), postId.toString());

        log.debug("[JSON_LIST] 글 삭제 (key={}): postId={}, lastId={}", key, postId, result);
        return Long.parseLong(result);
    }

    /**
     * <h3>삭제 후 보충</h3>
     * <p>RPUSH(JSON) — LIST 크기가 maxSize 미만일 때만 뒤에 추가</p>
     */
    public void appendPost(String key, PostCacheEntry entry, int maxSize) {
        Long currentSize = stringRedisTemplate.opsForList().size(key);
        if (currentSize != null && currentSize < maxSize) {
            String json = toJson(entry);
            stringRedisTemplate.opsForList().rightPush(key, json);

            log.debug("[JSON_LIST] 보충 추가 (key={}): postId={}", key, entry.id());
        }
    }

    private String toJson(PostCacheEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("[JSON_LIST] JSON 직렬화 실패: postId=" + entry.id(), e);
        }
    }
}
