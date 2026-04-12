package jaeik.bimillog.unit.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>RedisMemberAdapter 단위 테스트</h2>
 * <p>회원 페이지 캐시 저장/조회 로직을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@DisplayName("RedisMemberAdapter 단위 테스트")
@Tag("unit")
class RedisMemberAdapterTest extends BaseUnitTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisMemberAdapter redisMemberAdapter;

    private static final String MEMBER_KEY = "member:page:";

    // ==================== saveMemberPage ====================

    @Test
    @DisplayName("saveMemberPage - 10개 항목, page=0, size=10 → key 1개 저장")
    void shouldSaveOneKey_WhenTenItemsAndPageZero() throws Exception {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(objectMapper.writeValueAsString(any())).willReturn("[]");
        List<SimpleMemberDTO> members = createMembers(10);

        // When
        redisMemberAdapter.saveMemberPage(0, 10, members);

        // Then: member:page:0 만 저장
        verify(valueOperations).set(eq(MEMBER_KEY + 0), anyString(), eq(1L), eq(TimeUnit.MINUTES));
        verify(valueOperations, never()).set(eq(MEMBER_KEY + 1), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("saveMemberPage - 25개 항목, page=0, size=10 → key 3개 저장 (0, 1, 2)")
    void shouldSaveThreeKeys_WhenTwentyFiveItemsAndPageZero() throws Exception {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(objectMapper.writeValueAsString(any())).willReturn("[]");
        List<SimpleMemberDTO> members = createMembers(25);

        // When
        redisMemberAdapter.saveMemberPage(0, 10, members);

        // Then: member:page:0, member:page:1, member:page:2 저장
        verify(valueOperations).set(eq(MEMBER_KEY + 0), anyString(), eq(1L), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq(MEMBER_KEY + 1), anyString(), eq(1L), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq(MEMBER_KEY + 2), anyString(), eq(1L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("saveMemberPage - page=2 오프셋 → key가 2부터 시작")
    void shouldApplyPageOffset_WhenStartPageIsTwo() throws Exception {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(objectMapper.writeValueAsString(any())).willReturn("[]");
        List<SimpleMemberDTO> members = createMembers(10);

        // When
        redisMemberAdapter.saveMemberPage(2, 10, members);

        // Then: member:page:2 에만 저장
        verify(valueOperations).set(eq(MEMBER_KEY + 2), anyString(), eq(1L), eq(TimeUnit.MINUTES));
        verify(valueOperations, never()).set(eq(MEMBER_KEY + 0), anyString(), anyLong(), any());
        verify(valueOperations, never()).set(eq(MEMBER_KEY + 1), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("saveMemberPage - 직렬화 실패 시 예외 전파 없이 저장 건너뜀")
    void shouldSkipPage_WhenSerializationFails() throws Exception {
        // Given: 직렬화 실패, opsForValue()까지 도달 안 함
        given(objectMapper.writeValueAsString(any())).willThrow(new RuntimeException("직렬화 실패"));
        List<SimpleMemberDTO> members = createMembers(20);

        // When - 예외가 전파되지 않아야 함
        redisMemberAdapter.saveMemberPage(0, 10, members);

        // Then
        verify(redisTemplate, never()).opsForValue();
    }

    // ==================== getMemberByPage ====================

    @Test
    @DisplayName("getMemberByPage - 캐시 히트 → 해당 페이지 데이터 반환")
    void shouldReturnCachedPage_WhenCacheHit() throws Exception {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        List<SimpleMemberDTO> cached = createMembers(10);
        ObjectMapper realMapper = new ObjectMapper();
        given(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .willReturn(cached);
        given(valueOperations.get(MEMBER_KEY + 0)).willReturn("[...]");

        // When
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(0, 10);

        // Then
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.getContent()).hasSize(10);
        verify(valueOperations).get(MEMBER_KEY + 0);
    }

    @Test
    @DisplayName("getMemberByPage - 캐시 미스 → 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenCacheMiss() {
        // Given: Redis에 해당 키 없음
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(MEMBER_KEY + 0)).willReturn(null);

        // When
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(0, 10);

        // Then
        assertThat(result.isEmpty()).isTrue();
        verify(valueOperations).get(MEMBER_KEY + 0);
    }

    @Test
    @DisplayName("getMemberByPage - 역직렬화 실패 시 해당 페이지 건너뛰고 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenDeserializationFails() throws Exception {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(MEMBER_KEY + 0)).willReturn("invalid-json");
        given(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .willThrow(new RuntimeException("역직렬화 실패"));

        // When
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(0, 10);

        // Then: 예외 전파 없이 빈 페이지
        assertThat(result.isEmpty()).isTrue();
    }

    // ==================== 헬퍼 ====================

    private List<SimpleMemberDTO> createMembers(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new SimpleMemberDTO((long) i, "member" + i))
                .toList();
    }
}
