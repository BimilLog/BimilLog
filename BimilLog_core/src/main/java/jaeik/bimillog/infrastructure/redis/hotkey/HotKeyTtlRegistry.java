//package jaeik.bimillog.infrastructure.redis.hotkey;
//
//import jakarta.annotation.PostConstruct;
//import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
//import org.springframework.stereotype.Component;
//
//import java.time.Duration;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//
//import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;
//
///**
// * <h2>핫키 TTL 레지스트리</h2>
// * <p>캐시 키별 원래 TTL을 매핑합니다.</p>
// * <p>핫키 TTL 갱신 시 각 키의 원래 TTL을 조회하는 데 사용됩니다.</p>
// * <p>새로운 캐시 키가 추가되면 {@link #init()}에 매핑을 추가합니다.</p>
// *
// * @author Jaeik
// * @version 2.6.0
// * @deprecated 2.7.0부터 사용 중단. 스케줄러 기반 캐시 갱신으로 대체.
// */
//@Deprecated(since = "2.7.0", forRemoval = true)
//@Component
//public class HotKeyTtlRegistry {
//
//    private Map<String, Duration> keyTtlMap;
//
//    @PostConstruct
//    void init() {
//        Map<String, Duration> map = new HashMap<>();
//        map.put(getSimplePostHashKey(PostCacheFlag.WEEKLY), POST_CACHE_TTL_WEEKLY_LEGEND);
//        map.put(getSimplePostHashKey(PostCacheFlag.LEGEND), POST_CACHE_TTL_WEEKLY_LEGEND);
//        // REALTIME, NOTICE는 영구 TTL이므로 핫키 갱신 대상에서 제외
//        keyTtlMap = Collections.unmodifiableMap(map);
//    }
//
//    /**
//     * <h3>키의 원래 TTL 반환</h3>
//     *
//     * @param cacheKey Redis 캐시 키
//     * @return 원래 TTL (TTL이 없는 키이면 null)
//     */
//    public Duration getOriginalTtl(String cacheKey) {
//        return keyTtlMap.get(cacheKey);
//    }
//
//    /**
//     * <h3>핫키 TTL 갱신 대상 여부 확인</h3>
//     *
//     * @param cacheKey Redis 캐시 키
//     * @return TTL이 정의된 키이면 true
//     */
//    public boolean isRefreshable(String cacheKey) {
//        return keyTtlMap.containsKey(cacheKey);
//    }
//}
