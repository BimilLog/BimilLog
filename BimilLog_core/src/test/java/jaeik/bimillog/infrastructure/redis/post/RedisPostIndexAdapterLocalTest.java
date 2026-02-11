package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisPostIndexAdapter 로컬 통합 테스트</h2>
 * <p>Lua 스크립트(replaceIndex) 및 List 기반 인덱스 CRUD 동작을 검증합니다.</p>
 * <p>실행 전 MySQL(bimillogTest) + Redis(6380) 필요</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
@DisplayName("RedisPostIndexAdapter 로컬 통합 테스트")
class RedisPostIndexAdapterLocalTest {

    @Autowired
    private RedisPostIndexAdapter redisPostIndexAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_KEY = "test:index:ids";

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
    }

    // ==================== replaceIndex (Lua script) ====================

    @Test
    @DisplayName("인덱스 교체 - 순서 보존하며 교체")
    void replaceIndex_shouldPreserveOrder() {
        // When
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(10L, 20L, 30L), Duration.ofMinutes(5));

        // Then
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).containsExactly(10L, 20L, 30L);
    }

    @Test
    @DisplayName("인덱스 교체 - 기존 데이터 덮어쓰기")
    void replaceIndex_shouldOverwriteExisting() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(1L, 2L, 3L), Duration.ofMinutes(5));

        // When
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(100L, 200L), Duration.ofMinutes(5));

        // Then
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).containsExactly(100L, 200L);
    }

    @Test
    @DisplayName("인덱스 교체 - 빈 목록이면 키 삭제")
    void replaceIndex_emptyList_shouldDeleteKey() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(1L, 2L), Duration.ofMinutes(5));

        // When
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(), null);

        // Then
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("인덱스 교체 - null이면 키 삭제")
    void replaceIndex_nullList_shouldDeleteKey() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(1L, 2L), Duration.ofMinutes(5));

        // When
        redisPostIndexAdapter.replaceIndex(TEST_KEY, null, null);

        // Then
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).isEmpty();
    }

    // ==================== getIndexList ====================

    @Test
    @DisplayName("인덱스 조회 - 존재하지 않는 키는 빈 목록 반환")
    void getIndexList_nonExisting_shouldReturnEmpty() {
        List<Long> result = redisPostIndexAdapter.getIndexList("non:existing:key");
        assertThat(result).isEmpty();
    }

    // ==================== addToIndex (LPUSH) ====================

    @Test
    @DisplayName("인덱스 앞에 추가 - LPUSH로 맨 앞에 삽입")
    void addToIndex_shouldPrependToList() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(2L, 3L), Duration.ofMinutes(5));

        // When
        redisPostIndexAdapter.addToIndex(TEST_KEY, 1L);

        // Then
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    // ==================== removeFromIndex (LREM) ====================

    @Test
    @DisplayName("인덱스에서 제거 - 특정 요소 삭제")
    void removeFromIndex_shouldRemoveElement() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(1L, 2L, 3L), Duration.ofMinutes(5));

        // When
        redisPostIndexAdapter.removeFromIndex(TEST_KEY, 2L);

        // Then
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).containsExactly(1L, 3L);
    }

    // ==================== addToIndexWithTrim (LPUSH + LTRIM) ====================

    @Test
    @DisplayName("인덱스 추가 + 트림 - maxSize 유지")
    void addToIndexWithTrim_shouldMaintainMaxSize() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(2L, 3L, 4L), Duration.ofMinutes(5));

        // When - maxSize=3으로 1번 추가
        redisPostIndexAdapter.addToIndexWithTrim(TEST_KEY, 1L, 3);

        // Then - 맨 앞에 추가, 뒤에서 잘림 (4L 제거)
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("인덱스 추가 + 트림 - maxSize 미만이면 전부 유지")
    void addToIndexWithTrim_underMaxSize_shouldKeepAll() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(2L), Duration.ofMinutes(5));

        // When - maxSize=5로 1번 추가
        redisPostIndexAdapter.addToIndexWithTrim(TEST_KEY, 1L, 5);

        // Then
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).containsExactly(1L, 2L);
    }

    // ==================== removeFromIndexAndGetLast ====================

    @Test
    @DisplayName("인덱스 제거 후 마지막 요소 반환")
    void removeFromIndexAndGetLast_shouldReturnLastElement() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(1L, 2L, 3L), Duration.ofMinutes(5));

        // When
        Long last = redisPostIndexAdapter.removeFromIndexAndGetLast(TEST_KEY, 2L);

        // Then
        assertThat(last).isEqualTo(3L);
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("인덱스 제거 - 존재하지 않는 요소면 null 반환")
    void removeFromIndexAndGetLast_nonExisting_shouldReturnNull() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(1L, 2L), Duration.ofMinutes(5));

        // When
        Long last = redisPostIndexAdapter.removeFromIndexAndGetLast(TEST_KEY, 999L);

        // Then
        assertThat(last).isNull();
    }

    // ==================== appendToIndex (RPUSH) ====================

    @Test
    @DisplayName("인덱스 뒤에 추가 - maxSize 미만일 때 추가됨")
    void appendToIndex_shouldAppendWhenUnderMaxSize() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(1L, 2L), Duration.ofMinutes(5));

        // When
        redisPostIndexAdapter.appendToIndex(TEST_KEY, 3L, 3);

        // Then
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("인덱스 뒤에 추가 - maxSize 이상이면 추가 안 함")
    void appendToIndex_shouldNotAppendWhenAtMaxSize() {
        // Given
        redisPostIndexAdapter.replaceIndex(TEST_KEY, List.of(1L, 2L, 3L), Duration.ofMinutes(5));

        // When
        redisPostIndexAdapter.appendToIndex(TEST_KEY, 4L, 3);

        // Then
        List<Long> result = redisPostIndexAdapter.getIndexList(TEST_KEY);
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(1L, 2L, 3L);
    }
}
