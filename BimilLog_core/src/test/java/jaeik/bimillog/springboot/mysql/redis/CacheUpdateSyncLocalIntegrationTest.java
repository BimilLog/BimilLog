package jaeik.bimillog.springboot.mysql.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.post.async.CacheUpdateSync;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CacheUpdateSync 로컬 통합 테스트</h2>
 * <p>실제 Redis에서 글 작성/수정/삭제 시 JSON LIST 캐시가 올바르게 갱신되는지 검증합니다.</p>
 * <p>실행 전 MySQL(bimillogTest) + Redis(6379) 필요</p>
 */
@Tag("local-integration")
@DisplayName("CacheUpdateSync 로컬 통합 테스트")
@SpringBootTest
@ActiveProfiles("local-integration")
class CacheUpdateSyncLocalIntegrationTest {

    @Autowired
    private CacheUpdateSync cacheUpdateSync;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long TEST_POST_ID = 888888L;
    private static final Long OTHER_POST_ID = 888889L;

    @AfterEach
    void cleanRedis() {
        stringRedisTemplate.delete(RedisKey.FIRST_PAGE_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.POST_WEEKLY_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.POST_LEGEND_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.POST_NOTICE_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.POST_REALTIME_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.REALTIME_POST_SCORE_KEY);
    }

    // ==================== 글 추가 ====================

    @Test
    @DisplayName("asyncAddNewPost - 새 글이 첫 페이지 리스트 맨 앞에 추가됨")
    void asyncAddNewPost_shouldPushToFrontOfFirstPageList() throws Exception {
        // Given
        PostSimpleDetail post = buildPost(TEST_POST_ID, "새 글 제목");

        // When
        cacheUpdateSync.asyncAddNewPost(post);
        waitForAsync();

        // Then: 리스트 맨 앞(index 0)에 추가됨
        String raw = stringRedisTemplate.opsForList().index(RedisKey.FIRST_PAGE_JSON_KEY, 0);
        assertThat(raw).isNotNull();
        PostSimpleDetail saved = objectMapper.readValue(raw, PostSimpleDetail.class);
        assertThat(saved.getId()).isEqualTo(TEST_POST_ID);
        assertThat(saved.getTitle()).isEqualTo("새 글 제목");
    }

    @Test
    @DisplayName("asyncAddNewPost - 여러 글 추가 시 최신 글이 앞에 위치함")
    void asyncAddNewPost_shouldMaintainOrder_WhenMultiplePostsAdded() throws Exception {
        // Given
        PostSimpleDetail olderPost = buildPost(OTHER_POST_ID, "오래된 글");
        PostSimpleDetail newerPost = buildPost(TEST_POST_ID, "최신 글");

        // When: 순서대로 추가 (나중에 추가한 글이 앞으로)
        cacheUpdateSync.asyncAddNewPost(olderPost);
        waitForAsync();
        cacheUpdateSync.asyncAddNewPost(newerPost);
        waitForAsync();

        // Then: 최신 글이 index 0 (LPUSH 방식)
        String first = stringRedisTemplate.opsForList().index(RedisKey.FIRST_PAGE_JSON_KEY, 0);
        PostSimpleDetail firstPost = objectMapper.readValue(first, PostSimpleDetail.class);
        assertThat(firstPost.getId()).isEqualTo(TEST_POST_ID);

        String second = stringRedisTemplate.opsForList().index(RedisKey.FIRST_PAGE_JSON_KEY, 1);
        PostSimpleDetail secondPost = objectMapper.readValue(second, PostSimpleDetail.class);
        assertThat(secondPost.getId()).isEqualTo(OTHER_POST_ID);
    }

    @Test
    @DisplayName("asyncAddNewPost - FIRST_PAGE_SIZE+1 초과 시 LTRIM으로 자름")
    void asyncAddNewPost_shouldTrimWhenExceedsMaxSize() {
        // Given: FIRST_PAGE_SIZE + 1개 미리 채우기
        int limit = RedisKey.FIRST_PAGE_SIZE + 1;
        for (long i = 1; i <= limit; i++) {
            String json = toJsonSilent(buildPost(i, "글" + i));
            stringRedisTemplate.opsForList().leftPush(RedisKey.FIRST_PAGE_JSON_KEY, json);
        }
        assertThat(stringRedisTemplate.opsForList().size(RedisKey.FIRST_PAGE_JSON_KEY)).isEqualTo(limit);

        // When: 한 개 더 추가
        cacheUpdateSync.asyncAddNewPost(buildPost(TEST_POST_ID, "초과 글"));
        waitForAsync();

        // Then: maxSize = FIRST_PAGE_SIZE + 1 개로 유지됨
        Long size = stringRedisTemplate.opsForList().size(RedisKey.FIRST_PAGE_JSON_KEY);
        assertThat(size).isEqualTo(limit);
    }

    // ==================== 글 수정 ====================

