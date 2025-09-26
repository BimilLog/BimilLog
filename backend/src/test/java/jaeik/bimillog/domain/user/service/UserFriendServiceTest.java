package jaeik.bimillog.domain.user.service;

import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.user.application.port.out.KakaoFriendPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.application.service.UserFriendService;
import jaeik.bimillog.domain.user.entity.KakaoFriendsResponseVO;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>UserFriendService 테스트</h2>
 * <p>카카오 친구 조회 흐름과 예외 매핑을 검증합니다.</p>
 */
@DisplayName("UserFriendService 테스트")
class UserFriendServiceTest extends BaseUnitTest {

    @Mock
    private KakaoFriendPort kakaoFriendPort;

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private GlobalTokenQueryPort globalTokenQueryPort;

    @InjectMocks
    private UserFriendService userFriendService;

    @Test
    @DisplayName("카카오 친구 목록 조회 - 정상 플로우")
    void shouldGetKakaoFriendList_WhenValidRequest() {
        // Given
        Long tokenId = 1L;
        Token token = Token.createTemporaryToken("access-TemporaryToken", "refresh-TemporaryToken");
        List<KakaoFriendsResponseVO.Friend> friends = Arrays.asList(
                KakaoFriendsResponseVO.Friend.of(1L, "uuid-1", "이수민", "https://a.jpg", true, null),
                KakaoFriendsResponseVO.Friend.of(2L, "uuid-2", "홍길동", "https://b.jpg", false, null),
                KakaoFriendsResponseVO.Friend.of(3L, "uuid-3", "김철수", "https://c.jpg", false, null)
        );
        KakaoFriendsResponseVO kakaoResponse = KakaoFriendsResponseVO.of(
                friends,
                11,
                null,
                "https://kapi.kakao.com/v1/api/talk/friends?offset=3&limit=3&order=asc",
                1
        );
        List<String> userNames = Arrays.asList("", "bimillogUser", "");

        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-TemporaryToken", 0, 3)).willReturn(kakaoResponse);
        given(userQueryPort.findUserNamesInOrder(Arrays.asList("1", "2", "3"))).willReturn(userNames);

        // When
        KakaoFriendsResponseVO result = userFriendService.getKakaoFriendList(1L, tokenId, 0, 3);

        // Then
        assertThat(result.totalCount()).isEqualTo(11);
        assertThat(result.favoriteCount()).isEqualTo(1);
        assertThat(result.afterUrl()).isEqualTo("https://kapi.kakao.com/v1/api/talk/friends?offset=3&limit=3&order=asc");
        assertThat(result.elements()).extracting(KakaoFriendsResponseVO.Friend::userName)
                .containsExactly(null, "bimillogUser", null);

        verify(userQueryPort).findUserNamesInOrder(Arrays.asList("1", "2", "3"));
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 토큰이 존재하지 않으면 예외")
    void shouldThrowException_WhenTokenNotFound() {
        // Given
        given(globalTokenQueryPort.findById(1L)).willReturn(Optional.empty());

        // When
        Throwable thrown = catchThrowable(() -> userFriendService.getKakaoFriendList(1L, 1L, 0, 10));

        // Then
        assertThat(thrown)
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.KAKAO_FRIEND_API_ERROR.getMessage())
                .hasCauseInstanceOf(AuthCustomException.class);
        verify(globalTokenQueryPort).findById(1L);
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 액세스 토큰이 null이면 예외")
    void shouldThrowException_WhenAccessTokenIsNull() {
        // Given
        Token token = Token.createTemporaryToken(null, "refresh-TemporaryToken");
        given(globalTokenQueryPort.findById(1L)).willReturn(Optional.of(token));

        // When
        Throwable thrown = catchThrowable(() -> userFriendService.getKakaoFriendList(1L, 1L, 0, 10));

        // Then
        assertThat(thrown)
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.KAKAO_FRIEND_API_ERROR.getMessage())
                .hasCauseInstanceOf(AuthCustomException.class);
        verify(globalTokenQueryPort).findById(1L);
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 액세스 토큰이 빈 문자열이면 예외")
    void shouldThrowException_WhenAccessTokenIsEmpty() {
        // Given
        Token token = Token.createTemporaryToken("", "refresh-TemporaryToken");
        given(globalTokenQueryPort.findById(1L)).willReturn(Optional.of(token));

        // When
        Throwable thrown = catchThrowable(() -> userFriendService.getKakaoFriendList(1L, 1L, 0, 10));

        // Then
        assertThat(thrown)
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.KAKAO_FRIEND_API_ERROR.getMessage())
                .hasCauseInstanceOf(AuthCustomException.class);
        verify(globalTokenQueryPort).findById(1L);
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - null offset과 limit는 기본값으로 처리")
    void shouldUseDefaultValues_WhenOffsetAndLimitAreNull() {
        // Given
        Token token = Token.createTemporaryToken("access-TemporaryToken", "refresh-TemporaryToken");
        KakaoFriendsResponseVO kakaoResponse = KakaoFriendsResponseVO.of(
                Collections.emptyList(), 0, null, null, 0
        );

        given(globalTokenQueryPort.findById(1L)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-TemporaryToken", 0, 10)).willReturn(kakaoResponse);

        // When
        KakaoFriendsResponseVO result = userFriendService.getKakaoFriendList(1L, 1L, null, null);

        // Then
        assertThat(result).isEqualTo(kakaoResponse);
        verify(kakaoFriendPort).getFriendList("access-TemporaryToken", 0, 10);
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - limit는 최대 100으로 제한")
    void shouldLimitMaximumLimit_WhenLimitExceedsMaximum() {
        // Given
        Token token = Token.createTemporaryToken("access-TemporaryToken", "refresh-TemporaryToken");
        KakaoFriendsResponseVO kakaoResponse = KakaoFriendsResponseVO.of(
                Collections.emptyList(), 0, null, null, 0
        );

        given(globalTokenQueryPort.findById(1L)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-TemporaryToken", 0, 100)).willReturn(kakaoResponse);

        // When
        KakaoFriendsResponseVO result = userFriendService.getKakaoFriendList(1L, 1L, 0, 200);

        // Then
        assertThat(result).isEqualTo(kakaoResponse);
        verify(kakaoFriendPort).getFriendList("access-TemporaryToken", 0, 100);
    }

    @Test
    @DisplayName("카카오 친구 동의 필요 예외는 커스텀 메시지로 변환")
    void shouldThrowKakaoFriendConsentError_WhenConsentRequired() {
        // Given
        Token token = Token.createTemporaryToken("access-TemporaryToken", "refresh-TemporaryToken");
        given(globalTokenQueryPort.findById(1L)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-TemporaryToken", 0, 10))
                .willThrow(new UserCustomException(UserErrorCode.KAKAO_FRIEND_API_ERROR));

        // When & Then
        assertThatThrownBy(() -> userFriendService.getKakaoFriendList(1L, 1L, 0, 10))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.KAKAO_FRIEND_CONSENT_FAIL.getMessage());
    }

    @Test
    @DisplayName("카카오 API 일반 에러는 그대로 래핑")
    void shouldThrowKakaoApiError_WhenGeneralApiError() {
        // Given
        Token token = Token.createTemporaryToken("access-TemporaryToken", "refresh-TemporaryToken");
        given(globalTokenQueryPort.findById(1L)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-TemporaryToken", 0, 10))
                .willThrow(new RuntimeException("일반적인 API 에러"));

        // When & Then
        assertThatThrownBy(() -> userFriendService.getKakaoFriendList(1L, 1L, 0, 10))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.KAKAO_FRIEND_API_ERROR.getMessage());
    }

    @Test
    @DisplayName("친구 목록이 비어 있으면 추가 조회를 수행하지 않는다")
    void shouldHandleEmptyFriendList_WhenNoFriends() {
        // Given
        Token token = Token.createTemporaryToken("access-TemporaryToken", "refresh-TemporaryToken");
        KakaoFriendsResponseVO kakaoResponse = KakaoFriendsResponseVO.of(
                Collections.emptyList(), 0, null, null, 0
        );

        given(globalTokenQueryPort.findById(1L)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-TemporaryToken", 0, 10)).willReturn(kakaoResponse);

        // When
        KakaoFriendsResponseVO result = userFriendService.getKakaoFriendList(1L, 1L, 0, 10);

        // Then
        assertThat(result.elements()).isEmpty();
        verify(userQueryPort, never()).findUserNamesInOrder(anyList());
    }

    @Test
    @DisplayName("모든 친구가 가입한 경우 사용자 이름을 매핑한다")
    void shouldMapAllUserNames_WhenAllFriendsAreRegistered() {
        // Given
        Token token = Token.createTemporaryToken("access-TemporaryToken", "refresh-TemporaryToken");
        List<KakaoFriendsResponseVO.Friend> friends = Arrays.asList(
                KakaoFriendsResponseVO.Friend.of(1L, "uuid-1", "친구1", "https://img1.jpg", false, null),
                KakaoFriendsResponseVO.Friend.of(2L, "uuid-2", "친구2", "https://img2.jpg", true, null)
        );
        KakaoFriendsResponseVO kakaoResponse = KakaoFriendsResponseVO.of(
                friends, 2, null, null, 1
        );
        List<String> userNames = Arrays.asList("user1", "user2");

        given(globalTokenQueryPort.findById(1L)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-TemporaryToken", 0, 10)).willReturn(kakaoResponse);
        given(userQueryPort.findUserNamesInOrder(Arrays.asList("1", "2"))).willReturn(userNames);

        // When
        KakaoFriendsResponseVO result = userFriendService.getKakaoFriendList(1L, 1L, 0, 10);

        // Then
        assertThat(result.elements()).extracting(KakaoFriendsResponseVO.Friend::userName)
                .containsExactly("user1", "user2");
        verify(userQueryPort).findUserNamesInOrder(Arrays.asList("1", "2"));
    }
}
