package jaeik.bimillog.adapter.out.redis.paper;

import jaeik.bimillog.infrastructure.out.redis.paper.RedisPaperDeleteAdapter;
import jaeik.bimillog.infrastructure.out.redis.paper.RedisPaperKeys;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisPaperDeleteAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 롤링페이퍼 캐시 삭제 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisPaperDeleteAdapterIntegrationTest {

    @Autowired
    private RedisPaperDeleteAdapter redisPaperDeleteAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SCORE_KEY = RedisPaperKeys.REALTIME_PAPER_SCORE_KEY;

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기 롤링페이퍼 목록에서 회원 제거")
    void shouldRemoveMember_WhenMemberIdProvided() {
        // Given: Redis Sorted Set에 회원 데이터 설정
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 100.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "2", 80.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "3", 60.0);

        // 초기 크기 확인
        Long initialSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(initialSize).isEqualTo(3);

        // When: 회원 2번 제거
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(2L);

        // Then: 회원 2번만 제거됨
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isEqualTo(2);

        // 제거된 회원 확인
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "2")).isNull();

        // 남아있는 회원 확인
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "1")).isEqualTo(100.0);
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "3")).isEqualTo(60.0);
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 회원 제거 시도")
    void shouldDoNothing_WhenMemberDoesNotExist() {
        // Given: Redis Sorted Set에 회원 데이터 설정
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 100.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "2", 80.0);

        // 초기 크기 확인
        Long initialSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(initialSize).isEqualTo(2);

        // When: 존재하지 않는 회원 999번 제거 시도
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(999L);

        // Then: 크기 변화 없음
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isEqualTo(2);

        // 기존 데이터 확인
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "1")).isEqualTo(100.0);
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "2")).isEqualTo(80.0);
    }

    @Test
    @DisplayName("정상 케이스 - 빈 Sorted Set에서 회원 제거 시도")
    void shouldDoNothing_WhenSetIsEmpty() {
        // Given: 빈 Sorted Set

        // When: 회원 1번 제거 시도
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(1L);

        // Then: 에러 없이 정상 처리, 크기 0 유지
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isZero();
    }

    @Test
    @DisplayName("정상 케이스 - 여러 회원 순차적으로 제거")
    void shouldRemoveMultipleMembers_WhenCalledSequentially() {
        // Given: Redis Sorted Set에 5명의 회원 데이터 설정
        for (int i = 1; i <= 5; i++) {
            redisTemplate.opsForZSet().add(SCORE_KEY, String.valueOf(i), i * 10.0);
        }

        // 초기 크기 확인
        Long initialSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(initialSize).isEqualTo(5);

        // When: 회원 1, 3, 5번 제거
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(1L);
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(3L);
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(5L);

        // Then: 3명 제거, 2명 남음
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isEqualTo(2);

        // 제거된 회원 확인
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "1")).isNull();
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "3")).isNull();
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "5")).isNull();

        // 남아있는 회원 확인
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "2")).isEqualTo(20.0);
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "4")).isEqualTo(40.0);
    }

    @Test
    @DisplayName("정상 케이스 - 동일 회원 중복 제거 시도")
    void shouldHandleDuplicateRemoval_WhenSameMemberRemovedTwice() {
        // Given: Redis Sorted Set에 회원 데이터 설정
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 100.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "2", 80.0);

        // When: 동일 회원 1번을 2번 제거
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(1L);
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(1L); // 중복 제거

        // Then: 에러 없이 정상 처리, 최종 크기 1
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isEqualTo(1);

        // 회원 1번 제거 확인
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "1")).isNull();

        // 회원 2번 남아있음 확인
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "2")).isEqualTo(80.0);
    }

    @Test
    @DisplayName("정상 케이스 - 마지막 회원 제거")
    void shouldRemoveLastMember_WhenOnlyOneMemberExists() {
        // Given: 회원 1명만 존재
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 100.0);

        // When: 마지막 회원 제거
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(1L);

        // Then: Sorted Set이 비어있음
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isZero();
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "1")).isNull();
    }

    @Test
    @DisplayName("정상 케이스 - 모든 회원 제거")
    void shouldRemoveAllMembers_WhenAllMembersRemoved() {
        // Given: Redis Sorted Set에 10명의 회원 데이터 설정
        for (int i = 1; i <= 10; i++) {
            redisTemplate.opsForZSet().add(SCORE_KEY, String.valueOf(i), i * 10.0);
        }

        // When: 모든 회원 제거
        for (int i = 1; i <= 10; i++) {
            redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore((long) i);
        }

        // Then: Sorted Set이 비어있음
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isZero();
    }

    @Test
    @DisplayName("정상 케이스 - 음수 점수를 가진 회원 제거")
    void shouldRemoveMember_WhenScoreIsNegative() {
        // Given: 음수 점수를 가진 회원
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", -10.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "2", 50.0);

        // When: 음수 점수 회원 제거
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(1L);

        // Then: 정상적으로 제거됨
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isEqualTo(1);
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "1")).isNull();
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "2")).isEqualTo(50.0);
    }

    @Test
    @DisplayName("정상 케이스 - 0점을 가진 회원 제거")
    void shouldRemoveMember_WhenScoreIsZero() {
        // Given: 0점을 가진 회원
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 0.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "2", 50.0);

        // When: 0점 회원 제거
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(1L);

        // Then: 정상적으로 제거됨
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isEqualTo(1);
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "1")).isNull();
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "2")).isEqualTo(50.0);
    }
}
