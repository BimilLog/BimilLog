package jaeik.bimillog.infrastructure.redis.friend;

public class RedisFriendKeys {

    // 친구 관계 테이블 (Set) 키
    public static final String FRIEND_SHIP_PREFIX = "friend:";

    // 상호 작용 점수 테이블(ZSet) 키 접두사
    public static final String INTERACTION_PREFIX = "interaction:";

    // 상호 작용 점수 증가 기본 값
    public static final Double INTERACTION_SCORE_DEFAULT = 0.5;
}
