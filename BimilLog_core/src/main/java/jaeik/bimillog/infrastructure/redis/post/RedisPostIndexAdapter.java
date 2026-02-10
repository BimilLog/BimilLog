package jaeik.bimillog.infrastructure.redis.post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>Redis List 인덱스 어댑터</h2>
 * <p>주간/레전드/공지 인기글 ID를 List로 관리합니다.</p>
 * <p>글 단위 Hash(post:simple:{postId})의 인덱스 역할을 합니다.</p>
 * <p>List를 사용하여 주간/레전드는 인기순, 공지는 최신순 등 순서를 보장합니다.</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostIndexAdapter {
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * DEL + RPUSH + EXPIRE를 원자적으로 수행하는 Lua 스크립트.
     * 기존 List를 삭제하고 순서를 유지하며 새 요소를 추가한 뒤 TTL을 설정합니다.
     */
    private static final String REPLACE_INDEX_SCRIPT =
            "redis.call('DEL', KEYS[1]) " +
            "for i = 1, #ARGV - 1 do " +
            "    redis.call('RPUSH', KEYS[1], ARGV[i]) " +
            "end " +
            "if tonumber(ARGV[#ARGV]) > 0 then " +
            "    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[#ARGV])) " +
            "end " +
            "return redis.call('LLEN', KEYS[1])";

    /**
     * <h3>인덱스 교체 (원자적)</h3>
     * <p>DEL + RPUSH + EXPIRE를 Lua 스크립트로 원자적으로 수행합니다.</p>
     * <p>입력 순서가 그대로 보존됩니다.</p>
     *
     * @param key     Redis List 키
     * @param postIds 새로운 게시글 ID 목록 (순서 보존)
     * @param ttl     캐시 TTL (null이면 영구 저장)
     */
    public void replaceIndex(String key, List<Long> postIds, Duration ttl) {
        if (postIds == null || postIds.isEmpty()) {
            stringRedisTemplate.delete(key);
            return;
        }

        String[] args = new String[postIds.size() + 1];
        for (int i = 0; i < postIds.size(); i++) {
            args[i] = postIds.get(i).toString();
        }
        args[postIds.size()] = ttl != null ? String.valueOf(ttl.getSeconds()) : "0";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(REPLACE_INDEX_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, List.of(key), (Object[]) args);

        log.debug("[INDEX] 교체: key={}, count={}, ttl={}", key, postIds.size(), ttl);
    }

    /**
     * <h3>인덱스 목록 조회 (LRANGE)</h3>
     * <p>저장된 순서 그대로 반환합니다.</p>
     *
     * @param key Redis List 키
     * @return 게시글 ID List (순서 보존)
     */
    public List<Long> getIndexList(String key) {
        List<String> members = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        return members.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * <h3>인덱스 맨 앞에 추가 (LPUSH)</h3>
     * <p>공지사항 등 최신 글이 앞에 오도록 LPUSH를 사용합니다.</p>
     *
     * @param key    Redis List 키
     * @param postId 추가할 게시글 ID
     */
    public void addToIndex(String key, Long postId) {
        stringRedisTemplate.opsForList().leftPush(key, postId.toString());
    }

    /**
     * <h3>인덱스에서 제거 (LREM)</h3>
     *
     * @param key    Redis List 키
     * @param postId 제거할 게시글 ID
     */
    public void removeFromIndex(String key, Long postId) {
        stringRedisTemplate.opsForList().remove(key, 1, postId.toString());
    }
}
