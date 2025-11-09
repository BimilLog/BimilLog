package jaeik.bimillog.infrastructure.api.social.kakao;

import jaeik.bimillog.domain.global.strategy.SocialFriendStrategy;
import jaeik.bimillog.domain.member.entity.KakaoFriends;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.infrastructure.api.dto.KakaoFriendsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>카카오 친구 전략</h2>
 * <p>카카오 친구 API 호출을 담당하는 전략 구현체입니다.</p>
 */
@Component
@RequiredArgsConstructor
public class KakaoFriendStrategy implements SocialFriendStrategy {

    private final KakaoApiClient kakaoApiClient;

    /**
     * <h3>소셜 제공자 반환</h3>
     * <p>이 전략이 지원하는 소셜 제공자를 반환합니다.</p>
     *
     * @return KAKAO 제공자
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>액세스 토큰을 사용하여 사용자의 카카오 친구 목록을 조회합니다.</p>
     * <p>페이지네이션을 지원하며, offset과 limit을 통해 조회 범위를 제어할 수 있습니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @param offset 조회 시작 위치 (0부터 시작)
     * @param limit 조회할 친구 수
     * @return 친구 목록 응답 VO
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public KakaoFriends getFriendList(String accessToken, Integer offset, Integer limit) {
        try {
            KakaoFriendsDTO response = kakaoApiClient.getFriends("Bearer " + accessToken, offset, limit);
            return response != null ? response.toVO() : null;
        } catch (Exception e) {
            throw new MemberCustomException(MemberErrorCode.KAKAO_FRIEND_API_ERROR, e);
        }
    }
}
