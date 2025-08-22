package jaeik.growfarm.infrastructure.adapter.user.out.social;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.KakaoFriendsResponse;

/**
 * <h2>소셜 어댑터 인터페이스</h2>
 * <p>소셜 제공자별 부가 기능(친구 목록, 프로필 정보 등)을 추상화한 인터페이스</p>
 * <p>인증(로그인) 기능과 분리하여 소셜 제공자의 추가 기능을 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public interface SocialAdapter {

    /**
     * <h3>소셜 제공자 반환</h3>
     * <p>해당 어댑터가 지원하는 소셜 제공자를 반환합니다.</p>
     *
     * @return SocialProvider 지원하는 소셜 제공자
     * @since 2.0.0
     * @author Jaeik
     */
    SocialProvider getProvider();

    /**
     * <h3>친구 목록 조회</h3>
     * <p>소셜 제공자의 친구 목록을 조회합니다.</p>
     *
     * @param accessToken 액세스 토큰
     * @param offset      조회 시작 위치
     * @param limit       조회할 친구 수
     * @return KakaoFriendsResponse 친구 목록 응답
     * @since 2.0.0
     * @author Jaeik
     */
    KakaoFriendsResponse getFriendList(String accessToken, Integer offset, Integer limit);
}