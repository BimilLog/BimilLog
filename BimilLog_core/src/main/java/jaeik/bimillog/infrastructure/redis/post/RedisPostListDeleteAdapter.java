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
@RequiredArgsConstructor
public class RedisPostListDeleteAdapter {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String NOTICE_KEY = RedisKey.POST_NOTICE_JSON_KEY;

    private static final List<String> CACHE_KEYS = List.of(
            RedisKey.POST_WEEKLY_JSON_KEY,
            RedisKey.POST_LEGEND_JSON_KEY,
            RedisKey.POST_REALTIME_JSON_KEY
    );

    /**
     * <h3>공지사항 캐시 해제</h3>
     * <p>LIST에서 id 매칭 후 LREM으로 제거합니다.</p>
     */
    public void removePost(Long postId) {
        List<String> items = stringRedisTemplate.opsForList().range(NOTICE_KEY, 0, -1);
        if (items == null) return;
        String postIdStr = postId.toString();
        for (String item : items) {
            try {
                if (postIdStr.equals(String.valueOf(objectMapper.readValue(item, PostSimpleDetail.class).getId()))) {
                    stringRedisTemplate.opsForList().remove(NOTICE_KEY, 1, item);
                    break;
                }
            } catch (JsonProcessingException e) {
                log.warn("[JSON_LIST] JSON 파싱 실패 (key={}): {}", NOTICE_KEY, e.getMessage());
            }
        }
        log.debug("[JSON_LIST] 글 삭제 (key={}): postId={}", NOTICE_KEY, postId);
    }

    /**
     * <h3>글 삭제 시 캐시 일괄 제거</h3>
     * <p>주간/레전드/실시간 리스트에서 해당 게시글을 제거합니다.</p>
     * <p>공지는 공지 해제 후 별도 삭제, 첫 페이지는 보충 로직이 필요하므로 별도 처리합니다.</p>
     */
    public void removePostFromCacheLists(Long postId) {
        String postIdStr = postId.toString();
        for (String key : CACHE_KEYS) {
            List<String> items = stringRedisTemplate.opsForList().range(key, 0, -1);
            if (items == null) continue;
            for (String item : items) {
                try {
                    if (postIdStr.equals(String.valueOf(objectMapper.readValue(item, PostSimpleDetail.class).getId()))) {
                        stringRedisTemplate.opsForList().remove(key, 1, item);
                        break;
                    }
                } catch (JsonProcessingException e) {
                    log.warn("[JSON_LIST] JSON 파싱 실패 (key={}): {}", key, e.getMessage());
                }
            }
        }
        log.debug("[JSON_LIST] 캐시 일괄 삭제: postId={}", postId);
    }

    /**
     * <h3>첫 페이지 글 삭제 후 보충용 마지막 ID 반환</h3>
     * <p>LIST에서 id 매칭 → LREM 후 마지막 요소의 id를 반환합니다.</p>
     *
     * @return 삭제 후 마지막 요소의 postId, 없으면 null
     */
    public Long removePostAndGetLastId(String key, Long postId) {
        final String REMOVE_AND_GET_LAST_SCRIPT =
                "local items = redis.call('LRANGE', KEYS[1], 0, -1) " +
                        "for i, item in ipairs(items) do " +
                        "    local data = cjson.decode(item) " +
                        "    if tostring(data.id) == ARGV[1] then " +
                        "        redis.call('LREM', KEYS[1], 1, item) " +
                        "        local last = redis.call('LINDEX', KEYS[1], -1) " +
                        "        if last then " +
                        "            return tostring(cjson.decode(last).id) " +
                        "        end " +
                        "        return nil " +
                        "    end " +
                        "end " +
                        "return nil";
        DefaultRedisScript<String> script = new DefaultRedisScript<>(REMOVE_AND_GET_LAST_SCRIPT, String.class);
        String result = stringRedisTemplate.execute(script, List.of(key), postId.toString());
        log.debug("[JSON_LIST] 첫 페이지 글 삭제 (key={}): postId={}, lastId={}", key, postId, result);
        return result != null ? Long.parseLong(result) : null;
    }

    /**
     * <h3>삭제 후 보충</h3>
     * <p>RPUSH(JSON) — LIST 크기가 maxSize 미만일 때만 뒤에 추가</p>
     */
    public void appendPost(String key, PostSimpleDetail entry, int maxSize) {
        Long currentSize = stringRedisTemplate.opsForList().size(key);
        if (currentSize != null && currentSize < maxSize) {
            String json = toJson(entry);
            stringRedisTemplate.opsForList().rightPush(key, json);

            log.debug("[JSON_LIST] 보충 추가 (key={}): postId={}", key, entry.getId());
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
