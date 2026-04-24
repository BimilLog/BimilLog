package jaeik.bimillog.springboot.mysql.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter.CachedMemberPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisMemberAdapter 로컬 통합 테스트</h2>
 * <p>캐시 키 포맷 member:page:{page}:size:{size} 기반으로 (page, size) 조합별 독립 캐시가
 * 올바르게 저장/조회되는지 검증합니다.</p>
 * <p>실행 전 Redis(6379) 필요</p>
 */
@DisplayName("RedisMemberAdapter 로컬 통합 테스트")
@SpringBootTest
@Tag("local-integration")
@ActiveProfiles("local-integration")
class RedisMemberAdapterLocalIntegrationTest {

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

    // ==================== 캐시 저장 / 조회 ====================

    @Test
    @DisplayName("saveMemberPage - 저장 후 lookup 으로 동일 데이터 조회")
    void saveAndLookup_shouldReturnCachedMembers() {
        // Given
        int page = 0, size = 10;
        List<SimpleMemberDTO> members = buildMembers(1L, 2L, 3L);

        // When
        redisMemberAdapter.saveMemberPage(page, size, members);
        CachedMemberPage result = redisMemberAdapter.lookup(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(3);
        assertThat(result.data().get(0).getMemberId()).isEqualTo(1L);
        assertThat(result.data().get(1).getMemberId()).isEqualTo(2L);
        assertThat(result.data().get(2).getMemberId()).isEqualTo(3L);
        assertThat(result.isStale()).isFalse();
    }

    @Test
    @DisplayName("lookup - 캐시 미스 시 null 반환")
    void lookup_whenCacheMiss_shouldReturnNull() {
        // When
        CachedMemberPage result = redisMemberAdapter.lookup(99, 10);

        // Then
        assertThat(result).isNull();
    }

    // ==================== (page, size) 조합별 독립 캐시 ====================

    @Test
    @DisplayName("size가 다르면 독립적인 캐시 키를 사용한다")
    void differentSizes_shouldHaveIndependentCacheKeys() {
        // Given
        List<SimpleMemberDTO> members10 = buildMembers(10L, 11L);
        List<SimpleMemberDTO> members20 = buildMembers(20L, 21L, 22L);

        // When
        redisMemberAdapter.saveMemberPage(0, 10, members10);
        redisMemberAdapter.saveMemberPage(0, 20, members20);

        // Then: size=10 캐시
        CachedMemberPage result10 = redisMemberAdapter.lookup(0, 10);
        assertThat(result10.data()).hasSize(2);
        assertThat(result10.data().get(0).getMemberId()).isEqualTo(10L);

        // Then: size=20 캐시 (별도 키라 섞이지 않음)
        CachedMemberPage result20 = redisMemberAdapter.lookup(0, 20);
        assertThat(result20.data()).hasSize(3);
        assertThat(result20.data().get(0).getMemberId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("page가 다르면 독립적인 캐시 키를 사용한다")
    void differentPages_shouldHaveIndependentCacheKeys() {
        // Given
        List<SimpleMemberDTO> page0Members = buildMembers(1L, 2L);
        List<SimpleMemberDTO> page1Members = buildMembers(3L, 4L);

        // When
        redisMemberAdapter.saveMemberPage(0, 10, page0Members);
        redisMemberAdapter.saveMemberPage(1, 10, page1Members);

        // Then
        CachedMemberPage result0 = redisMemberAdapter.lookup(0, 10);
        assertThat(result0.data().get(0).getMemberId()).isEqualTo(1L);

        CachedMemberPage result1 = redisMemberAdapter.lookup(1, 10);
        assertThat(result1.data().get(0).getMemberId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("size=10, 20, 30 모두 독립 캐시로 동시에 존재할 수 있다")
    void allSizes_shouldCoexistIndependently() {
        // Given
        redisMemberAdapter.saveMemberPage(0, 10, buildMembers(10L));
        redisMemberAdapter.saveMemberPage(0, 20, buildMembers(20L));
        redisMemberAdapter.saveMemberPage(0, 30, buildMembers(30L));

        // Then
        assertThat(redisMemberAdapter.lookup(0, 10).data().get(0).getMemberId()).isEqualTo(10L);
        assertThat(redisMemberAdapter.lookup(0, 20).data().get(0).getMemberId()).isEqualTo(20L);
        assertThat(redisMemberAdapter.lookup(0, 30).data().get(0).getMemberId()).isEqualTo(30L);
    }

    // ==================== 캐시 키 포맷 검증 ====================

    @Test
    @DisplayName("캐시 키 포맷이 member:page:{page}:size:{size} 형태임을 검증")
    void cacheKeyFormat_shouldContainPageAndSize() {
        // When
        redisMemberAdapter.saveMemberPage(3, 20, buildMembers(1L));

        // Then: 해당 키가 Redis에 존재하는지 직접 확인
        Boolean exists = stringRedisTemplate.hasKey("member:page:3:size:20");
        assertThat(exists).isTrue();

        // 이전 포맷(member:page:3)은 존재하지 않음
        Boolean oldFormatExists = stringRedisTemplate.hasKey("member:page:3");
        assertThat(oldFormatExists).isFalse();
    }

    // ==================== Pageable 메타데이터 ====================

    @Test
    @DisplayName("저장된 캐시의 데이터 사이즈가 입력과 일치한다")
    void savedDataSize_shouldMatchInput() {
        // Given
        redisMemberAdapter.saveMemberPage(2, 20, buildMembers(100L, 101L));

        // When
        CachedMemberPage result = redisMemberAdapter.lookup(2, 20);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(2);
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
