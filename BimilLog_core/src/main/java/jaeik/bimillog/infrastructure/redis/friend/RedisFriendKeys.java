package jaeik.bimillog.infrastructure.redis.friend;

public class RedisFriendKeys {

    // 친구 관계 테이블 (Set) 키
    public static final String FRIEND_SHIP_PREFIX = "friend:";

    // 상호 작용 점수 테이블(Hash) 키 접두사
    public static final String INTERACTION_PREFIX = "interaction:";

    // 상호 작용 점수 테이블(Hash) 필드 접두사
    public static final String INTERACTION_SUFFIX = "member:";

    // 상호 작용 점수 증가 가능 최대값 (최대값은 10점)
    public static final Double INTERACTION_SCORE_LIMIT = 9.5;

    // 상호 작용 점수 증가 기본 값
    public static final Double INTERACTION_SCORE_DEFAULT= 0.5;


}
