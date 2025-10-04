package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalSocialStrategyPort;
import jaeik.bimillog.domain.global.application.strategy.SocialFriendStrategy;
import jaeik.bimillog.domain.global.application.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.application.port.out.MemberQueryPort;
import jaeik.bimillog.domain.member.application.service.MemberFriendService;
import jaeik.bimillog.domain.member.entity.KakaoFriendsResponseVO;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
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

    private static final Long MEMBER_ID = 1L;
    private static final Integer DEFAULT_OFFSET = 0;
    private static final Integer DEFAULT_LIMIT = 10;

    @Mock
    private MemberQueryPort memberQueryPort;

    @Mock
    private GlobalKakaoTokenQueryPort globalKakaoTokenQueryPort;

    @Mock
    private GlobalSocialStrategyPort globalSocialStrategyPort;

    @Mock
    private SocialPlatformStrategy socialPlatformStrategy;

    @Mock
    private SocialFriendStrategy socialFriendStrategy;

    @InjectMocks
    private MemberFriendService memberFriendService;

    private final Member testMember = TestMembers.copyWithId(TestMembers.MEMBER_1, MEMBER_ID);

    @Test
    @DisplayName("카카오 친구 목록 조회 시 가입자 이름을 매핑한다")
    void shouldMapMemberNamesFromStrategyResult() {
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");
        List<KakaoFriendsResponseVO.Friend> friends = Arrays.asList(
                KakaoFriendsResponseVO.Friend.of(1L, "uuid-1", "친구1", "img1", false, null),
                KakaoFriendsResponseVO.Friend.of(2L, "uuid-2", "친구2", "img2", true, null)
        );
        KakaoFriendsResponseVO response = KakaoFriendsResponseVO.of(friends, 2, null, null, 1);

        given(memberQueryPort.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
        given(globalKakaoTokenQueryPort.findByMemberId(MEMBER_ID)).willReturn(Optional.of(kakaoToken));
        given(globalSocialStrategyPort.getStrategy(SocialProvider.KAKAO)).willReturn(socialPlatformStrategy);
        given(socialPlatformStrategy.friend()).willReturn(Optional.of(socialFriendStrategy));
        given(socialFriendStrategy.getFriendList("access-token", DEFAULT_OFFSET, DEFAULT_LIMIT)).willReturn(response);
        given(memberQueryPort.findMemberNamesInOrder(Arrays.asList("1", "2"))).willReturn(Arrays.asList("user1", "user2"));

        KakaoFriendsResponseVO result = memberFriendService.getKakaoFriendList(MEMBER_ID, 99L, DEFAULT_OFFSET, DEFAULT_LIMIT);

        assertThat(result.elements()).extracting(KakaoFriendsResponseVO.Friend::memberName)
                .containsExactly("user1", "user2");
        verify(memberQueryPort).findMemberNamesInOrder(Arrays.asList("1", "2"));
    }

    @Test
    @DisplayName("친구 목록이 비어 있으면 이름 조회를 생략한다")
    void shouldSkipMemberLookupWhenNoFriends() {
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");
        KakaoFriendsResponseVO emptyResponse = KakaoFriendsResponseVO.of(Collections.emptyList(), 0, null, null, 0);

        given(memberQueryPort.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
        given(globalKakaoTokenQueryPort.findByMemberId(MEMBER_ID)).willReturn(Optional.of(kakaoToken));
        given(globalSocialStrategyPort.getStrategy(SocialProvider.KAKAO)).willReturn(socialPlatformStrategy);
        given(socialPlatformStrategy.friend()).willReturn(Optional.of(socialFriendStrategy));
        given(socialFriendStrategy.getFriendList("access-token", DEFAULT_OFFSET, DEFAULT_LIMIT)).willReturn(emptyResponse);

        KakaoFriendsResponseVO result = memberFriendService.getKakaoFriendList(MEMBER_ID, 1L, DEFAULT_OFFSET, DEFAULT_LIMIT);

        assertThat(result.elements()).isEmpty();
        verify(memberQueryPort, never()).findMemberNamesInOrder(anyList());
    }

    @Test
    @DisplayName("offset과 limit이 null이면 기본값을 사용한다")
    void shouldUseDefaultPaginationWhenNull() {
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");
        KakaoFriendsResponseVO response = KakaoFriendsResponseVO.of(Collections.emptyList(), 0, null, null, 0);

        given(memberQueryPort.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
        given(globalKakaoTokenQueryPort.findByMemberId(MEMBER_ID)).willReturn(Optional.of(kakaoToken));
        given(globalSocialStrategyPort.getStrategy(SocialProvider.KAKAO)).willReturn(socialPlatformStrategy);
        given(socialPlatformStrategy.friend()).willReturn(Optional.of(socialFriendStrategy));
        given(socialFriendStrategy.getFriendList("access-token", DEFAULT_OFFSET, DEFAULT_LIMIT)).willReturn(response);

        KakaoFriendsResponseVO result = memberFriendService.getKakaoFriendList(MEMBER_ID, 1L, null, null);

        assertThat(result).isSameAs(response);
        verify(socialFriendStrategy).getFriendList("access-token", DEFAULT_OFFSET, DEFAULT_LIMIT);
    }

    @Test
    @DisplayName("친구 전략이 존재하지 않으면 예외를 던진다")
    void shouldThrowWhenFriendStrategyMissing() {
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");

        given(memberQueryPort.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
        given(globalKakaoTokenQueryPort.findByMemberId(MEMBER_ID)).willReturn(Optional.of(kakaoToken));
        given(globalSocialStrategyPort.getStrategy(SocialProvider.KAKAO)).willReturn(socialPlatformStrategy);
        given(socialPlatformStrategy.friend()).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberFriendService.getKakaoFriendList(MEMBER_ID, 1L, DEFAULT_OFFSET, DEFAULT_LIMIT))
                .isInstanceOf(MemberCustomException.class)
                .hasMessage(MemberErrorCode.UNSUPPORTED_SOCIAL_FRIEND.getMessage());
    }

    @Test
    @DisplayName("카카오 토큰이 존재하지 않으면 공통 에러로 변환한다")
    void shouldWrapWhenKakaoTokenMissing() {
        given(memberQueryPort.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
        given(globalKakaoTokenQueryPort.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberFriendService.getKakaoFriendList(MEMBER_ID, 1L, DEFAULT_OFFSET, DEFAULT_LIMIT))
                .isInstanceOf(MemberCustomException.class)
                .hasMessage(MemberErrorCode.KAKAO_FRIEND_API_ERROR.getMessage())
                .hasCauseInstanceOf(AuthCustomException.class)
                .satisfies(ex -> assertThat(((MemberCustomException) ex).getCause())
                        .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.NOT_FIND_TOKEN));
    }

    @Test
    @DisplayName("친구 조회 시 동의 오류가 발생하면 전용 에러로 변환한다")
    void shouldConvertConsentError() {
        KakaoToken kakaoToken = KakaoToken.createKakaoToken("access-token", "refresh");

        given(memberQueryPort.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
        given(globalKakaoTokenQueryPort.findByMemberId(MEMBER_ID)).willReturn(Optional.of(kakaoToken));
        given(globalSocialStrategyPort.getStrategy(SocialProvider.KAKAO)).willReturn(socialPlatformStrategy);
        given(socialPlatformStrategy.friend()).willReturn(Optional.of(socialFriendStrategy));
        given(socialFriendStrategy.getFriendList("access-token", DEFAULT_OFFSET, DEFAULT_LIMIT))
                .willThrow(new MemberCustomException(MemberErrorCode.KAKAO_FRIEND_API_ERROR));

        assertThatThrownBy(() -> memberFriendService.getKakaoFriendList(MEMBER_ID, 1L, DEFAULT_OFFSET, DEFAULT_LIMIT))
                .isInstanceOf(MemberCustomException.class)
                .hasMessage(MemberErrorCode.KAKAO_FRIEND_CONSENT_FAIL.getMessage());
    }
}
