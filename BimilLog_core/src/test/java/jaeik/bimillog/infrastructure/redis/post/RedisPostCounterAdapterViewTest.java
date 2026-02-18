package jaeik.bimillog.infrastructure.redis.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static jaeik.bimillog.infrastructure.redis.RedisKey.VIEW_COUNTS_KEY;
import static jaeik.bimillog.infrastructure.redis.RedisKey.VIEW_TTL_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>RedisPostCounterAdapter 조회수 버퍼 단위 테스트</h2>
 * <p>Redis 기반 조회수 중복 방지 및 버퍼링 로직을 검증합니다.</p>
 */
@Tag("unit")
@DisplayName("RedisPostCounterAdapter 조회수 버퍼 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RedisPostCounterAdapterViewTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private RedisPostCounterAdapter adapter;

    @Test
    @DisplayName("새로운 조회 시 SET NX EX 성공 + 조회수 버퍼 증가")
    void markViewedAndIncrement_shouldReturnTrue_whenNewView() {
        // Given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent("post:view:1:m:100", "1", Duration.ofSeconds(VIEW_TTL_SECONDS)))
                .willReturn(true);
        given(stringRedisTemplate.opsForHash()).willReturn(hashOperations);

        // When
        boolean result = adapter.markViewedAndIncrement(1L, "m:100");

        // Then
        assertThat(result).isTrue();
        verify(hashOperations).increment(VIEW_COUNTS_KEY, "1", 1L);
    }

    @Test
    @DisplayName("이미 조회한 경우 SET NX EX 실패 + 조회수 증가하지 않음")
    void markViewedAndIncrement_shouldReturnFalse_whenAlreadyViewed() {
        // Given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent("post:view:1:m:100", "1", Duration.ofSeconds(VIEW_TTL_SECONDS)))
                .willReturn(false);

        // When
        boolean result = adapter.markViewedAndIncrement(1L, "m:100");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("조회수 버퍼 조회 및 초기화 - Lua 스크립트로 원자적 처리 후 데이터 반환")
    void getAndClearViewCounts_shouldReturnMapAndClear() {
        // Given: Lua 스크립트가 HGETALL 결과를 flat list로 반환
        given(stringRedisTemplate.execute(any(RedisScript.class), anyList()))
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
        given(stringRedisTemplate.execute(any(RedisScript.class), anyList()))
                .willReturn(null);

        // When
        Map<Long, Long> result = adapter.getAndClearViewCounts();

        // Then
        assertThat(result).isEmpty();
    }
}
