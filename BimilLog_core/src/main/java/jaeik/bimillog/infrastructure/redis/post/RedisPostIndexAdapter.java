package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostIndexAdapter {
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Lua: DEL + SADD 원자적 재구축
     */
    private static final String REBUILD_SET_SCRIPT =
            "redis.call('DEL', KEYS[1]) " +
                    "if #ARGV > 0 then " +
                    "    redis.call('SADD', KEYS[1], unpack(ARGV)) " +
                    "end " +
                    "return #ARGV";

    /**
     * Lua: 5개 SET 모두에서 SREM
     */
    private static final String REMOVE_FROM_ALL_SETS_SCRIPT =
            "for i = 1, #KEYS do " +
                    "    redis.call('SREM', KEYS[i], ARGV[1]) " +
                    "end " +
                    "return 1";


    /**
     * <h3>카테고리 SET 원자적 재구축 (24시간 스케줄러용)</h3>
     * <p>Lua 스크립트로 DEL + SADD를 원자적으로 수행합니다.</p>
     *
     * @param categoryKey 카테고리 SET 키 (e.g. {@link RedisKey#CACHED_WEEKLY_IDS_KEY})
     * @param postIds     새로운 게시글 ID 집합
     */
    public void rebuildCategorySet(String categoryKey, Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            stringRedisTemplate.delete(categoryKey);
            return;
        }

        String[] args = postIds.stream().map(String::valueOf).toArray(String[]::new);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(REBUILD_SET_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, List.of(categoryKey), (Object[]) args);
    }

    /**
     * <h3>카테고리 SET에 ID 단건 추가</h3>
     *
     * @param categoryKey 카테고리 SET 키
     * @param postId      추가할 게시글 ID
     */
    public void addToCategorySet(String categoryKey, Long postId) {
        stringRedisTemplate.opsForSet().add(categoryKey, postId.toString());
    }

    /**
     * <h3>카테고리 SET에서 ID 단건 제거</h3>
     *
     * @param categoryKey 카테고리 SET 키
     * @param postId      제거할 게시글 ID
     */
    public void removeFromCategorySet(String categoryKey, Long postId) {
        stringRedisTemplate.opsForSet().remove(categoryKey, postId.toString());
    }

    /**
     * <h3>카테고리 SET의 모든 ID 조회</h3>
     * <p>SMEMBERS로 SET의 모든 ID를 조회합니다.</p>
     *
     * @param categoryKey 카테고리 SET 키
     * @return 게시글 ID 집합
     */
    public Set<Long> getCategorySet(String categoryKey) {
        Set<String> ids = stringRedisTemplate.opsForSet().members(categoryKey);
        if (ids == null || ids.isEmpty()) return Set.of();
        return ids.stream().map(Long::parseLong).collect(Collectors.toSet());
    }

    /**
     * <h3>모든 카테고리 SET에서 ID 제거</h3>
     * <p>Lua 스크립트로 5개 SET 모두에서 SREM을 원자적으로 수행합니다.</p>
     * <p>글 삭제 시 사용합니다.</p>
     *
     * @param postId 제거할 게시글 ID
     */
    public void removeFromAllCategorySets(Long postId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(REMOVE_FROM_ALL_SETS_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, RedisKey.ALL_CACHED_CATEGORY_KEYS, postId.toString());
    }
}
