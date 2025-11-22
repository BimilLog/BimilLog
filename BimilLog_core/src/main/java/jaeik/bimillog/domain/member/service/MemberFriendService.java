package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.member.dto.KakaoFriendsDTO;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.out.MemberQueryAdapter;
import jaeik.bimillog.domain.member.out.MemberToAuthAdapter;
import jaeik.bimillog.infrastructure.api.social.kakao.KakaoFriendClient;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>회원 친구 서비스</h2>
 * <p>카카오 친구 목록을 조회하고 비밀로그 회원 정보와 매핑합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberFriendService {

    private final MemberQueryAdapter memberQueryAdapter;
    private final MemberToAuthAdapter memberToAuthAdapter;
    private final KakaoFriendClient kakaoFriendClient;

    /**
     * 친구 추가 정보 조회
     */
    public List<Friend.FriendInfo> addMyFriendInfo(List<Long> friendIds) {
        return memberQueryAdapter.getMyFriendPages(friendIds);
    }

    /**
     * 추천 친구 추가 정보 조회
     */
    public List<RecommendedFriend.RecommendedFriendInfo> addRecommendedFriendInfo(List<Long> friendIds) {
        return memberQueryAdapter.addRecommendedFriendInfo(friendIds);
    }

    // 추천 친구 아는 사람 추가 정보 조회
    public List<RecommendedFriend.AcquaintanceInfo> addAcquaintanceInfo(List<Long> acquaintanceIds) {
        return memberQueryAdapter.addAcquaintanceInfo(acquaintanceIds);
    }


    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>현재 로그인한 회원의 카카오 친구 목록을 조회하고 가입 여부를 확인합니다.</p>
     */
    @Transactional(readOnly = true)
    public KakaoFriendsDTO getKakaoFriendList(Long memberId, SocialProvider provider, long offset, Limit limit) {
        int actualLimit = limit != null ? Math.min(limit.max(), 100) : 10;

        try {
            if (provider != SocialProvider.KAKAO) {
                throw new CustomException(ErrorCode.MEMBER_UNSUPPORTED_SOCIAL_FRIEND);
            }

            SocialToken socialToken = memberToAuthAdapter.getSocialToken(memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SOCIAL_TOKEN_NOT_FOUNT));

            KakaoFriendsDTO response = kakaoFriendClient.getFriendList(
                    socialToken.getAccessToken(), (int) offset, actualLimit
            );

            return processFriendList(response);
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.MEMBER_KAKAO_FRIEND_API_ERROR) {
                throw new CustomException(ErrorCode.MEMBER_KAKAO_FRIEND_CONSENT_FAIL);
            }
            throw e;
        } catch (Exception e) {
            log.error("카카오 친구 API 호출 중 예기치 못한 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.MEMBER_KAKAO_FRIEND_API_ERROR, e);
        }
    }

    private KakaoFriendsDTO processFriendList(KakaoFriendsDTO friendsResponse) {
        if (friendsResponse == null) {
            return null;
        }

        List<KakaoFriendsDTO.KakaoFriend> elements = friendsResponse.getElements();
        if (elements == null || elements.isEmpty()) {
            return friendsResponse;
        }

        List<String> socialIds = elements.stream()
                .map(friend -> String.valueOf(friend.getId()))
                .toList();

        List<String> memberNames = memberQueryAdapter.findMemberNamesInOrder(socialIds);

        for (int index = 0; index < elements.size(); index++) {
            KakaoFriendsDTO.KakaoFriend originalKakaoFriend = elements.get(index);
            String memberName = (memberNames != null && memberNames.size() > index)
                    ? memberNames.get(index)
                    : "";

            if (memberName == null || memberName.isEmpty()) {
                continue;
            }

            originalKakaoFriend.setMemberName(memberName);
        }

        return friendsResponse;
    }
}
