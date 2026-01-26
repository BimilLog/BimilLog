package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;

import java.time.Duration;

/**
 * <h2>게시글 Redis 키</h2>
 * <p>게시글 관련 모든 Redis 키 정의, TTL, 스코어링 상수 및 캐시 메타데이터를 관리합니다.</p>
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
     * 게시글 목록 캐시 Hash 키 접미사
     * <p>Value Type: Hash (field=postId, value=PostSimpleDetail)</p>
     * <p>전체 키 형식: post:{type}:simple</p>
     */
    public static final String SIMPLE_POST_HASH_SUFFIX = ":simple";

    /**
     * 점수 저장소 키 접미사
     * <p>Value Type: Sorted Set (postId, score)</p>
     * <p>전체 키 형식: post:{type}:score</p>
     */
    public static final String SCORE_SUFFIX = ":score";

    /**
     * 실시간 인기글 점수 Sorted Set 키
     * <p>Value Type: ZSet (postId, score)</p>
     */
    public static final String REALTIME_POST_SCORE_KEY = "post:realtime:score";

    // ===================== 2. TTL (Time To Live, 만료 시간) =====================

    /**
     * 실시간 인기글 캐시 TTL (1분)
     */
    public static final Duration POST_CACHE_TTL_REALTIME = Duration.ofMinutes(1);

    /**
     * 주간/레전드 인기글 캐시 TTL (1분)
     * <p>Hash 캐시에 직접 적용</p>
     */
    public static final Duration POST_CACHE_TTL_WEEKLY_LEGEND = Duration.ofMinutes(1);

    /**
     * 공지사항 캐시 TTL (5분)
     */
    public static final Duration POST_CACHE_TTL_NOTICE = Duration.ofMinutes(5);


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

    // ===================== 4. LOCK (분산 락) =====================

    /**
     * 캐시 갱신 락 키 접미사
     * <p>전체 키 형식: post:{type}:refresh:lock</p>
     */
    public static final String REFRESH_LOCK_SUFFIX = ":refresh:lock";

    /**
     * 캐시 갱신 락 TTL (5초)
     */
    public static final Duration REFRESH_LOCK_TTL = Duration.ofSeconds(5);

    // ===================== 5. KEY GENERATION METHODS (키 생성 유틸리티) =====================

    /**
     * <h3>점수 저장소 키 생성</h3>
     * <p>실시간 인기글 점수를 저장하기 위한 Redis Sorted Set 키를 생성합니다.</p>
     *
     * @param type 게시글 캐시 유형 (REALTIME만 지원)
     * @return 생성된 Redis 키 (형식: post:realtime:score)
     * @throws IllegalArgumentException REALTIME 이외의 타입이 전달된 경우
     */
    public static String getScoreStorageKey(PostCacheFlag type) {
        if (type != PostCacheFlag.REALTIME) {
            throw new IllegalArgumentException("점수 저장소는 REALTIME 타입만 지원합니다: " + type);
        }
        return POST_PREFIX + type.name().toLowerCase() + SCORE_SUFFIX;
    }

    /**
     * <h3>게시글 목록 캐시 Hash 키 생성</h3>
     * <p>타입별로 하나의 Hash 키를 생성합니다.</p>
     * <p>예: post:weekly:simple, post:realtime:simple</p>
     *
     * @param type 게시글 캐시 유형
     * @return 생성된 Hash 키 (형식: post:{type}:simple)
     */
    public static String getSimplePostHashKey(PostCacheFlag type) {
        return POST_PREFIX + type.name().toLowerCase() + SIMPLE_POST_HASH_SUFFIX;
    }

    /**
     * <h3>캐시 갱신 락 키 생성</h3>
     * <p>타입별 분산 락 키를 생성합니다.</p>
     * <p>예: post:weekly:refresh:lock, post:realtime:refresh:lock</p>
     *
     * @param type 게시글 캐시 유형
     * @return 생성된 락 키 (형식: post:{type}:refresh:lock)
     */
    public static String getRefreshLockKey(PostCacheFlag type) {
        return POST_PREFIX + type.name().toLowerCase() + REFRESH_LOCK_SUFFIX;
    }
}
