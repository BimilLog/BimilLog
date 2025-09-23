package jaeik.bimillog.infrastructure.adapter.out.api.social.kakao;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.adapter.out.api.dto.KakaoFriendsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * <h2>KakaoSocialAdapter 테스트</h2>
 * <p>카카오 소셜 어댑터의 기본 동작 테스트</p>
 * <p>Feign Client를 사용한 기본 동작 검증에 집중</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoSocialAdapter 단위 테스트")
class KakaoAuthToUserAdapterTest {

    private static final String TEST_ACCESS_TOKEN = "test-access-token";
    private static final Integer DEFAULT_OFFSET = 0;
    private static final Integer DEFAULT_LIMIT = 10;

    @Mock
    private KakaoApiClient kakaoApiClient;

    @InjectMocks
    private KakaoSocialAdapter kakaoSocialAdapter;

    @BeforeEach
    void setUp() {
        // 필요한 경우 초기화 작업
    }

    @Test
    @DisplayName("getProvider - 카카오 제공자 반환")
    void shouldReturnKakaoProvider_WhenGetProviderCalled() {
        // When
        SocialProvider result = kakaoSocialAdapter.getProvider();

        // Then
        assertThat(result).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("getFriendList 성공 - 친구 목록 조회")
    void shouldGetFriendListSuccessfully() {
        // Given - Mock으로부터 반환될 KakaoFriendsDTO 생성
        KakaoFriendsDTO mockFriends = mock(KakaoFriendsDTO.class);
        KakaoFriendsDTO.Friend mockFriend1 = mock(KakaoFriendsDTO.Friend.class);
        KakaoFriendsDTO.Friend mockFriend2 = mock(KakaoFriendsDTO.Friend.class);

        given(mockFriend1.getProfileNickname()).willReturn("친구1");
        given(mockFriend2.getProfileNickname()).willReturn("친구2");

        List<KakaoFriendsDTO.Friend> friendList = List.of(mockFriend1, mockFriend2);
        given(mockFriends.getElements()).willReturn(friendList);
        given(mockFriends.getTotalCount()).willReturn(2);

        given(kakaoApiClient.getFriends(anyString(), anyInt(), anyInt()))
            .willReturn(mockFriends);

        // When
        KakaoFriendsDTO result = kakaoSocialAdapter.getFriendList(
            TEST_ACCESS_TOKEN, DEFAULT_OFFSET, DEFAULT_LIMIT);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getElements()).hasSize(2);
        assertThat(result.getElements().get(0).getProfileNickname()).isEqualTo("친구1");
        assertThat(result.getElements().get(1).getProfileNickname()).isEqualTo("친구2");

        verify(kakaoApiClient).getFriends(
            "Bearer " + TEST_ACCESS_TOKEN, DEFAULT_OFFSET, DEFAULT_LIMIT);
    }

    @Test
    @DisplayName("getFriendList 실패 - API 호출 실패 시 UserCustomException 발생")
    void shouldThrowUserCustomException_WhenApiCallFails() {
        // Given
        given(kakaoApiClient.getFriends(anyString(), anyInt(), anyInt()))
            .willThrow(new RuntimeException("Kakao API error"));

        // When & Then
        assertThatThrownBy(() ->
            kakaoSocialAdapter.getFriendList(TEST_ACCESS_TOKEN, DEFAULT_OFFSET, DEFAULT_LIMIT))
            .isInstanceOf(UserCustomException.class)
            .satisfies(exception -> {
                UserCustomException userException = (UserCustomException) exception;
                assertThat(userException.getUserErrorCode()).isEqualTo(UserErrorCode.KAKAO_FRIEND_API_ERROR);
            });

        verify(kakaoApiClient).getFriends(
            "Bearer " + TEST_ACCESS_TOKEN, DEFAULT_OFFSET, DEFAULT_LIMIT);
    }

    @Test
    @DisplayName("getFriendList - null 파라미터 처리")
    void shouldHandleNullParameters() {
        // Given
        KakaoFriendsDTO mockFriends = mock(KakaoFriendsDTO.class);
        lenient().when(mockFriends.getElements()).thenReturn(new ArrayList<>());
        lenient().when(mockFriends.getTotalCount()).thenReturn(0);

        given(kakaoApiClient.getFriends(anyString(), any(), any()))
            .willReturn(mockFriends);

        // When
        KakaoFriendsDTO result = kakaoSocialAdapter.getFriendList(
            TEST_ACCESS_TOKEN, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(0);

        // null 값이 전달되어도 API 호출은 정상적으로 수행
        verify(kakaoApiClient).getFriends(
            "Bearer " + TEST_ACCESS_TOKEN, null, null);
    }

    @Test
    @DisplayName("getFriendList - 빈 친구 목록 처리")
    void shouldHandleEmptyFriendList() {
        // Given
        KakaoFriendsDTO mockFriends = mock(KakaoFriendsDTO.class);
        lenient().when(mockFriends.getElements()).thenReturn(new ArrayList<>());
        lenient().when(mockFriends.getTotalCount()).thenReturn(0);

        given(kakaoApiClient.getFriends(anyString(), anyInt(), anyInt()))
            .willReturn(mockFriends);

        // When
        KakaoFriendsDTO result = kakaoSocialAdapter.getFriendList(
            TEST_ACCESS_TOKEN, DEFAULT_OFFSET, DEFAULT_LIMIT);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(0);
        assertThat(result.getElements()).isEmpty();
    }
}