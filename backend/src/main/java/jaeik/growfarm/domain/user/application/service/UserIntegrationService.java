package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.in.UserIntegrationUseCase;
import jaeik.growfarm.domain.user.application.port.out.KakaoFriendPort;
import jaeik.growfarm.domain.user.application.port.out.UserPort;
import jaeik.growfarm.domain.user.application.port.out.TokenPort;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.dto.auth.KakaoFriendsResponse;
import jaeik.growfarm.dto.auth.KakaoFriendDTO;
import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>사용자 통합 서비스</h2>
 * <p>사용자와 관련된 외부 API 통합 기능을 처리하는 서비스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserIntegrationService implements UserIntegrationUseCase {

    private final KakaoFriendPort kakaoFriendPort;
    private final UserPort userPort;
    private final TokenPort tokenPort;

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>현재 로그인한 사용자의 카카오 친구 목록을 조회하고, 비밀로그 가입 여부를 확인합니다.</p>
     *
     * @param userId 사용자 ID
     * @param offset 조회 시작 위치 (기본값: 0)
     * @param limit  조회할 친구 수 (기본값: 10, 최대: 100)
     * @return 카카오 친구 목록 응답 (비밀로그 가입 여부 포함)
     * @throws CustomException 사용자를 찾을 수 없거나 카카오 API 오류 시
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public KakaoFriendsResponse getKakaoFriendList(Long userId, Integer offset, Integer limit) {
        // 사용자 조회
        User user = userPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자의 토큰 조회
        Token token = tokenPort.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_TOKEN));
        
        // 카카오 액세스 토큰 확인
        if (token.getAccessToken() == null || token.getAccessToken().isEmpty()) {
            throw new CustomException(ErrorCode.NOT_FIND_TOKEN);
        }

        // 기본값 설정
        int actualOffset = offset != null ? offset : 0;
        int actualLimit = limit != null ? Math.min(limit, 100) : 10;

        try {
            // 카카오 친구 목록 조회
            KakaoFriendsResponse friendsResponse = kakaoFriendPort.getFriendList(
                    token.getAccessToken(),
                    actualOffset,
                    actualLimit
            );

            // 각 친구에 대해 비밀로그 가입 여부 확인
            List<KakaoFriendDTO> elements = friendsResponse.getElements();
            if (elements != null && !elements.isEmpty()) {
                for (KakaoFriendDTO friend : elements) {
                    // 카카오 소셜 ID로 비밀로그 가입자 찾기
                    userPort.findByProviderAndSocialId(SocialProvider.KAKAO, String.valueOf(friend.getId()))
                            .ifPresent(registeredUser -> friend.setUserName(registeredUser.getUserName()));
                }
            }

            return friendsResponse;

        } catch (CustomException e) {
            // 카카오 친구 동의 필요한 경우 특별한 에러 메시지 처리
            if (e.getMessage().contains("consent")) {
                throw new CustomException(ErrorCode.KAKAO_FRIEND_CONSENT_FAIL);
            }
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.KAKAO_API_ERROR, e);
        }
    }
}