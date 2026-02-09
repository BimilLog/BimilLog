package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * <h2>글 단위 Redis Hash 어댑터</h2>
 * <p>StringRedisTemplate 기반으로 글 단위 Hash를 관리합니다.</p>
 * <p>HINCRBY로 카운트를 즉시 반영할 수 있도록 모든 값을 plain string으로 저장합니다.</p>
 * <p>키 형식: post:simple:{postId} → HASH {id, title, likeCount, viewCount, ...}</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostHashAdapter {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = RedisKey.POST_SIMPLE_PREFIX;

    // ==================== Hash 필드명 상수 ====================

    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_VIEW_COUNT = "viewCount";
    public static final String FIELD_LIKE_COUNT = "likeCount";
    public static final String FIELD_COMMENT_COUNT = "commentCount";
    public static final String FIELD_MEMBER_ID = "memberId";
    public static final String FIELD_MEMBER_NAME = "memberName";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_FEATURED_TYPE = "featuredType";

    /**
     * <h3>글 단위 Hash 생성 (HMSET)</h3>
     * <p>PostSimpleDetail을 Map&lt;String, String&gt;으로 변환하여 저장합니다.</p>
     *
     * @param post 저장할 게시글
     */
    public void createPostHash(PostSimpleDetail post) {
        if (post == null || post.getId() == null) {
            return;
        }

        String key = PREFIX + post.getId();
        Map<String, String> hash = toStringMap(post);
        stringRedisTemplate.opsForHash().putAll(key, hash);
        stringRedisTemplate.expire(key, RedisKey.POST_SIMPLE_TTL);

        log.debug("[POST_HASH] 생성: postId={}", post.getId());
    }

    /**
     * <h3>글 단위 Hash 조회 (HGETALL)</h3>
     *
     * @param postId 게시글 ID
     * @return PostSimpleDetail (없으면 null)
     */
    public PostSimpleDetail getPostHash(Long postId) {
        String key = PREFIX + postId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return null;
        }
        return fromStringMap(entries);
    }

    /**
     * <h3>여러 글 Hash 조회 (Pipeline HGETALL)</h3>
     * <p>Pipeline으로 여러 글의 Hash를 한 번에 조회합니다.</p>
     * <p>존재하지 않는 글은 결과에서 제외됩니다.</p>
     *
     * @param postIds 조회할 게시글 ID 목록
     * @return PostSimpleDetail 리스트 (순서 보장 안 됨)
     */
    public List<PostSimpleDetail> getPostHashes(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long postId : postIds) {
                String key = PREFIX + postId;
                connection.hashCommands().hGetAll(key.getBytes());
            }
            return null;
        });

        List<PostSimpleDetail> posts = new ArrayList<>();
        int index = 0;
        for (Long postId : postIds) {
            if (index < results.size()) {
                Object result = results.get(index);
                if (result instanceof Map<?, ?> map && !map.isEmpty()) {
                    try {
                        posts.add(fromStringMap(map));
                    } catch (Exception e) {
                        log.warn("[POST_HASH] 변환 실패: postId={}, error={}", postId, e.getMessage());
                    }
                }
            }
            index++;
        }
        return posts;
    }

    /**
     * <h3>카운트 필드 증감 (HINCRBY)</h3>
     * <p>글 단위 Hash의 특정 카운트 필드를 원자적으로 증감시킵니다.</p>
     *
     * @param postId 게시글 ID
     * @param field  증감할 필드명 (viewCount, likeCount, commentCount)
     * @param delta  증감량 (양수: 증가, 음수: 감소)
     */
    public void incrementCount(Long postId, String field, long delta) {
        String key = PREFIX + postId;
        stringRedisTemplate.opsForHash().increment(key, field, delta);
    }

    /**
     * <h3>제목 업데이트 (HSET)</h3>
     *
     * @param postId   게시글 ID
     * @param newTitle 새 제목
     */
    public void updateTitle(Long postId, String newTitle) {
        String key = PREFIX + postId;
        stringRedisTemplate.opsForHash().put(key, FIELD_TITLE, newTitle);
    }

    /**
     * <h3>글 단위 Hash 삭제 (DEL)</h3>
     *
     * @param postId 삭제할 게시글 ID
     */
    public void deletePostHash(Long postId) {
        String key = PREFIX + postId;
        stringRedisTemplate.delete(key);
        log.debug("[POST_HASH] 삭제: postId={}", postId);
    }

    /**
     * <h3>글 단위 Hash 존재 여부 확인 (EXISTS)</h3>
     *
     * @param postId 게시글 ID
     * @return 존재하면 true
     */
    public boolean existsPostHash(Long postId) {
        String key = PREFIX + postId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    // ==================== 변환 메서드 ====================

    private Map<String, String> toStringMap(PostSimpleDetail post) {
        Map<String, String> map = new HashMap<>();
        map.put(FIELD_ID, post.getId().toString());
        map.put(FIELD_TITLE, post.getTitle() != null ? post.getTitle() : "");
        map.put(FIELD_VIEW_COUNT, String.valueOf(post.getViewCount() != null ? post.getViewCount() : 0));
        map.put(FIELD_LIKE_COUNT, String.valueOf(post.getLikeCount() != null ? post.getLikeCount() : 0));
        map.put(FIELD_COMMENT_COUNT, String.valueOf(post.getCommentCount() != null ? post.getCommentCount() : 0));
        map.put(FIELD_MEMBER_ID, post.getMemberId() != null ? post.getMemberId().toString() : "");
        map.put(FIELD_MEMBER_NAME, post.getMemberName() != null ? post.getMemberName() : "");
        map.put(FIELD_CREATED_AT, post.getCreatedAt() != null ? post.getCreatedAt().toString() : "");
        map.put(FIELD_FEATURED_TYPE, post.getFeaturedType() != null ? post.getFeaturedType().name() : "");
        return map;
    }

    private PostSimpleDetail fromStringMap(Map<?, ?> map) {
        return PostSimpleDetail.builder()
                .id(parseLong(map.get(FIELD_ID)))
                .title(parseString(map.get(FIELD_TITLE)))
                .viewCount(parseInteger(map.get(FIELD_VIEW_COUNT)))
                .likeCount(parseInteger(map.get(FIELD_LIKE_COUNT)))
                .commentCount(parseInteger(map.get(FIELD_COMMENT_COUNT)))
                .memberId(parseLongOrNull(map.get(FIELD_MEMBER_ID)))
                .memberName(parseString(map.get(FIELD_MEMBER_NAME)))
                .createdAt(parseInstant(map.get(FIELD_CREATED_AT)))
                .featuredType(parseFeaturedType(map.get(FIELD_FEATURED_TYPE)))
                .build();
    }

    private static Long parseLong(Object value) {
        if (value == null) return null;
        String s = value.toString();
        return s.isEmpty() ? null : Long.parseLong(s);
    }

    private static Long parseLongOrNull(Object value) {
        if (value == null) return null;
        String s = value.toString();
        return s.isEmpty() ? null : Long.parseLong(s);
    }

    private static Integer parseInteger(Object value) {
        if (value == null) return 0;
        String s = value.toString();
        return s.isEmpty() ? 0 : Integer.parseInt(s);
    }

    private static String parseString(Object value) {
        if (value == null) return null;
        String s = value.toString();
        return s.isEmpty() ? null : s;
    }

    private static Instant parseInstant(Object value) {
        if (value == null) return null;
        String s = value.toString();
        return s.isEmpty() ? null : Instant.parse(s);
    }

    private static PostCacheFlag parseFeaturedType(Object value) {
        if (value == null) return null;
        String s = value.toString();
        if (s.isEmpty()) return null;
        try {
            return PostCacheFlag.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
