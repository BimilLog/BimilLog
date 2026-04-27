package jaeik.bimillog.springboot.mysql.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter.CachedMemberPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * <h2>MemberQueryService Soft TTL 갱신 로컬 통합 테스트</h2>
 * <p>cachedAt 기준 50초 경계에서 비동기 갱신이 정확히 트리거되는지 검증.</p>
 * <p>실행 전 MySQL + Redis(6379) 필요.</p>
 */
@DisplayName("MemberQueryService Soft TTL 갱신 로컬 통합 테스트")
@SpringBootTest
@Tag("local-integration")
@ActiveProfiles("local-integration")
class MemberQueryServiceCacheRefreshLocalIntegrationTest {

    private static final int PAGE = 0;
    private static final int SIZE = 20;
    private static final String CACHE_KEY = "member:page:0:size:20";

    @Autowired
    private MemberQueryService memberQueryService;

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

    @Test
    @DisplayName("cachedAt이 49초 전(stale 미만)이면 비동기 갱신이 트리거되지 않는다")
    void shouldNotTriggerRefresh_whenCachedAtUnder50Seconds() throws Exception {
        // Given: 49초 전 cachedAt 직접 주입 (stale=false)
        long pastCachedAt = System.currentTimeMillis() - 49_000L;
        injectCachedPage(pastCachedAt, List.of(buildMember(1L)));

        // When: 캐시 조회 호출
        memberQueryService.findAllMembers(PageRequest.of(PAGE, SIZE));

        // Then: 잠시 기다려도 cachedAt 그대로 (비동기 갱신 발생 안 함)
        Thread.sleep(800);
        CachedMemberPage stillFresh = redisMemberAdapter.lookup(PAGE, SIZE);
        assertThat(stillFresh).isNotNull();
        assertThat(stillFresh.cachedAt())
                .as("49초 전 cachedAt은 stale이 아니므로 갱신되지 않아야 함")
                .isEqualTo(pastCachedAt);
    }

    @Test
    @DisplayName("cachedAt이 51초 전(stale)이면 비동기 갱신이 트리거되어 cachedAt이 새로워진다")
    void shouldTriggerRefresh_whenCachedAtOver50Seconds() throws Exception {
        // Given: 51초 전 cachedAt 직접 주입 (stale=true)
        long pastCachedAt = System.currentTimeMillis() - 51_000L;
        injectCachedPage(pastCachedAt, List.of(buildMember(1L)));

        // When: 캐시 조회 호출 → stale 감지 → 비동기 refresh 트리거
        memberQueryService.findAllMembers(PageRequest.of(PAGE, SIZE));

        // Then: Awaitility로 cachedAt 갱신을 5초 한도로 대기
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    CachedMemberPage refreshed = redisMemberAdapter.lookup(PAGE, SIZE);
                    assertThat(refreshed).isNotNull();
                    assertThat(refreshed.cachedAt())
                            .as("stale 감지 후 비동기 갱신으로 cachedAt이 새로워져야 함")
                            .isGreaterThan(pastCachedAt);
                    assertThat(refreshed.isStale())
                            .as("갱신 직후라 stale이 아니어야 함")
                            .isFalse();
                });
    }

    @Test
    @DisplayName("stale 캐시에 대해 동시 다발적 호출 시 모든 호출이 즉시 stale 데이터를 받고 갱신은 1회만 발생한다")
    void shouldDedupeConcurrentStaleRefresh() throws Exception {
        // Given: 55초 전 cachedAt (stale)
        long pastCachedAt = System.currentTimeMillis() - 55_000L;
        List<SimpleMemberDTO> staleData = List.of(buildMember(7L));
        injectCachedPage(pastCachedAt, staleData);

        // When: 동시 다발적으로 5번 호출
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(5);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<Long>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                long t0 = System.nanoTime();
                memberQueryService.findAllMembers(PageRequest.of(PAGE, SIZE));
                return System.nanoTime() - t0;
            }));
        }
        start.countDown();

        // Then: 모든 호출이 1초 이내 즉시 반환 (대기 없음)
        for (java.util.concurrent.Future<Long> f : futures) {
            long elapsedNanos = f.get(2, TimeUnit.SECONDS);
            assertThat(elapsedNanos)
                    .as("stale 데이터는 비동기 갱신을 트리거하되 즉시 반환되어야 함")
                    .isLessThan(TimeUnit.SECONDS.toNanos(1));
        }
        pool.shutdown();

        // Then: 비동기 갱신 1회로 cachedAt이 새로워짐
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    CachedMemberPage refreshed = redisMemberAdapter.lookup(PAGE, SIZE);
                    assertThat(refreshed).isNotNull();
                    assertThat(refreshed.cachedAt()).isGreaterThan(pastCachedAt);
                });
    }

    private void injectCachedPage(long cachedAt, List<SimpleMemberDTO> data) throws Exception {
        String json = objectMapper.writeValueAsString(new CachedMemberPage(cachedAt, data));
        stringRedisTemplate.opsForValue().set(CACHE_KEY, json, 60, TimeUnit.SECONDS);
    }

    private SimpleMemberDTO buildMember(Long id) {
        return SimpleMemberDTO.builder()
                .memberId(id)
                .memberName("member" + id)
                .build();
    }
}
