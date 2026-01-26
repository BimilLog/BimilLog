package jaeik.bimillog.infrastructure.redis.hotkey;

import java.time.Duration;

/**
 * <h2>핫키 Redis 키 상수</h2>
 * <p>핫키 판별 및 TTL 갱신 관련 모든 Redis 키, TTL, 샘플링 상수를 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 * @deprecated 2.7.0부터 사용 중단. 스케줄러 기반 캐시 갱신으로 대체.
 */
@Deprecated(since = "2.7.0", forRemoval = true)
public final class RedisHotKeyKeys {

    // ===================== 1. KEYS =====================

    /**
     * 핫키 접근 횟수 분산 합산용 ZSet 키
     * <p>Value Type: Sorted Set (cacheKey, accessCount)</p>
     */
    public static final String HOTKEY_ACCESS_ZSET = "hotkey:access:zset";

    /**
     * TTL 갱신 분산락 키
     * <p>한 인스턴스만 TTL 갱신을 수행하도록 보장합니다.</p>
     */
    public static final String HOTKEY_TTL_REFRESH_LOCK = "hotkey:ttl:refresh:lock";

    // ===================== 2. TTL =====================

    /**
     * 분산락 TTL (2초)
     */
    public static final Duration HOTKEY_LOCK_TTL = Duration.ofSeconds(2);

    // ===================== 3. SAMPLING =====================

    /**
     * 샘플링 분모 (10 = 10% 확률)
     * <p>ThreadLocalRandom.nextInt(SAMPLING_DENOMINATOR) == 0 일 때만 기록합니다.</p>
     */
    public static final int SAMPLING_DENOMINATOR = 10;

    /**
     * 핫키 판별 임계값 (샘플링 카운트 기준)
     * <p>30초 윈도우에서 샘플링 카운트가 이 값 이상이면 핫키로 판별합니다.</p>
     * <p>10% 샘플링이므로 실제 접근 약 1,000건/30초에 해당합니다.</p>
     */
    public static final long HOTKEY_THRESHOLD = 100;

    private RedisHotKeyKeys() {
    }
}
