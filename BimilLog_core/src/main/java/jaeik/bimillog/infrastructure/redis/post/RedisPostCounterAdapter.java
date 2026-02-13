package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * <h2>게시글 카운터 캐시 Redis 어댑터</h2>
 * <p>단일 Hash {@code post:counters}에 모든 캐시글의 카운터를 절대값으로 관리합니다.</p>
 * <p>Set {@code post:cached:ids}로 캐시글 여부를 판단합니다.</p>
 * <ul>
 *   <li>카운터 구조: {@code HSET post:counters {postId}:like 42}, {@code {postId}:comment 7}, {@code {postId}:view 1580}</li>
 *   <li>캐시글 SET: {@code SADD post:cached:ids {postId}} — 주간/레전드/공지/첫페이지 합집합</li>
 *   <li>실시간 캐시글: 기존 ZSet {@code post:realtime:score}으로 ZSCORE 확인</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostCounterAdapter {
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Lua: DEL + SADD 원자적 재구축
     */
    private static final String REBUILD_CACHED_IDS_SCRIPT =
            "redis.call('DEL', KEYS[1]) " +
            "if #ARGV > 0 then " +
            "    redis.call('SADD', KEYS[1], unpack(ARGV)) " +
            "end " +
            "return #ARGV";

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
     * @param counts 게시글 ID → 증감값 맵
     * @param suffix 카운터 접미사 ({@link RedisKey#COUNTER_SUFFIX_VIEW})
     */
    public void batchIncrementCounter(Map<Long, Long> counts, String suffix) {
        if (counts.isEmpty()) return;

        counts.forEach((postId, delta) ->
                stringRedisTemplate.opsForHash()
                        .increment(RedisKey.POST_COUNTERS_KEY, postId + suffix, delta));
    }

    // ==================== 캐시글 ID Set ====================

    /**
     * <h3>캐시글 ID SET 원자적 재구축 (24시간 스케줄러용)</h3>
     * <p>Lua 스크립트로 DEL + SADD를 원자적으로 수행합니다.</p>
     * <p>4개 JSON LIST(첫페이지/주간/레전드/공지)의 모든 게시글 ID를 읽어 합집합으로 구축합니다.</p>
     *
     * @param postIds 캐시글 ID 집합
     */
    public void rebuildCachedPostIds(Set<Long> postIds) {
        if (postIds.isEmpty()) {
            stringRedisTemplate.delete(RedisKey.CACHED_POST_IDS_KEY);
            return;
        }

        String[] args = postIds.stream().map(String::valueOf).toArray(String[]::new);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(REBUILD_CACHED_IDS_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, List.of(RedisKey.CACHED_POST_IDS_KEY), (Object[]) args);
    }

    /**
     * <h3>캐시글 ID 일괄 추가</h3>
     * <p>SADD로 여러 게시글 ID를 SET에 추가합니다.</p>
     *
     * @param postIds 추가할 게시글 ID 목록
     */
    public void addCachedPostIds(Collection<Long> postIds) {
        if (postIds.isEmpty()) return;

        String[] ids = postIds.stream().map(String::valueOf).toArray(String[]::new);
        stringRedisTemplate.opsForSet().add(RedisKey.CACHED_POST_IDS_KEY, ids);
    }

    /**
     * <h3>캐시글 ID 단건 추가</h3>
     * <p>SADD로 단일 게시글 ID를 SET에 추가합니다.</p>
     *
     * @param postId 추가할 게시글 ID
     */
    public void addCachedPostId(Long postId) {
        stringRedisTemplate.opsForSet().add(RedisKey.CACHED_POST_IDS_KEY, postId.toString());
    }

    /**
     * <h3>캐시글 ID 제거</h3>
     * <p>SREM으로 게시글 ID를 SET에서 제거합니다.</p>
     *
     * @param postId 제거할 게시글 ID
     */
    public void removeCachedPostId(Long postId) {
        stringRedisTemplate.opsForSet().remove(RedisKey.CACHED_POST_IDS_KEY, postId.toString());
    }

    // ==================== 카운터 병합 (조회 시점) ====================

    /**
     * <h3>HMGET으로 카운터를 조회하여 PostSimpleDetail에 병합</h3>
     * <p>게시글 ID 목록으로 Hash에서 view/like/comment 카운터를 일괄 조회(HMGET)하고,
     * null이 아닌 값만 PostSimpleDetail에 오버라이드합니다.</p>
     * <p>Hash에 값이 없으면(비캐시글 등) JSON LIST의 원래 값을 유지합니다.</p>
     * <p>Redis 장애 시에도 JSON LIST 값을 유지하여 안전하게 반환합니다.</p>
     *
     * @param posts 카운터를 병합할 게시글 목록 (mutable)
     */
    public void mergeCounters(List<PostSimpleDetail> posts) {
        if (posts.isEmpty()) return;

        try {
            List<Object> hashKeys = new ArrayList<>(posts.size() * 3);
            for (PostSimpleDetail post : posts) {
                String prefix = post.getId().toString();
                hashKeys.add(prefix + RedisKey.COUNTER_SUFFIX_VIEW);
                hashKeys.add(prefix + RedisKey.COUNTER_SUFFIX_LIKE);
                hashKeys.add(prefix + RedisKey.COUNTER_SUFFIX_COMMENT);
            }

            List<Object> values = stringRedisTemplate.opsForHash()
                    .multiGet(RedisKey.POST_COUNTERS_KEY, hashKeys);

            for (int i = 0; i < posts.size(); i++) {
                PostSimpleDetail post = posts.get(i);
                int base = i * 3;

                Object viewVal = values.get(base);
                Object likeVal = values.get(base + 1);
                Object commentVal = values.get(base + 2);

                if (viewVal != null) post.setViewCount(Integer.parseInt(viewVal.toString()));
                if (likeVal != null) post.setLikeCount(Integer.parseInt(likeVal.toString()));
                if (commentVal != null) post.setCommentCount(Integer.parseInt(commentVal.toString()));
            }
        } catch (Exception e) {
            log.warn("[COUNTER_MERGE] 카운터 병합 실패, JSON LIST 값 유지: {}", e.getMessage());
        }
    }

    // ==================== 캐시글 ID Set ====================

    /**
     * <h3>캐시글 여부 확인</h3>
     * <p>SET(SISMEMBER)과 실시간 ZSet(ZSCORE)을 순차 확인합니다.</p>
     * <p>SET에 있으면 즉시 true, 없으면 ZSet도 확인합니다.</p>
     *
     * @param postId 확인할 게시글 ID
     * @return 캐시글이면 true
     */
    public boolean isCachedPost(Long postId) {
        String id = postId.toString();

        Boolean inSet = stringRedisTemplate.opsForSet()
                .isMember(RedisKey.CACHED_POST_IDS_KEY, id);
        if (Boolean.TRUE.equals(inSet)) return true;

        Double score = stringRedisTemplate.opsForZSet()
                .score(RedisKey.REALTIME_POST_SCORE_KEY, id);
        return score != null;
    }
}
