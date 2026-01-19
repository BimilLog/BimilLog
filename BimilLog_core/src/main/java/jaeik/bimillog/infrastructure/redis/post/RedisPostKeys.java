package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * <h2>게시글 Redis 키</h2>
 * <p>게시글 관련 모든 Redis 키 정의, TTL, 스코어링 상수 및 캐시 메타데이터를 관리합니다.</p>
 * <p>Redis 클러스터 환경을 위해 Hash Tag를 사용하여 같은 타입의 키들이 동일 슬롯에 배치됩니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
public final class RedisPostKeys {

    // ===================== 1. PREFIXES (접두사 및 고정 키) =====================
    /**
     * 게시글 캐시 접두사
     */
    public static final String POST_PREFIX = "post:";

    /**
     * 개별 게시글 캐시 접두사 (Hash Tag 포함)
     * <p>Redis 클러스터에서 같은 타입의 게시글이 동일 슬롯에 배치됩니다.</p>
     */
    public static final String SIMPLE_POST_PREFIX = "post:{%s}:simple:";

    /**
     * 게시글 상세 정보 캐시 키 접미사
     * <p>Value Type: String (PostDetail)</p>
     * <p>전체 키 형식: post:{postId}:detail</p>
     */
    public static final String FULL_POST_CACHE_SUFFIX = ":detail";

    /**
     * 티어2 저장소 키 접미사
     * <p>Value Type: Sorted Set (주간/레전드), Set (공지)</p>
     * <p>전체 키 형식: post:{type}:postids</p>
     */
    public static final String POST_IDS_SUFFIX = ":postids";

    /**
     * 실시간 인기글 점수 Sorted Set 키
     * <p>Value Type: ZSet (postId, score)</p>
     */
    public static final String REALTIME_POST_SCORE_KEY = "post:realtime:score";

    // ===================== 2. TTL (Time To Live, 만료 시간) =====================

    /**
     * 게시글 기본 캐시 TTL (5분)
     */
    public static final Duration POST_CACHE_TTL = Duration.ofMinutes(5);

    /**
     * 주간/레전드 postId 저장소 TTL (1일)
     */
    public static final Duration POST_IDS_TTL_WEEKLY_LEGEND = Duration.ofDays(1);

    // ===================== 3. SCORE CONSTANTS (점수 관련 상수) =====================

    /**
     * 실시간 인기글 점수 감쇠율 (0.97)
     * <p>5분마다 모든 게시글 점수에 곱해져 시간 경과에 따른 인기도 감소를 반영합니다.</p>
     */
    public static final double REALTIME_POST_SCORE_DECAY_RATE = 0.97;

    /**
     * 실시간 인기글 제거 임계값 (1.0)
     * <p>이 값 이하의 점수를 가진 게시글은 실시간 인기글 목록에서 제거됩니다.</p>
     */
    public static final double REALTIME_POST_SCORE_THRESHOLD = 1.0;

    // ===================== 4. METADATA STRUCTURE (메타데이터 구조) =====================

    /**
     * 인기글 목록 캐시 메타데이터를 위한 내부 레코드
     * <p>각 PostCacheFlag 타입별로 Redis 키와 TTL을 관리합니다.</p>
     *
     * @param key Redis 키
     * @param ttl 캐시 만료 시간
     */
    public record CacheMetadata(String key, Duration ttl) {}

    // ===================== 5. CACHE METADATA MAP (캐시 타입별 메타데이터 맵) =====================

    /**
     * PostCacheFlag 유형별 목록 캐시 키와 TTL을 저장하는 맵
     * <p>REALTIME, WEEKLY, LEGEND, NOTICE 각 타입에 대한 캐시 메타데이터를 제공합니다.</p>
     */
    public static final Map<PostCacheFlag, CacheMetadata> CACHE_METADATA_MAP = initializeCacheMetadata();

