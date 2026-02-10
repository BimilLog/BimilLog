package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
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
 * <p>모든 값을 plain string으로 저장하며, 카운트는 1분 플러시 시 batchIncrementCounts로 일괄 반영합니다.</p>
 * <p>키 형식: post:simple:{postId} → HASH {id, title, likeCount, viewCount, ...}</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostHashAdapter {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = RedisKey.POST_SIMPLE_PREFIX;

    // ==================== Hash 필드명 상수 ====================

    public static final String FIELD_VIEW_COUNT = "viewCount";
    public static final String FIELD_LIKE_COUNT = "likeCount";
    public static final String FIELD_COMMENT_COUNT = "commentCount";

    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_MEMBER_ID = "memberId";
    private static final String FIELD_MEMBER_NAME = "memberName";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_IS_WEEKLY = "isWeekly";
    private static final String FIELD_IS_LEGEND = "isLegend";
    private static final String FIELD_IS_NOTICE = "isNotice";

    /**
     * <h3>글 단위 Hash 생성 (HMSET)</h3>
     * <p>PostSimpleDetail을 Map&lt;String, String&gt;으로 변환하여 저장합니다.</p>
     *
     * @param post 저장할 게시글
     */
    public void createPostHash(PostSimpleDetail post) {
        String key = PREFIX + post.getId();
        Map<String, String> hash = toStringMap(post);
        stringRedisTemplate.opsForHash().putAll(key, hash);
        stringRedisTemplate.expire(key, RedisKey.DEFAULT_CACHE_TTL);

        log.debug("[POST_HASH] 생성: postId={}", post.getId());
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
     * <h3>여러 글의 카운트 필드 일괄 증감 (Pipeline EXISTS + HINCRBY)</h3>
     * <p>존재하는 Hash만 카운트를 증감합니다.</p>
     * <p>존재하지 않는 키에 HINCRBY하면 불완전한 Hash가 생성되므로 방지합니다.</p>
     *
     * @param countsByPostId postId → 증감량 맵
     * @param field          증감할 필드명 (viewCount, likeCount, commentCount)
     */
    public void batchIncrementCounts(Map<Long, Long> countsByPostId, String field) {
        if (countsByPostId == null || countsByPostId.isEmpty()) {
            return;
        }

        List<Long> postIds = new ArrayList<>(countsByPostId.keySet());

        // 1. Pipeline EXISTS: 어떤 Hash가 존재하는지 확인
        List<Object> existsResults = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long postId : postIds) {
                String key = PREFIX + postId;
                connection.keyCommands().exists(key.getBytes());
            }
            return null;
        });

        // 2. 존재하는 Hash만 Pipeline HINCRBY
        List<Long> existingPostIds = new ArrayList<>();
        for (int i = 0; i < postIds.size(); i++) {
            if (i < existsResults.size() && Boolean.TRUE.equals(existsResults.get(i))) {
                existingPostIds.add(postIds.get(i));
            }
        }

        if (existingPostIds.isEmpty()) {
            return;
        }

        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] fieldBytes = field.getBytes();
            for (Long postId : existingPostIds) {
                String key = PREFIX + postId;
                connection.hashCommands().hIncrBy(key.getBytes(), fieldBytes, countsByPostId.get(postId));
            }
            return null;
        });

        log.debug("[POST_HASH] 일괄 카운트 반영: field={}, count={}", field, existingPostIds.size());
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
        map.put(FIELD_IS_WEEKLY, String.valueOf(post.isWeekly()));
        map.put(FIELD_IS_LEGEND, String.valueOf(post.isLegend()));
        map.put(FIELD_IS_NOTICE, String.valueOf(post.isNotice()));
        return map;
    }

    private PostSimpleDetail fromStringMap(Map<?, ?> map) {
        return PostSimpleDetail.builder()
                .id(parseLong(map.get(FIELD_ID)))
                .title(parseString(map.get(FIELD_TITLE)))
                .viewCount(parseInteger(map.get(FIELD_VIEW_COUNT)))
                .likeCount(parseInteger(map.get(FIELD_LIKE_COUNT)))
                .commentCount(parseInteger(map.get(FIELD_COMMENT_COUNT)))
                .memberId(parseLong(map.get(FIELD_MEMBER_ID)))
                .memberName(parseString(map.get(FIELD_MEMBER_NAME)))
                .createdAt(parseInstant(map.get(FIELD_CREATED_AT)))
                .isWeekly(parseBoolean(map.get(FIELD_IS_WEEKLY)))
                .isLegend(parseBoolean(map.get(FIELD_IS_LEGEND)))
                .isNotice(parseBoolean(map.get(FIELD_IS_NOTICE)))
                .build();
    }

    private static Long parseLong(Object value) {
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

    private static boolean parseBoolean(Object value) {
        if (value == null) return false;
        return Boolean.parseBoolean(value.toString());
    }
}
