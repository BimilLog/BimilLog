package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.auth.entity.JwtToken;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.domain.user.application.port.in.UserFriendUseCase;
import jaeik.bimillog.domain.user.application.port.out.KakaoFriendPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.KakaoFriendsResponseVO;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class UserFriendService implements UserFriendUseCase {

    private final KakaoFriendPort kakaoFriendPort;
    private final UserQueryPort userQueryPort;
    private final GlobalTokenQueryPort globalTokenQueryPort;
    private final GlobalKakaoTokenQueryPort globalKakaoTokenQueryPort;

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>현재 로그인한 사용자의 카카오 친구 목록을 조회하고, 비밀로그 가입 여부를 확인합니다.</p>
     * <p><strong>성능 최적화:</strong> findUserNamesInOrder 메소드를 사용하여 배치 조회로 N+1 문제를 해결합니다.</p>
     *
     * @param userId   사용자 ID
     * @param offset   조회 시작 위치 (기본값: 0)
     * @param limit    조회할 친구 수 (기본값: 10, 최대: 100)
     * @param tokenId  현재 요청 기기 토큰 ID
     * @return KakaoFriendsResponseVO 카카오 친구 목록 응답 (비밀로그 가입 여부 포함)
     * @throws CustomException 사용자를 찾을 수 없거나 카카오 API 오류 시
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional(readOnly = true)
    public KakaoFriendsResponseVO getKakaoFriendList(Long userId, Long tokenId, Integer offset, Integer limit) {
        // 기본값 설정
        int actualOffset = offset != null ? offset : 0;
        int actualLimit = limit != null ? Math.min(limit, 100) : 10;

        try {
            // 1. 현재 요청 기기의 JwtToken 조회 (userId 추출용)
            JwtToken jwtToken = globalTokenQueryPort.findById(tokenId)
                    .orElseThrow(() -> new AuthCustomException(AuthErrorCode.NOT_FIND_TOKEN));

            // 2. KakaoToken 조회
            KakaoToken kakaoToken = globalKakaoTokenQueryPort.findByUserId(jwtToken.getUsers().getId())
                    .orElseThrow(() -> new AuthCustomException(AuthErrorCode.NOT_FIND_TOKEN));

            // 카카오 액세스 토큰 확인
            if (kakaoToken.getKakaoAccessToken() == null || kakaoToken.getKakaoAccessToken().isEmpty()) {
                throw new AuthCustomException(AuthErrorCode.NOT_FIND_TOKEN);
            }

            // 3. 카카오 친구 목록 조회
            KakaoFriendsResponseVO response = kakaoFriendPort.getFriendList(
                    kakaoToken.getKakaoAccessToken(), actualOffset, actualLimit);
            
            // 4. 친구 목록 처리 (비밀로그 가입 여부 체크)
            return processFriendList(response);
            
        } catch (UserCustomException e) {
            // 카카오 친구 동의 필요한 경우 특별한 에러 메시지 처리
            if (e.getUserErrorCode() == UserErrorCode.KAKAO_FRIEND_API_ERROR) {
                throw new UserCustomException(UserErrorCode.KAKAO_FRIEND_CONSENT_FAIL);
            }
            throw e;
        } catch (Exception e) {
            // UserCustomException은 그대로 전파
            if (e instanceof UserCustomException) {
                throw e;
            }
            log.error("카카오 API 호출 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new UserCustomException(UserErrorCode.KAKAO_FRIEND_API_ERROR, e);
        }
    }

    /**
     * <h3>친구 목록에 비밀로그 가입 정보 매핑</h3>
     * <p>카카오 친구 목록 응답을 받아 비밀로그 가입 여부와 사용자 이름을 매핑합니다.</p>
     *
     * @param friendsResponse 카카오 API에서 받은 친구 목록 응답
     * @return 비밀로그 가입 정보가 추가된 새로운 응답 객체
     */
    private KakaoFriendsResponseVO processFriendList(KakaoFriendsResponseVO friendsResponse) {
        List<KakaoFriendsResponseVO.Friend> elements = friendsResponse.elements();
        if (elements == null || elements.isEmpty()) {
            return friendsResponse;
        }

        // 1. 모든 친구의 소셜 ID를 수집
        List<String> socialIds = elements.stream()
                .map(friend -> String.valueOf(friend.id()))
                .collect(Collectors.toList());

        // 2. 배치로 사용자 이름 조회 (N+1 문제 해결)
        List<String> userNames = userQueryPort.findUserNamesInOrder(socialIds);

        // 3. 결과를 각 친구에게 매핑 (Stream API 활용)
        List<KakaoFriendsResponseVO.Friend> updatedElements = elements.stream()
                .map(originalFriend -> {
                    // 순서가 보장되므로 인덱스로 사용자 이름 조회
                    int index = elements.indexOf(originalFriend);
                    String userName = userNames.get(index);
                    if (!userName.isEmpty()) {
                        // 가입된 친구인 경우 userName 필드 업데이트
                        return KakaoFriendsResponseVO.Friend.of(
                                originalFriend.id(),
                                originalFriend.uuid(),
                                originalFriend.profileNickname(),
                                originalFriend.profileThumbnailImage(),
                                originalFriend.favorite(),
                                userName
                        );
                    }
                    return originalFriend;
                })
                .toList();

        // 4. 새로운 응답 객체 생성
        return KakaoFriendsResponseVO.of(
                updatedElements,
                friendsResponse.totalCount(),
                friendsResponse.beforeUrl(),
                friendsResponse.afterUrl(),
                friendsResponse.favoriteCount()
        );
    }
}
