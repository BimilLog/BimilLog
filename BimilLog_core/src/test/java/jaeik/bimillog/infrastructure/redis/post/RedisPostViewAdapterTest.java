package jaeik.bimillog.infrastructure.redis.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>RedisPostViewAdapter 단위 테스트</h2>
 * <p>Redis 기반 조회수 중복 방지 및 버퍼링 로직을 검증합니다.</p>
 */
@Tag("unit")
@DisplayName("RedisPostViewAdapter 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RedisPostViewAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisPostViewAdapter adapter;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("조회 이력이 없는 경우 false 반환")
    void hasViewed_shouldReturnFalse_whenNotViewed() {
        // Given
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.isMember("post:view:1", "m:100")).willReturn(false);

        // When
        boolean result = adapter.hasViewed(1L, "m:100");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("조회 이력이 있는 경우 true 반환")
    void hasViewed_shouldReturnTrue_whenAlreadyViewed() {
        // Given
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.isMember("post:view:1", "m:100")).willReturn(true);

        // When
        boolean result = adapter.hasViewed(1L, "m:100");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("조회 마킹 시 SET에 추가하고 TTL 설정")
    void markViewed_shouldAddToSetAndSetTtl() {
        // Given
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).willReturn(true);

        // When
        adapter.markViewed(1L, "m:100");

        // Then
        verify(setOperations).add("post:view:1", "m:100");
        verify(redisTemplate).expire("post:view:1", VIEW_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("조회수 증가 시 Hash에 HINCRBY 실행")
    void incrementViewCount_shouldIncrementHash() {
        // Given
        given(redisTemplate.opsForHash()).willReturn(hashOperations);

        // When
        adapter.incrementViewCount(1L);

        // Then
        verify(hashOperations).increment(VIEW_COUNTS_KEY, "1", 1L);
    }

    @Test
    @DisplayName("조회수 버퍼 조회 및 초기화 - RENAME 성공 시 데이터 반환")
    void getAndClearViewCounts_shouldReturnMapAndClear() {
        // Given
        String flushKey = VIEW_COUNTS_KEY + ":flush";
        given(redisTemplate.renameIfAbsent(VIEW_COUNTS_KEY, flushKey)).willReturn(true);
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(flushKey)).willReturn(Map.of("1", 5L, "2", 3L));
        given(redisTemplate.delete(flushKey)).willReturn(true);

        // When
        Map<Long, Long> result = adapter.getAndClearViewCounts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isEqualTo(5L);
        assertThat(result.get(2L)).isEqualTo(3L);
        verify(redisTemplate).delete(flushKey);
    }

    @Test
    @DisplayName("조회수 버퍼 조회 및 초기화 - RENAME 실패 시 빈 맵 반환")
    void getAndClearViewCounts_shouldReturnEmptyMap_whenRenameFailes() {
        // Given
        given(redisTemplate.renameIfAbsent(VIEW_COUNTS_KEY, VIEW_COUNTS_KEY + ":flush")).willReturn(false);

        // When
        Map<Long, Long> result = adapter.getAndClearViewCounts();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("플러시 분산 락 획득 성공")
    void tryAcquireFlushLock_shouldReturnTrue_whenAcquired() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(VIEW_FLUSH_LOCK_KEY, "1", VIEW_FLUSH_LOCK_TTL)).willReturn(true);

        // When
        boolean result = adapter.tryAcquireFlushLock();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("플러시 분산 락 획득 실패")
    void tryAcquireFlushLock_shouldReturnFalse_whenNotAcquired() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(VIEW_FLUSH_LOCK_KEY, "1", VIEW_FLUSH_LOCK_TTL)).willReturn(false);

        // When
        boolean result = adapter.tryAcquireFlushLock();

        // Then
        assertThat(result).isFalse();
    }
}
