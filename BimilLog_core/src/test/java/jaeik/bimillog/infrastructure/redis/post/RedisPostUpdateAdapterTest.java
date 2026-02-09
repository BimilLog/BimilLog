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
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static jaeik.bimillog.infrastructure.redis.RedisKey.VIEW_COUNTS_KEY;
import static jaeik.bimillog.infrastructure.redis.RedisKey.VIEW_TTL_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>RedisPostUpdateAdapter 단위 테스트</h2>
 * <p>Redis 기반 조회수 중복 방지 및 버퍼링 로직을 검증합니다.</p>
 */
@Tag("unit")
@DisplayName("RedisPostUpdateAdapter 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RedisPostUpdateAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private RedisPostUpdateAdapter adapter;

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
    @DisplayName("원자적 조회 마킹 + 조회수 증가 - 새로운 조회 시 1 반환")
    void markViewedAndIncrement_shouldReturnTrue_whenNewView() {
        // Given
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .willReturn(1L);

        // When
        boolean result = adapter.markViewedAndIncrement(1L, "m:100");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("원자적 조회 마킹 + 조회수 증가 - 이미 조회한 경우 0 반환")
    void markViewedAndIncrement_shouldReturnFalse_whenAlreadyViewed() {
        // Given
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .willReturn(0L);

        // When
        boolean result = adapter.markViewedAndIncrement(1L, "m:100");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("조회수 버퍼 조회 및 초기화 - Lua 스크립트로 원자적 처리 후 데이터 반환")
    void getAndClearViewCounts_shouldReturnMapAndClear() {
        // Given: Lua 스크립트가 HGETALL 결과를 flat list로 반환
        given(redisTemplate.execute(any(RedisScript.class), anyList()))
                .willReturn(List.of("1", "5", "2", "3"));

        // When
        Map<Long, Long> result = adapter.getAndClearViewCounts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isEqualTo(5L);
        assertThat(result.get(2L)).isEqualTo(3L);
    }

    @Test
    @DisplayName("조회수 버퍼 조회 및 초기화 - 키가 존재하지 않으면 빈 맵 반환")
    void getAndClearViewCounts_shouldReturnEmptyMap_whenKeyNotExists() {
        // Given: Lua 스크립트가 nil 반환 → Java에서 null
        given(redisTemplate.execute(any(RedisScript.class), anyList()))
                .willReturn(null);

        // When
        Map<Long, Long> result = adapter.getAndClearViewCounts();

        // Then
        assertThat(result).isEmpty();
    }
}