    @Test
    @DisplayName("asyncUpdatePost - 첫 페이지 리스트의 제목이 갱신됨")
    void asyncUpdatePost_shouldUpdateTitleInFirstPageList() throws Exception {
        // Given: 첫 페이지에 기존 글 저장
        PostSimpleDetail original = buildPost(TEST_POST_ID, "기존 제목");
        stringRedisTemplate.opsForList().rightPush(RedisKey.FIRST_PAGE_JSON_KEY, toJsonSilent(original));

        PostSimpleDetail updated = buildPost(TEST_POST_ID, "수정된 제목");

        // When
        cacheUpdateSync.asyncUpdatePost(TEST_POST_ID, updated);
        waitForAsync();

        // Then: 제목이 갱신됨
        String raw = stringRedisTemplate.opsForList().index(RedisKey.FIRST_PAGE_JSON_KEY, 0);
        PostSimpleDetail saved = objectMapper.readValue(raw, PostSimpleDetail.class);
        assertThat(saved.getId()).isEqualTo(TEST_POST_ID);
        assertThat(saved.getTitle()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("asyncUpdatePost - 모든 JSON LIST(주간/레전드 등)에 제목이 갱신됨")
    void asyncUpdatePost_shouldUpdateTitleInAllLists() throws Exception {
        // Given: 주간, 레전드 리스트에도 글 저장
        PostSimpleDetail original = buildPost(TEST_POST_ID, "원래 제목");
        String json = toJsonSilent(original);
        stringRedisTemplate.opsForList().rightPush(RedisKey.POST_WEEKLY_JSON_KEY, json);
        stringRedisTemplate.opsForList().rightPush(RedisKey.POST_LEGEND_JSON_KEY, json);

        PostSimpleDetail updated = buildPost(TEST_POST_ID, "갱신된 제목");

        // When
        cacheUpdateSync.asyncUpdatePost(TEST_POST_ID, updated);
        waitForAsync();

        // Then: 주간, 레전드 모두 갱신됨
        String weeklyRaw = stringRedisTemplate.opsForList().index(RedisKey.POST_WEEKLY_JSON_KEY, 0);
        assertThat(objectMapper.readValue(weeklyRaw, PostSimpleDetail.class).getTitle()).isEqualTo("갱신된 제목");

        String legendRaw = stringRedisTemplate.opsForList().index(RedisKey.POST_LEGEND_JSON_KEY, 0);
        assertThat(objectMapper.readValue(legendRaw, PostSimpleDetail.class).getTitle()).isEqualTo("갱신된 제목");
    }

    // ==================== 글 삭제 ====================

    @Test
    @DisplayName("asyncDeletePost - 첫 페이지 리스트에서 해당 글이 제거됨")
    void asyncDeletePost_shouldRemovePostFromFirstPageList() {
        // Given: 두 개의 글을 첫 페이지에 저장
        stringRedisTemplate.opsForList().rightPush(RedisKey.FIRST_PAGE_JSON_KEY, toJsonSilent(buildPost(OTHER_POST_ID, "남을 글")));
        stringRedisTemplate.opsForList().rightPush(RedisKey.FIRST_PAGE_JSON_KEY, toJsonSilent(buildPost(TEST_POST_ID, "삭제될 글")));

        // When
        cacheUpdateSync.asyncDeletePost(TEST_POST_ID);
        waitForAsync();

        // Then: TEST_POST_ID 글이 제거되고, 다른 글은 남아있음
        List<String> items = stringRedisTemplate.opsForList().range(RedisKey.FIRST_PAGE_JSON_KEY, 0, -1);
        assertThat(items).isNotNull();
        boolean deletedExists = items.stream().anyMatch(item -> item.contains("\"id\":" + TEST_POST_ID));
        boolean otherExists = items.stream().anyMatch(item -> item.contains("\"id\":" + OTHER_POST_ID));
        assertThat(deletedExists).isFalse();
        assertThat(otherExists).isTrue();
    }

    @Test
    @DisplayName("asyncDeletePost - 주간/레전드/실시간 캐시에서도 해당 글이 제거됨")
    void asyncDeletePost_shouldRemovePostFromCacheLists() {
        // Given: 여러 캐시 리스트에 글 저장
        String json = toJsonSilent(buildPost(TEST_POST_ID, "삭제될 글"));
        stringRedisTemplate.opsForList().rightPush(RedisKey.POST_WEEKLY_JSON_KEY, json);
        stringRedisTemplate.opsForList().rightPush(RedisKey.POST_LEGEND_JSON_KEY, json);
        stringRedisTemplate.opsForList().rightPush(RedisKey.POST_REALTIME_JSON_KEY, json);

        // When
        cacheUpdateSync.asyncDeletePost(TEST_POST_ID);
        waitForAsync();

        // Then: 모든 캐시에서 제거됨
        List<String> weekly = stringRedisTemplate.opsForList().range(RedisKey.POST_WEEKLY_JSON_KEY, 0, -1);
        List<String> legend = stringRedisTemplate.opsForList().range(RedisKey.POST_LEGEND_JSON_KEY, 0, -1);
        List<String> realtime = stringRedisTemplate.opsForList().range(RedisKey.POST_REALTIME_JSON_KEY, 0, -1);

        assertThat(weekly).noneMatch(item -> item.contains("\"id\":" + TEST_POST_ID));
        assertThat(legend).noneMatch(item -> item.contains("\"id\":" + TEST_POST_ID));
        assertThat(realtime).noneMatch(item -> item.contains("\"id\":" + TEST_POST_ID));
    }

    // ==================== 헬퍼 ====================

    private PostSimpleDetail buildPost(Long id, String title) {
        return PostSimpleDetail.builder()
                .id(id)
                .title(title)
                .viewCount(0)
                .likeCount(0)
                .createdAt(Instant.now())
                .memberId(null)
                .memberName("테스터")
                .commentCount(0)
                .build();
    }

    private String toJsonSilent(PostSimpleDetail post) {
        try {
            return objectMapper.writeValueAsString(post);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForAsync() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
