package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheEntry;
import jaeik.bimillog.domain.post.entity.PostCountCache;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>게시글 카운터 캐시 Redis 어댑터</h2>
 * <p>단일 Hash {@code post:counters}에 모든 캐시글의 카운터를 절대값으로 관리합니다.</p>
 * <p>카테고리별 Set 5개로 캐시글 여부를 판단합니다. (Lua 스크립트로 한 번에 체크)</p>
 * <ul>
 *   <li>카운터 구조: {@code HSET post:counters {postId}:like 42}, {@code {postId}:comment 7}, {@code {postId}:view 1580}</li>
 *   <li>첫페이지 SET: {@code post:cached:firstpage:ids}</li>
 *   <li>주간 SET: {@code post:cached:weekly:ids}</li>
 *   <li>레전드 SET: {@code post:cached:legend:ids}</li>
 *   <li>공지 SET: {@code post:cached:notice:ids}</li>
 *   <li>실시간 SET: {@code post:cached:realtime:ids}</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.9.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostCounterAdapter {
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
     * Lua: 5개 SET 중 하나라도 SISMEMBER=1이면 true
     */
    private static final String IS_CACHED_POST_SCRIPT =
            "for i = 1, #KEYS do " +
            "    if redis.call('SISMEMBER', KEYS[i], ARGV[1]) == 1 then return 1 end " +
            "end " +
            "return 0";

    /**
     * Lua: 5개 SET 모두에서 SREM
     */
    private static final String REMOVE_FROM_ALL_SETS_SCRIPT =
            "for i = 1, #KEYS do " +
            "    redis.call('SREM', KEYS[i], ARGV[1]) " +
            "end " +
            "return 1";

    // ==================== 카운터 Hash ====================

    /**
     * <h3>카운터 절대값 일괄 설정 (24시간 스케줄러용)</h3>
     * <p>게시글 목록의 viewCount/likeCount/commentCount를 Hash에 절대값으로 SET합니다.</p>
     *
     * @param posts 캐시할 게시글 목록
     */
    public void batchSetCounters(List<PostSimpleDetail> posts) {
        if (posts.isEmpty()) return;

        Map<String, String> entries = new HashMap<>(posts.size() * 3);
        for (PostSimpleDetail post : posts) {
            String prefix = post.getId().toString();
            entries.put(prefix + RedisKey.COUNTER_SUFFIX_VIEW, String.valueOf(post.getViewCount()));
            entries.put(prefix + RedisKey.COUNTER_SUFFIX_LIKE, String.valueOf(post.getLikeCount()));
            entries.put(prefix + RedisKey.COUNTER_SUFFIX_COMMENT, String.valueOf(post.getCommentCount()));
        }

        stringRedisTemplate.opsForHash().putAll(RedisKey.POST_COUNTERS_KEY, entries);
    }

    /**
     * <h3>특정 카운터 증감 (추천/댓글용)</h3>
     * <p>HINCRBY로 특정 게시글의 카운터를 증감합니다.</p>
     *
     * @param postId 게시글 ID
     * @param suffix 카운터 접미사 ({@link RedisKey#COUNTER_SUFFIX_LIKE}, {@link RedisKey#COUNTER_SUFFIX_COMMENT})
     * @param delta  증감값 (양수: 증가, 음수: 감소)
     */
    public void incrementCounter(Long postId, String suffix, long delta) {
        stringRedisTemplate.opsForHash()
                .increment(RedisKey.POST_COUNTERS_KEY, postId + suffix, delta);
    }

    /**
     * <h3>카운터 일괄 증감 (1분 조회수 플러시용)</h3>
     * <p>HINCRBY를 일괄로 수행하여 여러 게시글의 카운터를 증감합니다.</p>
     *
     * @param counts 게시글 ID -> 증감값 맵
     * @param suffix 카운터 접미사 ({@link RedisKey#COUNTER_SUFFIX_VIEW})
     */
    public void batchIncrementCounter(Map<Long, Long> counts, String suffix) {
        if (counts.isEmpty()) return;

        counts.forEach((postId, delta) ->
                stringRedisTemplate.opsForHash()
                        .increment(RedisKey.POST_COUNTERS_KEY, postId + suffix, delta));
    }

    // ==================== 카테고리별 SET ====================

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
     * <h3>카테고리 SET에 ID 일괄 추가</h3>
     *
     * @param categoryKey 카테고리 SET 키
     * @param postIds     추가할 게시글 ID 목록
     */
    public void addToCategorySet(String categoryKey, Collection<Long> postIds) {
        if (postIds.isEmpty()) return;

        String[] ids = postIds.stream().map(String::valueOf).toArray(String[]::new);
        stringRedisTemplate.opsForSet().add(categoryKey, ids);
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

    // ==================== 카운터 Hash 필드 제거 ====================

    /**
     * <h3>카운터 Hash 필드 제거</h3>
     * <p>HDEL로 특정 게시글의 view/like/comment 카운터 필드를 제거합니다.</p>
     * <p>어떤 캐시에도 속하지 않는 글의 카운터를 정리할 때 사용합니다.</p>
     *
     * @param postId 카운터를 제거할 게시글 ID
     */
    public void removeCounterFields(Long postId) {
        String prefix = postId.toString();
        stringRedisTemplate.opsForHash().delete(RedisKey.POST_COUNTERS_KEY,
                prefix + RedisKey.COUNTER_SUFFIX_VIEW,
                prefix + RedisKey.COUNTER_SUFFIX_LIKE,
                prefix + RedisKey.COUNTER_SUFFIX_COMMENT);
    }

    // ==================== 카운터 결합 (조회 시점) ====================

    /**
     * <h3>PostCacheEntry + HMGET 카운터 → PostSimpleDetail 결합</h3>
     * <p>JSON LIST에서 조회한 정적 데이터(PostCacheEntry)와 카운터 Hash의 카운트를 결합합니다.</p>
     * <p>Redis 장애 시 카운트 0으로 반환합니다.</p>
     *
     * @param entries JSON LIST에서 조회한 캐시 엔트리 목록
     * @return 카운트가 결합된 PostSimpleDetail 목록
     */
    public List<PostSimpleDetail> combineWithCounters(List<PostCacheEntry> entries) {
        if (entries.isEmpty()) return List.of();

        try {
            List<Object> hashKeys = new ArrayList<>(entries.size() * 3);
            for (PostCacheEntry entry : entries) {
                String prefix = entry.id().toString();
                hashKeys.add(prefix + RedisKey.COUNTER_SUFFIX_VIEW);
                hashKeys.add(prefix + RedisKey.COUNTER_SUFFIX_LIKE);
                hashKeys.add(prefix + RedisKey.COUNTER_SUFFIX_COMMENT);
            }

            List<Object> values = stringRedisTemplate.opsForHash()
                    .multiGet(RedisKey.POST_COUNTERS_KEY, hashKeys);

            List<PostSimpleDetail> result = new ArrayList<>(entries.size());
            for (int i = 0; i < entries.size(); i++) {
                int base = i * 3;
                PostCountCache counts = new PostCountCache(
                        parseIntOrZero(values.get(base)),
                        parseIntOrZero(values.get(base + 1)),
                        parseIntOrZero(values.get(base + 2))
                );
                result.add(entries.get(i).toPostSimpleDetail(counts));
            }
            return result;
        } catch (Exception e) {
            log.warn("[COUNTER_COMBINE] 카운터 결합 실패, 카운트 0으로 반환: {}", e.getMessage());
            return entries.stream()
                    .map(entry -> entry.toPostSimpleDetail(PostCountCache.ZERO))
                    .toList();
        }
    }

    private int parseIntOrZero(Object value) {
        return value != null ? Integer.parseInt(value.toString()) : 0;
    }

    // ==================== 캐시글 여부 확인 ====================

    /**
     * <h3>캐시글 여부 확인</h3>
     * <p>Lua 스크립트로 5개 카테고리 SET(첫페이지/주간/레전드/공지/실시간)을 한 번에 확인합니다.</p>
     * <p>하나라도 SISMEMBER=1이면 캐시글로 판단합니다.</p>
     *
     * @param postId 확인할 게시글 ID
     * @return 캐시글이면 true
     */
    public boolean isCachedPost(Long postId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(IS_CACHED_POST_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script, RedisKey.ALL_CACHED_CATEGORY_KEYS, postId.toString());
        return result != null && result == 1L;
    }
}
