package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.member.out.MemberToAuthAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.domain.member.dto.KakaoFriendsDTO;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.out.MemberQueryAdapter;
import jaeik.bimillog.infrastructure.api.social.kakao.KakaoFriendClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>사용자 친구 서비스</h2>
 * <p>사용자와 친구 기능을 처리하는 서비스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberFriendService {
    private final MemberQueryAdapter memberQueryAdapter;
    private final MemberToAuthAdapter memberToAuthAdapter;
    private final KakaoFriendClient kakaoFriendClient;

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>현재 로그인한 사용자의 카카오 친구 목록을 조회하고, 비밀로그 가입 여부를 확인합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param offset   조회 시작 위치 (기본값: 0)
     * @param limit    조회할 친구 수 (기본값: 10, 최대: 100)
     * @return KakaoFriendsDTO 카카오 친구 목록 응답 (비밀로그 가입 여부 포함)
     * @throws CustomException 사용자를 찾을 수 없거나 카카오 API 오류 시
     * @since 2.0.0
     * @author Jaeik
     */
    @Transactional(readOnly = true)
    public KakaoFriendsDTO getKakaoFriendList(Long memberId, SocialProvider provider, long offset, Limit limit) {
        // 기본값 설정
        int actualLimit = limit != null ? Math.min(limit.max(), 100) : 10;

        try {
            if (provider != SocialProvider.KAKAO) {
                throw new CustomException(ErrorCode.MEMBER_UNSUPPORTED_SOCIAL_FRIEND);
            }

            // 소셜 토큰 조회
            SocialToken socialToken = memberToAuthAdapter.getSocialToken(memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SOCIAL_TOKEN_NOT_FOUNT));

            KakaoFriendsDTO response = kakaoFriendClient.getFriendList(
                    socialToken.getAccessToken(), (int) offset, actualLimit);

            return processFriendList(response);
            
        } catch (CustomException e) {
            // 카카오 친구 동의 필요한 경우
            if (e.getErrorCode() == ErrorCode.MEMBER_KAKAO_FRIEND_API_ERROR) {
                throw new CustomException(ErrorCode.MEMBER_KAKAO_FRIEND_CONSENT_FAIL);
            }
            throw e;
        } catch (Exception e) {
            log.error("카카오 API 호출 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.MEMBER_KAKAO_FRIEND_API_ERROR, e);
        }
    }

    /**
     * <h3>친구 목록에 비밀로그 가입 정보 매핑</h3>
     * <p>카카오 친구 목록 응답을 받아 비밀로그 가입 여부와 사용자 이름을 매핑합니다.</p>
     *
     * @param friendsResponse 카카오 API에서 받은 친구 목록 응답
     * @return 비밀로그 가입 정보가 추가된 새로운 응답 객체
     */
    private KakaoFriendsDTO processFriendList(KakaoFriendsDTO friendsResponse) {
        if (friendsResponse == null) {
            return null;
        }

        List<KakaoFriendsDTO.Friend> elements = friendsResponse.getElements();
        if (elements == null || elements.isEmpty()) {
            return friendsResponse;
        }

        List<String> socialIds = elements.stream()
                .map(friend -> String.valueOf(friend.getId()))
                .toList();

        List<String> memberNames = memberQueryAdapter.findMemberNamesInOrder(socialIds);

        for (int index = 0; index < elements.size(); index++) {
            KakaoFriendsDTO.Friend originalFriend = elements.get(index);
            String memberName = (memberNames != null && memberNames.size() > index)
                    ? memberNames.get(index)
                    : "";

            if (memberName == null || memberName.isEmpty()) {
                continue;
            }

            originalFriend.setMemberName(memberName);
        }

        return friendsResponse;
    }
}
