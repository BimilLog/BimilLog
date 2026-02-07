//package jaeik.bimillog.infrastructure.redis.hotkey;
//
//import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Tag;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//
//import java.time.Duration;
//import java.util.stream.Stream;
//
//import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;
//import static jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter.getSimplePostHashKey;
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Tag("unit")
//@DisplayName("HotKeyTtlRegistry 단위 테스트")
//class HotKeyTtlRegistryTest {
//
//    private HotKeyTtlRegistry registry;
//
//    @BeforeEach
//    void setUp() {
//        registry = new HotKeyTtlRegistry();
//        registry.init();
//    }
//
//    @ParameterizedTest(name = "{0} 키의 원래 TTL은 {1}이다")
//    @MethodSource("provideCacheKeyAndTtl")
//    @DisplayName("캐시 키별 원래 TTL을 정확히 반환한다")
//    void shouldReturnCorrectOriginalTtl(String cacheKey, Duration expectedTtl) {
//        // when
//        Duration ttl = registry.getOriginalTtl(cacheKey);
//
//        // then
//        assertThat(ttl).isEqualTo(expectedTtl);
//    }
//
//    private static Stream<Arguments> provideCacheKeyAndTtl() {
//        return Stream.of(
//                Arguments.of(getSimplePostHashKey(PostCacheFlag.WEEKLY), POST_CACHE_TTL_WEEKLY_LEGEND),
//                Arguments.of(getSimplePostHashKey(PostCacheFlag.LEGEND), POST_CACHE_TTL_WEEKLY_LEGEND)
//        );
//    }
//
//    @ParameterizedTest(name = "{0} 키는 갱신 대상이다")
//    @MethodSource("provideRefreshableKeys")
//    @DisplayName("TTL이 정의된 캐시 키는 갱신 대상이다")
//    void shouldBeRefreshable(String cacheKey) {
//        // when & then
//        assertThat(registry.isRefreshable(cacheKey)).isTrue();
//    }
//
//    private static Stream<Arguments> provideRefreshableKeys() {
//        return Stream.of(
//                Arguments.of(getSimplePostHashKey(PostCacheFlag.WEEKLY)),
//                Arguments.of(getSimplePostHashKey(PostCacheFlag.LEGEND))
//        );
//    }
//
//    @Test
//    @DisplayName("미등록 키는 갱신 대상이 아니다")
//    void shouldNotBeRefreshableForUnregisteredKey() {
//        // when & then
//        assertThat(registry.isRefreshable("paper:realtime:score")).isFalse();
//        assertThat(registry.isRefreshable("unknown:key")).isFalse();
//        // REALTIME, NOTICE는 영구 TTL이므로 핫키 갱신 대상에서 제외됨
//        assertThat(registry.isRefreshable(getSimplePostHashKey(PostCacheFlag.REALTIME))).isFalse();
//        assertThat(registry.isRefreshable(getSimplePostHashKey(PostCacheFlag.NOTICE))).isFalse();
//    }
//
//    @Test
//    @DisplayName("미등록 키의 TTL은 null이다")
//    void shouldReturnNullForUnregisteredKey() {
//        // when & then
//        assertThat(registry.getOriginalTtl("paper:realtime:score")).isNull();
//        assertThat(registry.getOriginalTtl("unknown:key")).isNull();
//    }
//}
