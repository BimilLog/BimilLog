package jaeik.bimillog.springboot.mysql.redis;

import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisMemberAdapter 로컬 통합 테스트</h2>
 * <p>실제 Redis 환경에서 회원 페이지 캐시 저장/조회의 E2E 동작을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("local-integration")
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RedisMemberAdapterIntegrationTest {

    @Autowired
    private RedisMemberAdapter redisMemberAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        RedisTestHelper.flushRedis(redisTemplate);
    }

    // ==================== save → get 라운드트립 ====================

    @Test
    @DisplayName("save → get 라운드트립 - page=0, size=10, 10개 저장 후 정상 조회")
    void shouldReturnSavedData_WhenSaveAndGetPage0() {
        // Given
        List<SimpleMemberDTO> members = createMembers(10);
        redisMemberAdapter.saveMemberPage(0, 10, members);

        // When
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(0, 10);

        // Then
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getContent().get(0).getMemberId()).isEqualTo(1L);
        assertThat(result.getContent().get(9).getMemberId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("save → get 라운드트립 - page=1, size=10으로 저장된 1페이지 조회")
    void shouldReturnSavedData_WhenSaveAndGetPage1() {
        // Given: page=1 위치에 직접 저장
        List<SimpleMemberDTO> page1Members = createMembersFrom(11, 10);
        redisMemberAdapter.saveMemberPage(1, 10, page1Members);

        // When
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(1, 10);

        // Then
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getContent().get(0).getMemberId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("save → get 라운드트립 - 25개 저장 후 각 페이지 독립 조회")
    void shouldReturnCorrectPage_WhenTwentyFiveItemsSaved() {
        // Given: 25개를 page=0 시작으로 저장 → member:page:0(10개), :1(10개), :2(5개)
        List<SimpleMemberDTO> allMembers = createMembers(25);
        redisMemberAdapter.saveMemberPage(0, 10, allMembers);

        // When
        Page<SimpleMemberDTO> page0 = redisMemberAdapter.getMemberByPage(0, 10);
        Page<SimpleMemberDTO> page1 = redisMemberAdapter.getMemberByPage(1, 10);
        Page<SimpleMemberDTO> page2 = redisMemberAdapter.getMemberByPage(2, 10);

        // Then
        assertThat(page0.getContent()).hasSize(10);
        assertThat(page0.getContent().get(0).getMemberId()).isEqualTo(1L);

        assertThat(page1.getContent()).hasSize(10);
        assertThat(page1.getContent().get(0).getMemberId()).isEqualTo(11L);

        assertThat(page2.getContent()).hasSize(5);
        assertThat(page2.getContent().get(0).getMemberId()).isEqualTo(21L);
    }

    @Test
    @DisplayName("getMemberByPage - 캐시 미스 → 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenNothingSaved() {
        // When: 아무것도 저장하지 않고 조회
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(0, 10);

        // Then
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("getMemberByPage - 다른 페이지 저장 후 없는 페이지 조회 → 빈 페이지")
    void shouldReturnEmptyPage_WhenRequestingNonExistentPage() {
        // Given: page=0만 저장
        redisMemberAdapter.saveMemberPage(0, 10, createMembers(10));

        // When: page=5 조회
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(5, 10);

        // Then
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("memberName 데이터 무결성 - 저장한 memberName 그대로 조회")
    void shouldPreserveMemberNames_AfterCacheRoundtrip() {
        // Given
        List<SimpleMemberDTO> members = List.of(
                new SimpleMemberDTO(1L, "홍길동"),
                new SimpleMemberDTO(2L, "이순신"),
                new SimpleMemberDTO(3L, "김유신")
        );
        redisMemberAdapter.saveMemberPage(0, 10, members);

        // When
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(0, 10);

        // Then
        assertThat(result.getContent()).extracting(SimpleMemberDTO::getMemberName)
                .containsExactly("홍길동", "이순신", "김유신");
    }

    @Test
    @DisplayName("빈 리스트 저장 후 조회 → 빈 페이지")
    void shouldReturnEmptyPage_WhenEmptyListSaved() {
        // Given
        redisMemberAdapter.saveMemberPage(0, 10, List.of());

        // When
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(0, 10);

        // Then: 빈 리스트는 저장 루프 자체가 실행 안 됨 → 캐시 미스 → 빈 페이지
        assertThat(result.isEmpty()).isTrue();
    }

    // ==================== 헬퍼 ====================

    private List<SimpleMemberDTO> createMembers(int count) {
        return createMembersFrom(1, count);
    }

    private List<SimpleMemberDTO> createMembersFrom(int startId, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new SimpleMemberDTO((long) (startId + i), "member" + (startId + i)))
                .toList();
    }
}
