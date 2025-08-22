package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.out.KakaoFriendPort;
import jaeik.growfarm.domain.user.application.port.out.TokenPort;
import jaeik.growfarm.domain.user.application.port.out.UserQueryPort;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.KakaoFriendsResponse;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>UserIntegrationService 테스트</h2>
 * <p>사용자 통합 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>카카오 친구 목록 조회 및 비밀로그 가입 여부 확인 기능을 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserIntegrationService 테스트")
class UserIntegrationServiceTest {

    @Mock
    private KakaoFriendPort kakaoFriendPort;
    
    @Mock
    private UserQueryPort userQueryPort;
    
    @Mock
    private TokenPort tokenPort;

    @InjectMocks
    private UserIntegrationService userIntegrationService;

    @Test
    @DisplayName("카카오 친구 목록 조회 - 정상 케이스")
    void shouldGetKakaoFriendList_WhenValidRequest() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        Integer offset = 0;
        Integer limit = 10;
        
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .provider(SocialProvider.KAKAO)
                .socialId("123456")
                .role(UserRole.USER)
                .build();
        
        Token token = Token.builder()
                .id(tokenId)
                .accessToken("valid_access_token")
                .refreshToken("refresh_token")
                .build();
        
        KakaoFriendsResponse kakaoResponse = new KakaoFriendsResponse();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(tokenPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("valid_access_token", offset, limit)).willReturn(kakaoResponse);

        // When
        KakaoFriendsResponse result = userIntegrationService.getKakaoFriendList(userId, tokenId, offset, limit);

        // Then
        verify(userQueryPort).findById(userId);
        verify(tokenPort).findById(tokenId);
        verify(kakaoFriendPort).getFriendList("valid_access_token", offset, limit);
        
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(kakaoResponse);
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 사용자가 존재하지 않는 경우")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        Long nonexistentUserId = 999L;

        given(userQueryPort.findById(nonexistentUserId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userIntegrationService.getKakaoFriendList(nonexistentUserId, 1L, 0, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        
        verify(userQueryPort).findById(nonexistentUserId);
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 토큰이 존재하지 않는 경우")
    void shouldThrowException_WhenTokenNotFound() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .build();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(tokenPort.findById(tokenId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userIntegrationService.getKakaoFriendList(userId, tokenId, 0, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_FIND_TOKEN.getMessage());
        
        verify(userQueryPort).findById(userId);
        verify(tokenPort).findById(tokenId);
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 액세스 토큰이 null인 경우")
    void shouldThrowException_WhenAccessTokenIsNull() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .build();
        
        Token token = Token.builder()
                .id(tokenId)
                .accessToken(null) // null 토큰
                .refreshToken("refresh_token")
                .build();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(tokenPort.findById(tokenId)).willReturn(Optional.of(token));

        // When & Then
        assertThatThrownBy(() -> userIntegrationService.getKakaoFriendList(userId, tokenId, 0, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_FIND_TOKEN.getMessage());
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 액세스 토큰이 빈 문자열인 경우")
    void shouldThrowException_WhenAccessTokenIsEmpty() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .build();
        
        Token token = Token.builder()
                .id(tokenId)
                .accessToken("") // 빈 문자열 토큰
                .refreshToken("refresh_token")
                .build();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(tokenPort.findById(tokenId)).willReturn(Optional.of(token));

        // When & Then
        assertThatThrownBy(() -> userIntegrationService.getKakaoFriendList(userId, 1L, 0, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_FIND_TOKEN.getMessage());
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 기본값 설정 (null offset, limit)")
    void shouldUseDefaultValues_WhenOffsetAndLimitAreNull() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .build();
        
        Token token = Token.builder()
                .id(tokenId)
                .accessToken("valid_token")
                .build();
        
        KakaoFriendsResponse kakaoResponse = new KakaoFriendsResponse();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(tokenPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("valid_token", 0, 10)).willReturn(kakaoResponse);

        // When
        userIntegrationService.getKakaoFriendList(userId, 1L, null, null);

        // Then
        verify(kakaoFriendPort).getFriendList("valid_token", 0, 10); // 기본값 0, 10 사용
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - limit 최대값 제한")
    void shouldLimitMaximumLimit_WhenLimitExceedsMaximum() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .build();
        
        Token token = Token.builder()
                .id(tokenId)
                .accessToken("valid_token")
                .build();
        
        KakaoFriendsResponse kakaoResponse = new KakaoFriendsResponse();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(tokenPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("valid_token", 0, 100)).willReturn(kakaoResponse);

        // When
        userIntegrationService.getKakaoFriendList(userId, 1L, 0, 200); // 200을 요청하지만 100으로 제한

        // Then
        verify(kakaoFriendPort).getFriendList("valid_token", 0, 100); // 최대값 100으로 제한
    }

    @Test
    @DisplayName("카카오 친구 동의 필요 에러 처리")
    void shouldThrowKakaoFriendConsentError_WhenConsentRequired() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .build();
        
        Token token = Token.builder()
                .id(tokenId)
                .accessToken("valid_token")
                .build();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(tokenPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("valid_token", 0, 10))
                .willThrow(new CustomException(ErrorCode.KAKAO_API_ERROR));

        // When & Then
        assertThatThrownBy(() -> userIntegrationService.getKakaoFriendList(userId, 1L, 0, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.KAKAO_FRIEND_CONSENT_FAIL.getMessage());
    }

    @Test
    @DisplayName("카카오 API 일반 에러 처리")
    void shouldThrowKakaoApiError_WhenGeneralApiError() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .build();
        
        Token token = Token.builder()
                .id(tokenId)
                .accessToken("valid_token")
                .build();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(tokenPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("valid_token", 0, 10))
                .willThrow(new RuntimeException("일반적인 API 에러"));

        // When & Then
        assertThatThrownBy(() -> userIntegrationService.getKakaoFriendList(userId, 1L, 0, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.KAKAO_API_ERROR.getMessage());
    }
}