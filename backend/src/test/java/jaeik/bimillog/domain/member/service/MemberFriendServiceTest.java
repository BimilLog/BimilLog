package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalAuthTokenQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalSocialFriendStrategyPort;
import jaeik.bimillog.domain.member.application.port.out.MemberQueryPort;
import jaeik.bimillog.domain.member.application.service.MemberFriendService;
import jaeik.bimillog.domain.member.entity.KakaoFriendsResponseVO;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("unit")
class MemberFriendServiceTest extends BaseUnitTest {

    @Mock
    private GlobalSocialFriendStrategyPort globalSocialFriendStrategyPort;

    @Mock
    private MemberQueryPort memberQueryPort;

    @Mock
    private GlobalAuthTokenQueryPort globalAuthTokenQueryPort;

    @Mock
    private GlobalKakaoTokenQueryPort globalKakaoTokenQueryPort;

    @InjectMocks
    private MemberFriendService memberFriendService;

    @Test
    @DisplayName("카카오 친구 목록 조회 - 가입자 이름 매핑")
    void shouldMapMemberNamesFromStrategyResult() {
        // given
        AuthToken authToken = AuthToken.createToken("refresh-token", getTestMember());
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");
        List<KakaoFriendsResponseVO.Friend> friends = Arrays.asList(
                KakaoFriendsResponseVO.Friend.of(1L, "uuid-1", "친구1", "img1", false, null),
                KakaoFriendsResponseVO.Friend.of(2L, "uuid-2", "친구2", "img2", true, null)
        );
        KakaoFriendsResponseVO response = KakaoFriendsResponseVO.of(friends, 2, null, null, 1);

        given(globalAuthTokenQueryPort.findById(99L)).willReturn(Optional.of(authToken));
        given(globalKakaoTokenQueryPort.findByMemberId(getTestMember().getId())).willReturn(Optional.of(kakaoToken));
        given(globalSocialFriendStrategyPort.getFriendList(SocialProvider.KAKAO, "access-token", 0, 10)).willReturn(response);
        given(memberQueryPort.findMemberNamesInOrder(Arrays.asList("1", "2"))).willReturn(Arrays.asList("user1", "user2"));

        // when
        KakaoFriendsResponseVO result = memberFriendService.getKakaoFriendList(1L, 99L, 0, 10);

        // then
        assertThat(result.elements()).extracting(KakaoFriendsResponseVO.Friend::memberName)
                .containsExactly("user1", "user2");
        verify(globalSocialFriendStrategyPort).getFriendList(SocialProvider.KAKAO, "access-token", 0, 10);
        verify(memberQueryPort).findMemberNamesInOrder(Arrays.asList("1", "2"));
    }

    @Test
    @DisplayName("친구 목록이 비어 있으면 이름 조회를 생략한다")
    void shouldSkipMemberLookupWhenNoFriends() {
        // given
        AuthToken authToken = AuthToken.createToken("refresh-token", getTestMember());
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");
        KakaoFriendsResponseVO emptyResponse = KakaoFriendsResponseVO.of(Collections.emptyList(), 0, null, null, 0);

        given(globalAuthTokenQueryPort.findById(1L)).willReturn(Optional.of(authToken));
        given(globalKakaoTokenQueryPort.findByMemberId(getTestMember().getId())).willReturn(Optional.of(kakaoToken));
        given(globalSocialFriendStrategyPort.getFriendList(SocialProvider.KAKAO, "access-token", 0, 10)).willReturn(emptyResponse);

        // when
        KakaoFriendsResponseVO result = memberFriendService.getKakaoFriendList(1L, 1L, 0, 10);

        // then
        assertThat(result.elements()).isEmpty();
        verify(memberQueryPort, never()).findMemberNamesInOrder(anyList());
    }

