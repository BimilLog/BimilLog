package jaeik.bimillog.domain.global.strategy;

import jaeik.bimillog.domain.member.entity.KakaoFriends;
import jaeik.bimillog.domain.member.entity.SocialProvider;

/**
 * <h2>소셜 친구 전략</h2>
 * <p>플랫폼별 친구 목록 조회 등 추가 기능을 정의합니다.</p>
 */
public interface SocialFriendStrategy {

    /**
     * 지원하는 소셜 제공자를 반환합니다.
     *
     * @return 소셜 제공자
     */
    SocialProvider getProvider();

    /**
     * 친구 목록을 조회합니다.
     *
     * @param accessToken 소셜 플랫폼 액세스 토큰
     * @param offset 조회 시작 위치
     * @param limit 조회할 수량
     * @return 친구 목록 응답
     */
    KakaoFriends getFriendList(String accessToken, Integer offset, Integer limit);
}