    /**
     * <h3>캐시 메타데이터 맵 초기화</h3>
     * <p>각 PostCacheFlag 타입에 대한 Redis 키와 TTL을 설정합니다.</p>
     * <p>목록 캐시는 Hash 구조로 저장되며, Field는 postId, Value는 PostSimpleDetail입니다.</p>
     *
     * @return PostCacheFlag별 캐시 메타데이터 맵
     * @author Jaeik
     * @since 2.0.0
     */
    private static Map<PostCacheFlag, CacheMetadata> initializeCacheMetadata() {
        Map<PostCacheFlag, CacheMetadata> map = new EnumMap<>(PostCacheFlag.class);
        map.put(PostCacheFlag.REALTIME, new CacheMetadata("post:realtime:list", POST_CACHE_TTL));
        map.put(PostCacheFlag.WEEKLY, new CacheMetadata("post:weekly:list", POST_CACHE_TTL));
        map.put(PostCacheFlag.LEGEND, new CacheMetadata("post:legend:list", POST_CACHE_TTL));
        map.put(PostCacheFlag.NOTICE, new CacheMetadata("post:notice:list", POST_CACHE_TTL));
        return map;
    }

    // ===================== 6. LUA SCRIPT (Redis 스크립트) =====================

    /**
     * 실시간 인기글 점수 감쇠를 위한 Lua 스크립트
     * <p>Redis Sorted Set의 모든 게시글 점수에 SCORE_DECAY_RATE(0.9)를 곱합니다.</p>
     */
    public static final RedisScript<Long> SCORE_DECAY_SCRIPT;

    static {
        String luaScript =
            "local members = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES') " +
            "for i = 1, #members, 2 do " +
            "    local member = members[i] " +
            "    local score = tonumber(members[i + 1]) " +
            "    local newScore = score * tonumber(ARGV[1]) " +
            "    redis.call('ZADD', KEYS[1], newScore, member) " +
            "end " +
            "return redis.call('ZCARD', KEYS[1])";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        SCORE_DECAY_SCRIPT = script;
    }

    // ===================== 7. KEY GENERATION METHODS (키 생성 유틸리티) =====================

    /**
     * <h3>게시글 상세 캐시 키 생성</h3>
     * <p>게시글 ID를 사용하여 Redis 상세 캐시 키를 생성합니다.</p>
     *
     * @param postId 게시글 ID
     * @return 생성된 Redis 키 (형식: post:{postId}:detail)
     * @author Jaeik
     * @since 2.0.0
     */
    public static String getPostDetailKey(Long postId) {
        return POST_PREFIX + postId + FULL_POST_CACHE_SUFFIX;
    }

    /**
     * <h3>postId 목록 영구 저장소 키 생성</h3>
     * <p>캐시 타입별로 postId 목록을 영구 저장하기 위한 Redis 키를 생성합니다.</p>
     *
     * @param type 게시글 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @return 생성된 Redis 키 (형식: post:{type}:postids)
     */
    public static String getPostIdsStorageKey(PostCacheFlag type) {
        return POST_PREFIX + type.name().toLowerCase() + POST_IDS_SUFFIX;
    }

    /**
     * <h3>개별 게시글 캐시 키 생성 (Hash Tag 포함)</h3>
     * <p>Redis 클러스터에서 같은 타입의 게시글이 동일 슬롯에 배치됩니다.</p>
     * <p>예: post:{realtime}:simple:123</p>
     *
     * @param type   게시글 캐시 유형
     * @param postId 게시글 ID
     * @return 생성된 Redis 키
     */
    public static String getSimplePostKey(PostCacheFlag type, Long postId) {
        String typeTag = type.name().toLowerCase();
        return String.format(SIMPLE_POST_PREFIX, typeTag) + postId;
    }

    /**
     * <h3>개별 게시글 캐시 키 목록 생성</h3>
     * <p>여러 postId에 대한 캐시 키 목록을 생성합니다.</p>
     *
     * @param type    게시글 캐시 유형
     * @param postIds 게시글 ID 목록
     * @return 캐시 키 목록
     */
    public static java.util.List<String> getSimplePostKeys(PostCacheFlag type, java.util.List<Long> postIds) {
        return postIds.stream()
                .map(id -> getSimplePostKey(type, id))
                .toList();
    }
}
