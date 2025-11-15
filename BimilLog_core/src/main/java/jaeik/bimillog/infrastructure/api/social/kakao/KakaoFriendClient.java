package jaeik.bimillog.infrastructure.api.social.kakao;

import jaeik.bimillog.domain.member.entity.KakaoFriends;
import jaeik.bimillog.infrastructure.api.dto.KakaoFriendsDTO;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>카카오 친구 클라이언트</h2>
 * <p>카카오 친구 API 호출을 담당하는 인프라 서비스입니다.</p>
 */
@Component
@RequiredArgsConstructor
public class KakaoFriendClient {

    private final KakaoApiClient kakaoApiClient;

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>액세스 토큰을 사용하여 사용자의 카카오 친구 목록을 조회합니다.</p>
     * <p>페이지네이션을 지원하며, offset과 limit을 통해 조회 범위를 제어할 수 있습니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @param offset 조회 시작 위치 (0부터 시작)
     * @param limit 조회할 친구 수
     * @return 친구 목록 응답 VO
     */
    public KakaoFriends getFriendList(String accessToken, Integer offset, Integer limit) {
        try {
            KakaoFriendsDTO response = kakaoApiClient.getFriends("Bearer " + accessToken, offset, limit);
            return response != null ? response.toVO() : null;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.MEMBER_KAKAO_FRIEND_API_ERROR, e);
        }
    }
}