    @Test
    @DisplayName("offset과 limit가 null이면 기본값을 적용한다")
    void shouldUseDefaultPaginationWhenNull() {
        // given
        AuthToken authToken = AuthToken.createToken("refresh-token", getTestMember());
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");
        KakaoFriendsResponseVO response = KakaoFriendsResponseVO.of(Collections.emptyList(), 0, null, null, 0);

        given(globalAuthTokenQueryPort.findById(1L)).willReturn(Optional.of(authToken));
        given(globalKakaoTokenQueryPort.findByMemberId(getTestMember().getId())).willReturn(Optional.of(kakaoToken));
        given(globalSocialFriendStrategyPort.getFriendList(SocialProvider.KAKAO, "access-token", 0, 10)).willReturn(response);

        // when
        KakaoFriendsResponseVO result = memberFriendService.getKakaoFriendList(1L, 1L, null, null);

        // then
        assertThat(result).isSameAs(response);
        verify(globalSocialFriendStrategyPort).getFriendList(SocialProvider.KAKAO, "access-token", 0, 10);
    }

    @Test
    @DisplayName("limit는 최대 100으로 제한한다")
    void shouldCapLimitAtHundred() {
        // given
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");
        KakaoFriendsResponseVO response = KakaoFriendsResponseVO.of(Collections.emptyList(), 0, null, null, 0);

        given(globalKakaoTokenQueryPort.findByMemberId(1L)).willReturn(Optional.of(kakaoToken));
        given(globalSocialFriendStrategyPort.getFriendList(SocialProvider.KAKAO, "access-token", 0, 100)).willReturn(response);

        // when
        KakaoFriendsResponseVO result = memberFriendService.getKakaoFriendList(1L, 1L, 0, 200);

        // then
        assertThat(result).isSameAs(response);
        verify(globalSocialFriendStrategyPort).getFriendList(SocialProvider.KAKAO, "access-token", 0, 100);
    }

    @Test
    @DisplayName("전략에서 동의 오류가 발생하면 맞춤 에러로 변환한다")
    void shouldConvertConsentError() {
        // given
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");

        given(globalKakaoTokenQueryPort.findByMemberId(1L)).willReturn(Optional.of(kakaoToken));
        given(globalSocialFriendStrategyPort.getFriendList(SocialProvider.KAKAO, "access-token", 0, 10))
                .willThrow(new MemberCustomException(MemberErrorCode.KAKAO_FRIEND_API_ERROR));

        // expect
        assertThatThrownBy(() -> memberFriendService.getKakaoFriendList(1L, 1L, 0, 10))
                .isInstanceOf(MemberCustomException.class)
                .hasMessage(MemberErrorCode.KAKAO_FRIEND_CONSENT_FAIL.getMessage());
    }

    @Test
    @DisplayName("전략에서 일반 예외가 발생하면 공통 에러로 감싼다")
    void shouldWrapGenericException() {
        // given
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");

        given(globalKakaoTokenQueryPort.findByMemberId(1L)).willReturn(Optional.of(kakaoToken));
        given(globalSocialFriendStrategyPort.getFriendList(SocialProvider.KAKAO, "access-token", 0, 10))
                .willThrow(new RuntimeException("kakao down"));

        // expect
        assertThatThrownBy(() -> memberFriendService.getKakaoFriendList(1L, 1L, 0, 10))
                .isInstanceOf(MemberCustomException.class)
                .hasMessage(MemberErrorCode.KAKAO_FRIEND_API_ERROR.getMessage());
    }

    @Test
    @DisplayName("카카오 토큰이 없으면 토큰 조회 예외를 변환한다")
    void shouldTransformWhenKakaoTokenMissing() {
        // given
        given(globalKakaoTokenQueryPort.findByMemberId(1L)).willReturn(Optional.empty());

        // expect
        assertThatThrownBy(() -> memberFriendService.getKakaoFriendList(1L, 1L, 0, 10))
                .isInstanceOf(MemberCustomException.class)
                .hasMessage(MemberErrorCode.KAKAO_FRIEND_API_ERROR.getMessage())
                .hasCauseInstanceOf(AuthCustomException.class);

        verify(globalKakaoTokenQueryPort).findByMemberId(1L);
    }
}
