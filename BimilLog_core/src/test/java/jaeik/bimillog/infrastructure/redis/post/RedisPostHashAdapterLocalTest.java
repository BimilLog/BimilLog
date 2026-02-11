package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisPostHashAdapter 로컬 통합 테스트</h2>
 * <p>Redis Pipeline(HGETALL, EXISTS+HINCRBY) 및 Hash CRUD 동작을 검증합니다.</p>
 * <p>실행 전 MySQL(bimillogTest) + Redis(6380) 필요</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
@DisplayName("RedisPostHashAdapter 로컬 통합 테스트")
class RedisPostHashAdapterLocalTest {

    @Autowired
    private RedisPostHashAdapter redisPostHashAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
    }

    private PostSimpleDetail createTestPost(Long id, String title, int viewCount, int likeCount, int commentCount) {
        return PostSimpleDetail.builder()
                .id(id)
                .title(title)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .memberId(1L)
                .memberName("testUser")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .isWeekly(false)
                .isLegend(false)
                .isNotice(false)
                .build();
    }

    // ==================== createPostHash ====================

    @Test
    @DisplayName("Hash 생성 - 모든 필드가 저장됨")
    void createPostHash_shouldStoreAllFields() {
        // Given
        PostSimpleDetail post = PostSimpleDetail.builder()
                .id(1L)
                .title("테스트 게시글")
                .viewCount(100)
                .likeCount(50)
                .commentCount(10)
                .memberId(42L)
                .memberName("작성자")
                .createdAt(Instant.parse("2026-01-15T12:00:00Z"))
                .isWeekly(true)
                .isLegend(false)
                .isNotice(false)
                .build();

        // When
        redisPostHashAdapter.createPostHash(post);

        // Then
        String key = RedisKey.POST_SIMPLE_PREFIX + 1;
        Map<Object, Object> hash = stringRedisTemplate.opsForHash().entries(key);
        assertThat(hash).isNotEmpty();
        assertThat(hash.get("id")).isEqualTo("1");
        assertThat(hash.get("title")).isEqualTo("테스트 게시글");
        assertThat(hash.get("viewCount")).isEqualTo("100");
        assertThat(hash.get("likeCount")).isEqualTo("50");
        assertThat(hash.get("commentCount")).isEqualTo("10");
        assertThat(hash.get("memberId")).isEqualTo("42");
        assertThat(hash.get("memberName")).isEqualTo("작성자");
        assertThat(hash.get("isWeekly")).isEqualTo("true");
    }

    // ==================== getPostHashes (Pipeline HGETALL) ====================

    @Test
    @DisplayName("Pipeline 조회 - 여러 Hash를 한 번에 조회")
    void getPostHashes_shouldReturnMultiplePosts() {
        // Given
        redisPostHashAdapter.createPostHash(createTestPost(1L, "첫번째", 10, 5, 2));
        redisPostHashAdapter.createPostHash(createTestPost(2L, "두번째", 20, 10, 4));
        redisPostHashAdapter.createPostHash(createTestPost(3L, "세번째", 30, 15, 6));

        // When
        List<PostSimpleDetail> results = redisPostHashAdapter.getPostHashes(List.of(1L, 2L, 3L));

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(PostSimpleDetail::getTitle)
                .containsExactlyInAnyOrder("첫번째", "두번째", "세번째");
    }

    @Test
    @DisplayName("Pipeline 조회 - 존재하지 않는 키는 결과에서 제외")
    void getPostHashes_shouldSkipMissingKeys() {
        // Given
        redisPostHashAdapter.createPostHash(createTestPost(1L, "존재하는글", 10, 5, 2));

        // When
        List<PostSimpleDetail> results = redisPostHashAdapter.getPostHashes(List.of(1L, 999L, 888L));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("존재하는글");
    }

    @Test
    @DisplayName("Pipeline 조회 - 빈 목록이면 빈 결과")
    void getPostHashes_emptyInput_shouldReturnEmpty() {
        // When
        List<PostSimpleDetail> results = redisPostHashAdapter.getPostHashes(List.of());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Pipeline 조회 - 필드 값이 정확히 복원됨")
    void getPostHashes_shouldRestoreFieldsCorrectly() {
        // Given
        PostSimpleDetail original = PostSimpleDetail.builder()
                .id(42L)
                .title("복원 테스트")
                .viewCount(999)
                .likeCount(100)
                .commentCount(50)
                .memberId(7L)
                .memberName("테스터")
                .createdAt(Instant.parse("2026-06-15T09:30:00Z"))
                .isWeekly(false)
                .isLegend(true)
                .isNotice(false)
                .build();
        redisPostHashAdapter.createPostHash(original);

        // When
        List<PostSimpleDetail> results = redisPostHashAdapter.getPostHashes(List.of(42L));

        // Then
        assertThat(results).hasSize(1);
        PostSimpleDetail restored = results.get(0);
        assertThat(restored.getId()).isEqualTo(42L);
        assertThat(restored.getTitle()).isEqualTo("복원 테스트");
        assertThat(restored.getViewCount()).isEqualTo(999);
        assertThat(restored.getLikeCount()).isEqualTo(100);
        assertThat(restored.getCommentCount()).isEqualTo(50);
        assertThat(restored.getMemberId()).isEqualTo(7L);
        assertThat(restored.getMemberName()).isEqualTo("테스터");
        assertThat(restored.getCreatedAt()).isEqualTo(Instant.parse("2026-06-15T09:30:00Z"));
        assertThat(restored.isLegend()).isTrue();
        assertThat(restored.isWeekly()).isFalse();
    }

    // ==================== updateTitle ====================

    @Test
    @DisplayName("제목 수정 - Hash의 title 필드만 변경")
    void updateTitle_shouldOnlyUpdateTitleField() {
        // Given
        redisPostHashAdapter.createPostHash(createTestPost(1L, "원래제목", 100, 50, 10));

        // When
        redisPostHashAdapter.updateTitle(1L, "수정된제목");

        // Then
        List<PostSimpleDetail> results = redisPostHashAdapter.getPostHashes(List.of(1L));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("수정된제목");
        assertThat(results.get(0).getViewCount()).isEqualTo(100);
    }

    // ==================== deletePostHash ====================

    @Test
    @DisplayName("Hash 삭제 - 삭제 후 조회 불가")
    void deletePostHash_shouldRemoveHash() {
        // Given
        redisPostHashAdapter.createPostHash(createTestPost(1L, "삭제될글", 10, 5, 2));

        // When
        redisPostHashAdapter.deletePostHash(1L);

        // Then
        List<PostSimpleDetail> results = redisPostHashAdapter.getPostHashes(List.of(1L));
        assertThat(results).isEmpty();
    }

    // ==================== batchIncrementCounts (Pipeline EXISTS + HINCRBY) ====================

    @Test
    @DisplayName("일괄 카운트 증가 - 존재하는 Hash의 viewCount 증가")
    void batchIncrementCounts_viewCount_shouldIncrementExistingHashes() {
        // Given
        redisPostHashAdapter.createPostHash(createTestPost(1L, "글1", 10, 5, 2));
        redisPostHashAdapter.createPostHash(createTestPost(2L, "글2", 20, 10, 4));

        Map<Long, Long> counts = Map.of(1L, 3L, 2L, 7L);

        // When
        redisPostHashAdapter.batchIncrementCounts(counts, RedisPostHashAdapter.FIELD_VIEW_COUNT);

        // Then
        List<PostSimpleDetail> results = redisPostHashAdapter.getPostHashes(List.of(1L, 2L));
        PostSimpleDetail post1 = results.stream().filter(p -> p.getId() == 1L).findFirst().orElseThrow();
        PostSimpleDetail post2 = results.stream().filter(p -> p.getId() == 2L).findFirst().orElseThrow();

        assertThat(post1.getViewCount()).isEqualTo(13); // 10 + 3
        assertThat(post2.getViewCount()).isEqualTo(27); // 20 + 7
    }

    @Test
    @DisplayName("일괄 카운트 증가 - 존재하지 않는 Hash는 건너뜀")
    void batchIncrementCounts_shouldSkipNonExistingHashes() {
        // Given
        redisPostHashAdapter.createPostHash(createTestPost(1L, "글1", 10, 5, 2));

        Map<Long, Long> counts = Map.of(1L, 5L, 999L, 10L);

        // When
        redisPostHashAdapter.batchIncrementCounts(counts, RedisPostHashAdapter.FIELD_LIKE_COUNT);

        // Then
        List<PostSimpleDetail> results = redisPostHashAdapter.getPostHashes(List.of(1L));
        assertThat(results.get(0).getLikeCount()).isEqualTo(10); // 5 + 5

        Boolean exists = stringRedisTemplate.hasKey(RedisKey.POST_SIMPLE_PREFIX + 999);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("일괄 카운트 증가 - 빈 맵이면 아무 작업 안 함")
    void batchIncrementCounts_emptyMap_shouldDoNothing() {
        // Given
        redisPostHashAdapter.createPostHash(createTestPost(1L, "글1", 10, 5, 2));

        // When
        redisPostHashAdapter.batchIncrementCounts(Collections.emptyMap(), RedisPostHashAdapter.FIELD_COMMENT_COUNT);

        // Then
        List<PostSimpleDetail> results = redisPostHashAdapter.getPostHashes(List.of(1L));
        assertThat(results.get(0).getCommentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("일괄 카운트 감소 - commentCount 음수 증감")
    void batchIncrementCounts_commentCount_shouldDecrementCorrectly() {
        // Given
        redisPostHashAdapter.createPostHash(createTestPost(1L, "글1", 10, 5, 10));

        // When
        Map<Long, Long> counts = Map.of(1L, -2L);
        redisPostHashAdapter.batchIncrementCounts(counts, RedisPostHashAdapter.FIELD_COMMENT_COUNT);

        // Then
        List<PostSimpleDetail> results = redisPostHashAdapter.getPostHashes(List.of(1L));
        assertThat(results.get(0).getCommentCount()).isEqualTo(8); // 10 - 2
    }
}
