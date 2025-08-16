package jaeik.growfarm.infrastructure.adapter.user.out.social;

import jaeik.growfarm.infrastructure.adapter.auth.out.social.KakaoLoginStrategy;
import jaeik.growfarm.domain.user.application.port.out.KakaoFriendPort;
import jaeik.growfarm.infrastructure.adapter.user.out.social.dto.KakaoFriendsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>카카오 친구 목록 어댑터</h2>
 * <p>카카오 API를 통한 친구 목록 조회 기능을 구현하는 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class KakaoFriendAdapter implements KakaoFriendPort {

    private final KakaoLoginStrategy kakaoLoginStrategy;

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>카카오 로그인 전략을 통해 친구 목록을 조회합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @param offset      조회 시작 위치
     * @param limit       조회할 친구 수
     * @return 카카오 친구 목록 응답
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public KakaoFriendsResponse getFriendList(String accessToken, Integer offset, Integer limit) {
        return kakaoLoginStrategy.getFriendList(accessToken, offset, limit);
    }
}