package jaeik.bimillog.infrastructure.adapter.out.api.social.kakao;

import jaeik.bimillog.domain.global.application.strategy.SocialFriendStrategy;
import jaeik.bimillog.domain.member.entity.KakaoFriendsResponseVO;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.infrastructure.adapter.out.api.dto.KakaoFriendsDTO;
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

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public KakaoFriendsResponseVO getFriendList(String accessToken, Integer offset, Integer limit) {
        try {
            KakaoFriendsDTO response = kakaoApiClient.getFriends("Bearer " + accessToken, offset, limit);
            return response != null ? response.toVO() : null;
        } catch (Exception e) {
            throw new MemberCustomException(MemberErrorCode.KAKAO_FRIEND_API_ERROR, e);
        }
    }
}
