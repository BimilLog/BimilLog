package jaeik.bimillog.infrastructure.redis;

import java.time.Duration;

/**
 * Redis 키 & TTL 중앙 관리
 *
 * @author jaeik
 * @version 3.0.0
 */
public final class RedisKey {

    private RedisKey() {
    }

    // ==================== 공통 TTL ====================

    public static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24).plusMinutes(30);

    // ==================== 글 첫 페이지 (List) ====================

    public static final String FIRST_PAGE_LIST_KEY = "post:firstpage:ids";
    public static final int FIRST_PAGE_SIZE = 20;

    // ==================== 글 단위 Hash (per-post) ====================

    public static final String POST_SIMPLE_PREFIX = "post:simple:";               // Hash: 글 단위 캐시

    // ==================== 인덱스 (SET) ====================

    public static final String POST_WEEKLY_IDS_KEY = "post:weekly:ids";
    public static final String POST_LEGEND_IDS_KEY = "post:legend:ids";
    public static final String POST_NOTICE_IDS_KEY = "post:notice:ids";

    // ==================== 글 : 실시간 (ZSet) ====================

    public static final String REALTIME_POST_SCORE_KEY = "post:realtime:score";

    // ==================== 글 : 카운트 버퍼 (Hash) ====================
    // key: post:view:counts, post:like:counts, post:comment:counts

    public static final String VIEW_PREFIX = "post:view:";
    public static final String VIEW_COUNTS_KEY = "post:view:counts";
    public static final String LIKE_COUNTS_KEY = "post:like:counts";
    public static final String COMMENT_COUNTS_KEY = "post:comment:counts";
    public static final long VIEW_TTL_SECONDS = DEFAULT_CACHE_TTL.toSeconds();

    // ==================== 롤링페이퍼 : 실시간 (ZSet) ====================
    // key: paper:realtime:score

    public static final String REALTIME_PAPER_SCORE_KEY = "paper:realtime:score";

    // ==================== 인증 : JWT블랙리스트 (String) ====================
    // key: TemporaryToken:blacklist:{tokenHash}

    public static final String BLACKLIST_KEY_PREFIX = "TemporaryToken:blacklist:";
    public static final Duration BLACKLIST_DEFAULT_TTL = Duration.ofDays(30);

    // ==================== 회원 : 임시데이터 (String) ====================
    // key: temp:member:{uuid}

    public static final String TEMP_MEMBER_KEY_PREFIX = "temp:member:";
    public static final Duration TEMP_MEMBER_TTL = Duration.ofMinutes(5);

    // ==================== 친구 : 친구관계 (Set) ====================
    // key: friend:{memberId}  (TTL 없음 - 영구 저장)

    public static final String FRIENDSHIP_PREFIX = "friend:";

    // ==================== 친구 : 상호작용 점수 (ZSet + String) ====================
    // key: interaction:{memberId}  (TTL 없음 - 감쇠로 관리)
    // 멱등성 키: {idempotencyKey}  (SET NX EX)

    public static final String INTERACTION_PREFIX = "interaction:";
    public static final long IDEMPOTENCY_TTL_SECONDS = 60 * 60L;
}
