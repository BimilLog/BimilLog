package jaeik.bimillog.springboot.mysql.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.infrastructure.redis.member.CacheMemberDTO;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisMemberAdapter PER 로컬 통합 테스트</h2>
 * <p>PER 기반 캐시 조회(HIT / MISS / EARLY_REFRESH 판단)를 실제 Redis로 검증합니다.</p>
 */
@DisplayName("RedisMemberAdapter PER 로컬 통합 테스트")
@SpringBootTest
@Tag("local-integration")
@ActiveProfiles("local-integration")
class RedisMemberAdapterPERLocalIntegrationTest {

    @Autowired
    private RedisMemberAdapter redisMemberAdapter;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanRedis() {
        Set<String> keys = stringRedisTemplate.keys("member:page:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // ==================== MISS ====================

    @Test
    @DisplayName("캐시 없으면 Optional.empty() 반환 (MISS)")
    void getMemberByPageWithPER_whenCacheMiss_returnsEmpty() {
        Optional<CacheMemberDTO> result = redisMemberAdapter.getMemberByPageWithPER(99, 10);

        assertThat(result).isEmpty();
    }

    // ==================== HIT ====================

    @Test
    @DisplayName("TTL이 충분히 남아있으면 캐시 데이터 반환 (HIT)")
    void getMemberByPageWithPER_whenTTLSufficient_returnsData() {
        // Given: TTL 60초로 저장
        List<SimpleMemberDTO> members = buildMembers(1L, 2L, 3L);
        redisMemberAdapter.saveMemberPage(0, 10, members);

        // When
        Optional<CacheMemberDTO> result = redisMemberAdapter.getMemberByPageWithPER(0, 10);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSimpleMemberDTOPage().getContent()).hasSize(3);
        assertThat(result.get().getComputeTTL()).isPositive();
    }

    @Test
    @DisplayName("반환된 TTL이 저장 시 설정한 TTL 이하임을 검증")
    void getMemberByPageWithPER_computeTTL_isWithinExpectedRange() {
        // Given
        redisMemberAdapter.saveMemberPage(0, 10, buildMembers(1L));

        // When
        Optional<CacheMemberDTO> result = redisMemberAdapter.getMemberByPageWithPER(0, 10);

        // Then: jitter ±10 적용이므로 50~70초 범위
        assertThat(result).isPresent();
        assertThat(result.get().getComputeTTL()).isBetween(1.0, 70.0);
    }

    // ==================== MISS (TTL 만료) ====================

    @Test
    @DisplayName("TTL 만료 키는 Optional.empty() 반환 (MISS)")
    void getMemberByPageWithPER_whenTTLExpired_returnsEmpty() throws Exception {
        // Given: TTL 1초로 직접 저장
        String key = "member:page:0:size:10";
        String json = objectMapper.writeValueAsString(buildMembers(1L));
        stringRedisTemplate.opsForValue().set(key, json, 1, TimeUnit.SECONDS);

        // When: 2초 대기 후 조회
        Thread.sleep(2000);
        Optional<CacheMemberDTO> result = redisMemberAdapter.getMemberByPageWithPER(0, 10);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== 캐시 저장 후 PER 반환값 타입 ====================

    @Test
    @DisplayName("computeTTL이 Double 타입으로 정상 반환됨 (Long → Double 변환 검증)")
    void getMemberByPageWithPER_computeTTL_isDouble() {
        // Given
        redisMemberAdapter.saveMemberPage(1, 10, buildMembers(10L));

        // When
        Optional<CacheMemberDTO> result = redisMemberAdapter.getMemberByPageWithPER(1, 10);

        // Then: ClassCastException 없이 Double로 정상 반환
        assertThat(result).isPresent();
        assertThat(result.get().getComputeTTL()).isInstanceOf(Double.class);
    }

    // ==================== 헬퍼 ====================

    private List<SimpleMemberDTO> buildMembers(Long... ids) {
        return java.util.Arrays.stream(ids)
                .map(id -> SimpleMemberDTO.builder()
                        .memberId(id)
                        .memberName("member" + id)
                        .build())
                .toList();
    }
}
