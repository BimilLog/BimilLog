//package jaeik.bimillog.infrastructure.redis.hotkey;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//import java.time.Duration;
//import java.util.Map;
//import java.util.Set;
//
//import static jaeik.bimillog.infrastructure.redis.hotkey.RedisHotKeyKeys.*;
//
///**
// * <h2>핫키 Redis 어댑터</h2>
// * <p>핫키 ZSet 분산 합산, 분산락, TTL 갱신 관련 Redis 커맨드를 캡슐화합니다.</p>
// *
// * @author Jaeik
// * @version 2.6.0
// * @deprecated 2.7.0부터 사용 중단. 스케줄러 기반 캐시 갱신으로 대체.
// */
//@Deprecated(since = "2.7.0", forRemoval = true)
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class HotKeyRedisAdapter {
//
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    /**
//     * <h3>핫키 접근 횟수를 ZSet에 분산 합산 (ZINCRBY)</h3>
//     * <p>여러 인스턴스의 로컬 카운트를 하나의 ZSet에 합산합니다.</p>
//     *
//     * @param hotKeyCounts 핫키별 접근 횟수 맵
//     */
//    public void mergeAccessCounts(Map<String, Long> hotKeyCounts) {
//        hotKeyCounts.forEach((key, count) ->
//                redisTemplate.opsForZSet().incrementScore(HOTKEY_ACCESS_ZSET, key, count)
//        );
//    }
//
//    /**
//     * <h3>ZSet에서 모든 핫키 멤버 조회</h3>
//     *
//     * @return 핫키 캐시 키 Set (비어있으면 empty set)
//     */
//    public Set<Object> getAllHotKeys() {
//        return redisTemplate.opsForZSet().range(HOTKEY_ACCESS_ZSET, 0, -1);
//    }
//
//    /**
//     * <h3>TTL 갱신 분산락 획득 (SET NX)</h3>
//     * <p>한 인스턴스만 TTL 갱신을 수행하도록 보장합니다.</p>
//     *
//     * @return 락 획득 성공 시 true
//     */
//    public boolean tryAcquireTtlRefreshLock() {
//        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
//                HOTKEY_TTL_REFRESH_LOCK, "1", HOTKEY_LOCK_TTL
//        );
//        return Boolean.TRUE.equals(acquired);
//    }
//
//    /**
//     * <h3>TTL 갱신 분산락 해제</h3>
//     */
//    public void releaseTtlRefreshLock() {
//        redisTemplate.delete(HOTKEY_TTL_REFRESH_LOCK);
//    }
//
//    /**
//     * <h3>캐시 키 TTL 갱신</h3>
//     *
//     * @param cacheKey Redis 캐시 키
//     * @param ttl      갱신할 TTL
//     */
//    public void refreshTtl(String cacheKey, Duration ttl) {
//        redisTemplate.expire(cacheKey, ttl);
//    }
//
//    /**
//     * <h3>핫키 ZSet 삭제</h3>
//     * <p>TTL 갱신 완료 후 ZSet을 초기화합니다.</p>
//     */
//    public void deleteAccessZSet() {
//        redisTemplate.delete(HOTKEY_ACCESS_ZSET);
//    }
//}
