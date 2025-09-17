package jaeik.bimillog.infrastructure.adapter.social.kakao;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.adapter.social.SocialAdapter;
import jaeik.bimillog.infrastructure.adapter.user.dto.KakaoFriendsDTO;
import org.springframework.stereotype.Component;

/**
 * <h2>카카오 소셜 어댑터</h2>
 * <p>카카오의 소셜 기능(친구 목록 조회 등)을 담당하는 어댑터</p>
 * <p>인증(로그인) 기능과 분리하여 카카오의 부가 기능만 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class KakaoSocialAdapter implements SocialAdapter {

    private final KakaoApiClient kakaoApiClient;

    public KakaoSocialAdapter(KakaoApiClient kakaoApiClient) {
        this.kakaoApiClient = kakaoApiClient;
    }

    /**
     * <h3>소셜 제공자 반환</h3>
     * <p>카카오 제공자를 반환합니다.</p>
     *
     * @return SocialProvider.KAKAO
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>카카오 API를 통해 사용자의 친구 목록을 조회합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @param offset      조회 시작 위치 (기본값: 0)
     * @param limit       조회할 친구 수 (기본값: 10, 최대: 100)
     * @return KakaoFriendsDTO 친구 목록 응답
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public KakaoFriendsDTO getFriendList(String accessToken, Integer offset, Integer limit) {
        try {
            return kakaoApiClient.getFriends("Bearer " + accessToken, offset, limit);
        } catch (Exception e) {
            throw new UserCustomException(UserErrorCode.KAKAO_FRIEND_API_ERROR, e);
        }
    }
}