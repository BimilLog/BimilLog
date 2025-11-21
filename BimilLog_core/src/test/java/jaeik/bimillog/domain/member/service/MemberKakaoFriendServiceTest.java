package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.out.MemberQueryAdapter;
import jaeik.bimillog.domain.member.out.MemberToAuthAdapter;
import jaeik.bimillog.domain.member.dto.KakaoFriendsDTO;
import jaeik.bimillog.infrastructure.api.social.kakao.KakaoFriendClient;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.builder.KakaoTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Limit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("unit")
class MemberKakaoFriendServiceTest extends BaseUnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final long DEFAULT_OFFSET = 0L;
    private static final int DEFAULT_LIMIT_VALUE = 10;
    private static final Limit DEFAULT_LIMIT = Limit.of(DEFAULT_LIMIT_VALUE);

    @Mock
    private MemberQueryAdapter memberQueryAdapter;

    @Mock
    private MemberToAuthAdapter memberToAuthAdapter;

    @Mock
    private KakaoFriendClient kakaoFriendClient;

    @InjectMocks
    private MemberFriendService memberFriendService;

    private final Member testMember = TestMembers.copyWithId(TestMembers.MEMBER_1, MEMBER_ID);

    @Test
    @DisplayName("카카오 친구 목록 조회 시 가입자 이름을 매핑한다")
    void shouldMapMemberNamesFromStrategyResult() {
        List<KakaoFriendsDTO.KakaoFriend> kakaoFriends = Arrays.asList(
                KakaoTestDataBuilder.createKakaoFriend(1L, "uuid-1", "친구1", "img1", false),
                KakaoTestDataBuilder.createKakaoFriend(2L, "uuid-2", "친구2", "img2", true)
        );
        KakaoFriendsDTO response = KakaoTestDataBuilder.createKakaoFriendsResponse(kakaoFriends, 2, null, null, 1);
        SocialToken socialToken = testMember.getSocialToken();

        given(memberToAuthAdapter.getSocialToken(MEMBER_ID)).willReturn(Optional.of(socialToken));
        given(kakaoFriendClient.getFriendList(socialToken.getAccessToken(), (int) DEFAULT_OFFSET, DEFAULT_LIMIT_VALUE)).willReturn(response);
        given(memberQueryAdapter.findMemberNamesInOrder(Arrays.asList("1", "2"))).willReturn(Arrays.asList("user1", "user2"));

        KakaoFriendsDTO result = memberFriendService.getKakaoFriendList(MEMBER_ID, SocialProvider.KAKAO, DEFAULT_OFFSET, DEFAULT_LIMIT);

        assertThat(result.getElements()).extracting(KakaoFriendsDTO.KakaoFriend::getMemberName)
                .containsExactly("user1", "user2");
        verify(memberQueryAdapter).findMemberNamesInOrder(Arrays.asList("1", "2"));
    }

    @Test
    @DisplayName("친구 목록이 비어 있으면 이름 조회를 생략한다")
    void shouldSkipMemberLookupWhenNoFriends() {
        KakaoFriendsDTO emptyResponse = KakaoTestDataBuilder.createKakaoFriendsResponse(Collections.emptyList(), 0, null, null, 0);
        SocialToken socialToken = testMember.getSocialToken();

        given(memberToAuthAdapter.getSocialToken(MEMBER_ID)).willReturn(Optional.of(socialToken));
        given(kakaoFriendClient.getFriendList(socialToken.getAccessToken(), (int) DEFAULT_OFFSET, DEFAULT_LIMIT_VALUE)).willReturn(emptyResponse);

        KakaoFriendsDTO result = memberFriendService.getKakaoFriendList(MEMBER_ID, SocialProvider.KAKAO, DEFAULT_OFFSET, DEFAULT_LIMIT);

        assertThat(result.getElements()).isEmpty();
        verify(memberQueryAdapter, never()).findMemberNamesInOrder(anyList());
    }

    @Test
    @DisplayName("limit이 null이면 기본값 10을 사용한다")
    void shouldUseDefaultPaginationWhenNull() {
        KakaoFriendsDTO response = KakaoTestDataBuilder.createKakaoFriendsResponse(Collections.emptyList(), 0, null, null, 0);
        SocialToken socialToken = testMember.getSocialToken();

        given(memberToAuthAdapter.getSocialToken(MEMBER_ID)).willReturn(Optional.of(socialToken));
        given(kakaoFriendClient.getFriendList(socialToken.getAccessToken(), (int) DEFAULT_OFFSET, DEFAULT_LIMIT_VALUE)).willReturn(response);

        KakaoFriendsDTO result = memberFriendService.getKakaoFriendList(MEMBER_ID, SocialProvider.KAKAO, DEFAULT_OFFSET, null);

        assertThat(result).isSameAs(response);
        verify(kakaoFriendClient).getFriendList(socialToken.getAccessToken(), (int) DEFAULT_OFFSET, DEFAULT_LIMIT_VALUE);
    }

    @Test
    @DisplayName("카카오 외 제공자는 지원하지 않는다")
    void shouldThrowWhenProviderIsNotKakao() {
        assertThatThrownBy(() -> memberFriendService.getKakaoFriendList(MEMBER_ID, SocialProvider.GOOGLE, DEFAULT_OFFSET, DEFAULT_LIMIT))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_UNSUPPORTED_SOCIAL_FRIEND.getMessage());
    }

    @Test
    @DisplayName("소셜 토큰이 없으면 에러를 반환한다")
    void shouldFailWhenSocialTokenMissing() {
        given(memberToAuthAdapter.getSocialToken(MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberFriendService.getKakaoFriendList(MEMBER_ID, SocialProvider.KAKAO, DEFAULT_OFFSET, DEFAULT_LIMIT))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SOCIAL_TOKEN_NOT_FOUNT.getMessage());
    }

    @Test
    @DisplayName("친구 조회 시 동의 오류가 발생하면 전용 에러로 변환한다")
    void shouldConvertConsentError() {
        SocialToken socialToken = testMember.getSocialToken();

        given(memberToAuthAdapter.getSocialToken(MEMBER_ID)).willReturn(Optional.of(socialToken));
        given(kakaoFriendClient.getFriendList(socialToken.getAccessToken(), (int) DEFAULT_OFFSET, DEFAULT_LIMIT_VALUE))
                .willThrow(new CustomException(ErrorCode.MEMBER_KAKAO_FRIEND_API_ERROR));

        assertThatThrownBy(() -> memberFriendService.getKakaoFriendList(MEMBER_ID, SocialProvider.KAKAO, DEFAULT_OFFSET, DEFAULT_LIMIT))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_KAKAO_FRIEND_CONSENT_FAIL.getMessage());
    }
}
