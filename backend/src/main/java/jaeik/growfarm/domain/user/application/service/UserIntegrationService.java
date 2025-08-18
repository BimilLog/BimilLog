package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.in.UserIntegrationUseCase;
import jaeik.growfarm.domain.user.application.port.out.KakaoFriendPort;
import jaeik.growfarm.domain.user.application.port.out.UserQueryPort;
import jaeik.growfarm.domain.user.application.port.out.TokenPort;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.infrastructure.adapter.user.out.social.dto.KakaoFriendsResponse;
import jaeik.growfarm.infrastructure.adapter.user.out.social.dto.KakaoFriendDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    private final UserQueryPort userQueryPort;
    private final TokenPort tokenPort;

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>현재 로그인한 사용자의 카카오 친구 목록을 조회하고, 비밀로그 가입 여부를 확인합니다.</p>
     * <p><strong>성능 최적화:</strong> findUserNamesInOrder 메소드를 사용하여 배치 조회로 N+1 문제를 해결합니다.</p>
     * <ul>
     *   <li><strong>기존 방식:</strong> 각 친구마다 개별 쿼리 실행 (친구 10명 = 10번 쿼리)</li>
     *   <li><strong>최적화된 방식:</strong> 모든 친구를 한 번에 배치 조회 (친구 10명 = 1번 쿼리)</li>
     * </ul>
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
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자의 토큰 조회
        Token token = tokenPort.findByUsers(user)
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

            // 각 친구에 대해 비밀로그 가입 여부 확인 (성능 최적화: 배치 조회)
            List<KakaoFriendDTO> elements = friendsResponse.getElements();
            if (elements != null && !elements.isEmpty()) {
                // 1. 모든 친구의 소셜 ID를 수집
                List<String> socialIds = elements.stream()
                        .map(friend -> String.valueOf(friend.getId()))
                        .collect(Collectors.toList());

                // 2. 배치로 사용자 이름 조회 (N+1 문제 해결)
                List<String> userNames = userQueryPort.findUserNamesInOrder(socialIds);

                // 3. 결과를 각 친구에게 매핑
                for (int i = 0; i < elements.size(); i++) {
                    String userName = userNames.get(i);
                    if (!userName.isEmpty()) {
                        elements.get(i).setUserName(userName);
                    }
                }
            }

            return friendsResponse;

        } catch (CustomException e) {
            // 카카오 친구 동의 필요한 경우 특별한 에러 메시지 처리
            if (e.getErrorCode() == ErrorCode.KAKAO_API_ERROR) {
                throw new CustomException(ErrorCode.KAKAO_FRIEND_CONSENT_FAIL);
            }
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.KAKAO_API_ERROR, e);
        }
    }
}