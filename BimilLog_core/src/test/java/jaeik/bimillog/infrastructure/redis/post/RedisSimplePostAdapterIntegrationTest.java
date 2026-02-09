package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.infrastructure.redis.RedisKey;
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
 * <h2>RedisSimplePostAdapter 통합 테스트</h2>
 * <p>스케줄러 분산 락 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisSimplePostAdapterIntegrationTest {

    @Autowired
    private RedisSimplePostAdapter redisSimplePostAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @Test
    @DisplayName("스케줄러 분산 락 획득 및 해제")
    void shouldAcquireAndReleaseSchedulerLock() {
        // Given & When: 락 획득
        String lockValue = redisSimplePostAdapter.tryAcquireSchedulerLock();

        // Then: 락 획득 성공
        assertThat(lockValue).isNotNull();
        assertThat(redisTemplate.hasKey(RedisKey.SCHEDULER_LOCK_KEY)).isTrue();

        // When: 같은 인스턴스가 다시 락 획득 시도
        String secondLockValue = redisSimplePostAdapter.tryAcquireSchedulerLock();

        // Then: 이미 락이 있으므로 실패
        assertThat(secondLockValue).isNull();

        // When: 락 해제
        redisSimplePostAdapter.releaseSchedulerLock(lockValue);

        // Then: 락 해제 후 키가 삭제됨
        assertThat(redisTemplate.hasKey(RedisKey.SCHEDULER_LOCK_KEY)).isFalse();
    }

    @Test
    @DisplayName("분산 락 - 다른 UUID로는 해제 불가")
    void shouldNotReleaseLock_WhenUuidMismatch() {
        // Given: 락 획득
        String lockValue = redisSimplePostAdapter.tryAcquireSchedulerLock();
        assertThat(lockValue).isNotNull();

        // When: 다른 UUID로 해제 시도
        redisSimplePostAdapter.releaseSchedulerLock("wrong-uuid");

        // Then: 락이 여전히 존재
        assertThat(redisTemplate.hasKey(RedisKey.SCHEDULER_LOCK_KEY)).isTrue();

        // Cleanup
        redisSimplePostAdapter.releaseSchedulerLock(lockValue);
    }

    @Test
    @DisplayName("null lockValue로 해제 시도 시 안전하게 처리")
    void shouldHandleNullLockValue() {
        // When & Then: 예외 없이 처리
        redisSimplePostAdapter.releaseSchedulerLock(null);
    }
}
