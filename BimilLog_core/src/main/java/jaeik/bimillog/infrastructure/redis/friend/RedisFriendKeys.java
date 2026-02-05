package jaeik.bimillog.infrastructure.redis.friend;

public class RedisFriendKeys {

    // 친구 관계 테이블 (Set) 키
    public static final String FRIEND_SHIP_PREFIX = "friend:";

    // 상호 작용 점수 테이블(ZSet) 키 접두사
    public static final String INTERACTION_PREFIX = "interaction:";

    // 상호 작용 점수 증가 가능 최대값 (최대값은 10점)
    public static final Double INTERACTION_SCORE_LIMIT = 9.5;

    // 상호 작용 점수 증가 기본 값
    public static final Double INTERACTION_SCORE_DEFAULT = 0.5;

    // 멱등성 보장을 위한 처리된 이벤트 Set 키
    public static final String IDEMPOTENCY_PREFIX = "idempotency:interaction:";

    // 멱등성 키 TTL (1일, 초 단위)
    public static final Long IDEMPOTENCY_TTL_SECONDS = 24 * 60 * 60L;

    // 상호 작용 점수 지수 감쇠율 (1일마다 0.95 곱하기)
    public static final Double INTERACTION_SCORE_DECAY_RATE = 0.95;

    // 상호 작용 점수 삭제 임계값 (이 점수 이하면 삭제)
    public static final Double INTERACTION_SCORE_THRESHOLD = 0.1;

}
