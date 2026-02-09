package jaeik.bimillog.infrastructure.redis;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 키 & TTL 중앙 관리
 *
 * @author jaeik
 * @version 2.5.0
 */
public final class RedisKey {

    private RedisKey() {
    }

    // ==================== 글 첫 페이지 (List) ====================

    public static final String FIRST_PAGE_LIST_KEY = "post:board:first-page";
    public static final String FIRST_PAGE_REFRESH_LOCK_KEY = "post:board:refresh:lock";
    public static final Duration FIRST_PAGE_CACHE_TTL = Duration.ofHours(24);
    public static final Duration FIRST_PAGE_REFRESH_LOCK_TTL = Duration.ofMinutes(5);
    public static final int FIRST_PAGE_SIZE = 20;

    // ==================== 캐시글 목록 (Hash) ====================

    public static final String WEEKLY_SIMPLE_KEY = "post:weekly:simple";           // TTL 24시간 30분
    public static final String LEGEND_SIMPLE_KEY = "post:legend:simple";           // TTL 24시간 30분
    public static final String NOTICE_SIMPLE_KEY = "post:notice:simple";           // TTL 없음 (영구)
    public static final Duration POST_CACHE_TTL_WEEKLY_LEGEND = Duration.ofHours(24).plusMinutes(30);

    public static final String SCHEDULER_LOCK_KEY = "post:cache:scheduler:lock";
    public static final Duration SCHEDULER_LOCK_TTL = Duration.ofSeconds(90);

    // ==================== 글 : 실시간 (ZSet) ====================

    public static final String REALTIME_POST_SCORE_KEY = "post:realtime:score";
    public static final String REALTIME_SIMPLE_KEY = "post:realtime:simple";       // TTL 없음 (영구)
    public static final String REALTIME_REFRESH_LOCK_KEY = "post:realtime:refresh:lock";
    public static final Duration REALTIME_REFRESH_LOCK_TTL = Duration.ofSeconds(10);



    // ==================== 글 : 조회수 (Set + Hash) ====================
    // key: post:view:{postId}, post:view:counts

    public static final String VIEW_PREFIX = "post:view:";
    public static final String VIEW_COUNTS_KEY = "post:view:counts";
    public static final long VIEW_TTL_SECONDS = TimeUnit.HOURS.toSeconds(24);

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
