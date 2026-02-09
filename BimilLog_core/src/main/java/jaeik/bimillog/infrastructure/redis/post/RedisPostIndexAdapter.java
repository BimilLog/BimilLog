package jaeik.bimillog.infrastructure.redis.post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>Redis SET 인덱스 어댑터</h2>
 * <p>주간/레전드/공지 인기글 ID를 SET으로 관리합니다.</p>
 * <p>글 단위 Hash(post:simple:{postId})의 인덱스 역할을 합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostIndexAdapter {
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * DEL + SADD + EXPIRE를 원자적으로 수행하는 Lua 스크립트.
     * 기존 SET을 삭제하고 새 멤버를 추가한 뒤 TTL을 설정합니다.
     */
    private static final String REPLACE_INDEX_SCRIPT =
            "redis.call('DEL', KEYS[1]) " +
            "for i = 1, #ARGV - 1 do " +
            "    redis.call('SADD', KEYS[1], ARGV[i]) " +
            "end " +
            "if tonumber(ARGV[#ARGV]) > 0 then " +
            "    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[#ARGV])) " +
            "end " +
            "return redis.call('SCARD', KEYS[1])";

    /**
     * <h3>인덱스 교체 (원자적)</h3>
     * <p>DEL + SADD + EXPIRE를 Lua 스크립트로 원자적으로 수행합니다.</p>
     *
     * @param key     Redis SET 키
     * @param postIds 새로운 게시글 ID 목록
     * @param ttl     캐시 TTL (null이면 영구 저장)
     */
    public void replaceIndex(String key, Set<Long> postIds, Duration ttl) {
        if (postIds == null || postIds.isEmpty()) {
            stringRedisTemplate.delete(key);
            return;
        }

        String[] args = new String[postIds.size() + 1];
        int i = 0;
        for (Long postId : postIds) {
            args[i++] = postId.toString();
        }
        args[i] = ttl != null ? String.valueOf(ttl.getSeconds()) : "0";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(REPLACE_INDEX_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, List.of(key), (Object[]) args);

        log.debug("[INDEX] 교체: key={}, count={}, ttl={}", key, postIds.size(), ttl);
    }

    /**
     * <h3>인덱스 멤버 조회 (SMEMBERS)</h3>
     *
     * @param key Redis SET 키
     * @return 게시글 ID Set
     */
    public Set<Long> getIndexMembers(String key) {
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        return members.stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }

    /**
     * <h3>인덱스에 추가 (SADD)</h3>
     *
     * @param key    Redis SET 키
     * @param postId 추가할 게시글 ID
     */
    public void addToIndex(String key, Long postId) {
        stringRedisTemplate.opsForSet().add(key, postId.toString());
    }

    /**
     * <h3>인덱스에서 제거 (SREM)</h3>
     *
     * @param key    Redis SET 키
     * @param postId 제거할 게시글 ID
     */
    public void removeFromIndex(String key, Long postId) {
        stringRedisTemplate.opsForSet().remove(key, postId.toString());
    }
}
