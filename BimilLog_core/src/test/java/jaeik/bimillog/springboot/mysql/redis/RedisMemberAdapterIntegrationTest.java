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
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private static final String MEMBER_KEY_PREFIX = "member:page:";

    @Autowired
    private RedisMemberAdapter redisMemberAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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

    // ==================== size 변동 시나리오 (BASE_SIZE=10 기반 키 합산) ====================

    @Test
    @DisplayName("size=20 page=0 → member:page:0 + member:page:1 합산 반환")
    void shouldMergeTwoKeys_WhenSize20Page0() {
        // Given: BASE_SIZE=10 단위로 저장
        redisMemberAdapter.saveMemberPage(0, 10, createMembers(20));

        // When
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(0, 20);

        // Then: key:0(1~10) + key:1(11~20) 합산 → 20개
        assertThat(result.getContent()).hasSize(20);
        assertThat(result.getContent().get(0).getMemberId()).isEqualTo(1L);
        assertThat(result.getContent().get(19).getMemberId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("size=30 page=0 → member:page:0/1/2 합산 반환")
    void shouldMergeThreeKeys_WhenSize30Page0() {
        // Given
        redisMemberAdapter.saveMemberPage(0, 10, createMembers(30));

        // When
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(0, 30);

        // Then: key:0 + key:1 + key:2 합산 → 30개
        assertThat(result.getContent()).hasSize(30);
        assertThat(result.getContent().get(0).getMemberId()).isEqualTo(1L);
        assertThat(result.getContent().get(29).getMemberId()).isEqualTo(30L);
    }

    @Test
    @DisplayName("size=20 page=1 → member:page:2 + member:page:3 합산 반환 (절대 위치 20~39)")
    void shouldMergeCorrectKeys_WhenSize20Page1() {
        // Given: 40개 저장 → key:0(1~10), key:1(11~20), key:2(21~30), key:3(31~40)
        redisMemberAdapter.saveMemberPage(0, 10, createMembers(40));

        // When: page=1, size=20 → 절대 위치 20~39
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(1, 20);

        // Then: key:2 + key:3 합산 → 20개 (memberId 21~40)
        assertThat(result.getContent()).hasSize(20);
        assertThat(result.getContent().get(0).getMemberId()).isEqualTo(21L);
        assertThat(result.getContent().get(19).getMemberId()).isEqualTo(40L);
    }

    @Test
    @DisplayName("size=30 page=1 → member:page:3/4/5 합산 반환 (절대 위치 30~59)")
    void shouldMergeCorrectKeys_WhenSize30Page1() {
        // Given: 60개 저장 → key:0~5
        redisMemberAdapter.saveMemberPage(0, 10, createMembers(60));

        // When: page=1, size=30 → 절대 위치 30~59
        Page<SimpleMemberDTO> result = redisMemberAdapter.getMemberByPage(1, 30);

        // Then: key:3 + key:4 + key:5 합산 → 30개 (memberId 31~60)
        assertThat(result.getContent()).hasSize(30);
        assertThat(result.getContent().get(0).getMemberId()).isEqualTo(31L);
        assertThat(result.getContent().get(29).getMemberId()).isEqualTo(60L);
    }

    @Test
    @DisplayName("size=20 DB폴백 저장 후 size=10으로 조회 → 각 페이지 정상 반환")
    void shouldReadWithSize10_AfterSavedWithSize20() {
        // Given: size=20 DB폴백으로 page=0 저장 → key:0(1~10), key:1(11~20)
        redisMemberAdapter.saveMemberPage(0, 20, createMembers(20));

        // When: size=10으로 각 페이지 조회
        Page<SimpleMemberDTO> page0 = redisMemberAdapter.getMemberByPage(0, 10);
        Page<SimpleMemberDTO> page1 = redisMemberAdapter.getMemberByPage(1, 10);

        // Then: 키가 BASE_SIZE 단위이므로 size=10 조회도 정상
        assertThat(page0.getContent()).hasSize(10);
        assertThat(page0.getContent().get(0).getMemberId()).isEqualTo(1L);
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page1.getContent().get(0).getMemberId()).isEqualTo(11L);
    }

    // ==================== Redis 키 이름 검증 ====================

    @Test
    @DisplayName("saveMemberPage - page=0, 10개 저장 시 member:page:0 키에만 저장")
    void shouldWriteToCorrectKey_WhenPage0Size10() {
        // Given
        List<SimpleMemberDTO> members = createMembers(10);

        // When
        redisMemberAdapter.saveMemberPage(0, 10, members);

        // Then: "member:page:0" 에만 데이터 존재
        assertThat(stringRedisTemplate.hasKey(MEMBER_KEY_PREFIX + "0")).isTrue();
        assertThat(stringRedisTemplate.hasKey(MEMBER_KEY_PREFIX + "1")).isFalse();
    }

    @Test
    @DisplayName("saveMemberPage - page=0, 25개 저장 시 member:page:0/1/2 에 분산 저장")
    void shouldWriteToCorrectKeys_WhenTwentyFiveItemsSaved() {
        // Given
        List<SimpleMemberDTO> members = createMembers(25);

        // When
        redisMemberAdapter.saveMemberPage(0, 10, members);

        // Then: 0~9 → :0, 10~19 → :1, 20~24 → :2
        assertThat(stringRedisTemplate.hasKey(MEMBER_KEY_PREFIX + "0")).isTrue();
        assertThat(stringRedisTemplate.hasKey(MEMBER_KEY_PREFIX + "1")).isTrue();
        assertThat(stringRedisTemplate.hasKey(MEMBER_KEY_PREFIX + "2")).isTrue();
        assertThat(stringRedisTemplate.hasKey(MEMBER_KEY_PREFIX + "3")).isFalse();
    }

    @Test
    @DisplayName("saveMemberPage - page=2로 저장 시 member:page:2 에 저장 (page 오프셋 반영)")
    void shouldApplyPageOffset_WhenStartPageIsTwo() {
        // Given
        List<SimpleMemberDTO> members = createMembers(10);

        // When: page=2부터 저장
        redisMemberAdapter.saveMemberPage(2, 10, members);

        // Then: :0, :1 은 비어있고 :2 에만 저장
        assertThat(stringRedisTemplate.hasKey(MEMBER_KEY_PREFIX + "0")).isFalse();
        assertThat(stringRedisTemplate.hasKey(MEMBER_KEY_PREFIX + "1")).isFalse();
        assertThat(stringRedisTemplate.hasKey(MEMBER_KEY_PREFIX + "2")).isTrue();
    }

    @Test
    @DisplayName("getMemberByPage - 저장된 키와 조회 키가 일치 → page=0 저장 후 page=0 조회 성공")
    void shouldReadFromMatchingKey_SavePage0GetPage0() {
        // Given
        redisMemberAdapter.saveMemberPage(0, 10, createMembers(10));

        // When: page=0 조회
        Page<SimpleMemberDTO> page0 = redisMemberAdapter.getMemberByPage(0, 10);
        // page=1 은 저장 안 했으므로 미스
        Page<SimpleMemberDTO> page1 = redisMemberAdapter.getMemberByPage(1, 10);

        // Then
        assertThat(page0.isEmpty()).isFalse();
        assertThat(page1.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("getMemberByPage - page=1로 저장 후 page=0 조회 → 미스, page=1 조회 → 히트")
    void shouldReadFromMatchingKey_SavePage1GetPage1() {
        // Given: page=1 위치에 저장
        redisMemberAdapter.saveMemberPage(1, 10, createMembers(10));

        // When
        Page<SimpleMemberDTO> page0 = redisMemberAdapter.getMemberByPage(0, 10);
        Page<SimpleMemberDTO> page1 = redisMemberAdapter.getMemberByPage(1, 10);

        // Then: page=0 은 미스, page=1 은 히트
        assertThat(page0.isEmpty()).isTrue();
        assertThat(page1.isEmpty()).isFalse();
        assertThat(page1.getContent()).hasSize(10);
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
