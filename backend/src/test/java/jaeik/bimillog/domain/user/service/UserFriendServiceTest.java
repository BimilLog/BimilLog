package jaeik.bimillog.domain.user.service;

import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.application.port.out.KakaoFriendPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.application.service.UserFriendService;
import jaeik.bimillog.domain.user.entity.*;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.global.application.port.out.GlobalTokenQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

/**
 * <h2>UserFriendService 테스트</h2>
 * <p>사용자 통합 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>카카오 친구 목록 조회 및 비밀로그 가입 여부 확인 기능을 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserFriendService 테스트")
class UserFriendServiceTest {

    @Mock
    private KakaoFriendPort kakaoFriendPort;
    
    @Mock
    private UserQueryPort userQueryPort;
    
    @Mock
    private GlobalTokenQueryPort globalTokenQueryPort;

    @InjectMocks
    private UserFriendService userFriendService;

    @Test
    @DisplayName("카카오 친구 목록 조회 - 정상 케이스 (실제 API 응답 구조)")
    void shouldGetKakaoFriendList_WhenValidRequest() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        Integer offset = 0;
        Integer limit = 3;
        
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .provider(SocialProvider.KAKAO)
                .socialId("123456")
                .role(UserRole.USER)
                .build();
        
        Token token = Token.createTemporaryToken("access-token", "refresh-token");
                
        
        // 실제 카카오 API 응답과 동일한 구조의 테스트 데이터
        List<KakaoFriendsResponseVO.Friend> friends = Arrays.asList(
                KakaoFriendsResponseVO.Friend.of(1L, "abcdefg0001", "이수민", "https://xxx.kakao.co.kr/.../aaa.jpg", true, null),
                KakaoFriendsResponseVO.Friend.of(2L, "abcdefg0002", "홍길동", "https://xxx.kakao.co.kr/.../bbb.jpg", false, null),
                KakaoFriendsResponseVO.Friend.of(3L, "abcdefg0003", "김철수", "https://xxx.kakao.co.kr/.../ccc.jpg", false, null)
        );
        
        KakaoFriendsResponseVO kakaoResponseVO = KakaoFriendsResponseVO.of(
                friends, 
                11, 
                null, 
                "https://kapi.kakao.com/v1/api/talk/friends?offset=3&limit=3&order=asc", 
                1
        );
        
        // 비밀로그 사용자 이름 조회 결과 (친구 중 홍길동만 가입)
        List<String> userNames = Arrays.asList("", "bimillogUser", "");

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-token", offset, limit)).willReturn(kakaoResponseVO);
        given(userQueryPort.findUserNamesInOrder(Arrays.asList("1", "2", "3"))).willReturn(userNames);

        // When
        KakaoFriendsResponseVO result = userFriendService.getKakaoFriendList(userId, tokenId, offset, limit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalCount()).isEqualTo(11);
        assertThat(result.favoriteCount()).isEqualTo(1);
        assertThat(result.elements()).hasSize(3);
        assertThat(result.afterUrl()).isEqualTo("https://kapi.kakao.com/v1/api/talk/friends?offset=3&limit=3&order=asc");
        
        // 비밀로그 사용자 이름 매핑 확인
        assertThat(result.elements().get(0).userName()).isNull(); // 이수민 - 비가입
        assertThat(result.elements().get(1).userName()).isEqualTo("bimillogUser"); // 홍길동 - 가입
        assertThat(result.elements().get(2).userName()).isNull(); // 김철수 - 비가입
        
        verify(userQueryPort).findById(userId);
        verify(globalTokenQueryPort).findById(tokenId);
        verify(kakaoFriendPort).getFriendList("access-token", offset, limit);
        verify(userQueryPort).findUserNamesInOrder(Arrays.asList("1", "2", "3"));
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 사용자가 존재하지 않는 경우")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        Long nonexistentUserId = 999L;

        given(userQueryPort.findById(nonexistentUserId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userFriendService.getKakaoFriendList(nonexistentUserId, 1L, 0, 10))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
        
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
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userFriendService.getKakaoFriendList(userId, tokenId, 0, 10))
                .isInstanceOf(AuthCustomException.class)
                .hasMessage(AuthErrorCode.NOT_FIND_TOKEN.getMessage());
        
        verify(userQueryPort).findById(userId);
        verify(globalTokenQueryPort).findById(tokenId);
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
        
        Token token = Token.createTemporaryToken(null, "refresh-token");
                

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(token));

        // When & Then
        assertThatThrownBy(() -> userFriendService.getKakaoFriendList(userId, tokenId, 0, 10))
                .isInstanceOf(AuthCustomException.class)
                .hasMessage(AuthErrorCode.NOT_FIND_TOKEN.getMessage());
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
        
        Token token = Token.createTemporaryToken("", "refresh-token");
                

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(token));

        // When & Then
        assertThatThrownBy(() -> userFriendService.getKakaoFriendList(userId, 1L, 0, 10))
                .isInstanceOf(AuthCustomException.class)
                .hasMessage(AuthErrorCode.NOT_FIND_TOKEN.getMessage());
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
        
        Token token = Token.createTemporaryToken("access-token", "refresh-token");
                
        
        KakaoFriendsResponseVO kakaoResponseVO = KakaoFriendsResponseVO.of(
                Collections.emptyList(), 0, null, null, 0
        );

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-token", 0, 10)).willReturn(kakaoResponseVO);

        // When
        KakaoFriendsResponseVO result = userFriendService.getKakaoFriendList(userId, tokenId, null, null);

        // Then
        assertThat(result).isEqualTo(kakaoResponseVO);

        verify(kakaoFriendPort).getFriendList("access-token", 0, 10); // 기본값 0, 10 사용
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
        
        Token token = Token.createTemporaryToken("access-token", "refresh-token");
                
        
        KakaoFriendsResponseVO kakaoResponseVO = KakaoFriendsResponseVO.of(
                Collections.emptyList(), 0, null, null, 0
        );

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-token", 0, 100)).willReturn(kakaoResponseVO);

        // When
        KakaoFriendsResponseVO result = userFriendService.getKakaoFriendList(userId, tokenId, 0, 200); // 200을 요청하지만 100으로 제한

        // Then
        assertThat(result).isEqualTo(kakaoResponseVO);

        verify(kakaoFriendPort).getFriendList("access-token", 0, 100); // 최대값 100으로 제한
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
        
        Token token = Token.createTemporaryToken("access-token", "refresh-token");
                

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-token", 0, 10))
                .willThrow(new UserCustomException(UserErrorCode.KAKAO_FRIEND_API_ERROR));

        // When & Then
        assertThatThrownBy(() -> userFriendService.getKakaoFriendList(userId, tokenId, 0, 10))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.KAKAO_FRIEND_CONSENT_FAIL.getMessage());
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
        
        Token token = Token.createTemporaryToken("access-token", "refresh-token");
                

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-token", 0, 10))
                .willThrow(new RuntimeException("일반적인 API 에러"));

        // When & Then
        assertThatThrownBy(() -> userFriendService.getKakaoFriendList(userId, tokenId, 0, 10))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.KAKAO_FRIEND_API_ERROR.getMessage());
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 친구 목록이 비어있는 경우")
    void shouldHandleEmptyFriendList_WhenNoFriends() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .build();
        
        Token token = Token.createTemporaryToken("access-token", "refresh-token");
                
        
        KakaoFriendsResponseVO emptyResponseVO = KakaoFriendsResponseVO.of(
                Collections.emptyList(), 0, null, null, 0
        );

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-token", 0, 10)).willReturn(emptyResponseVO);

        // When
        KakaoFriendsResponseVO result = userFriendService.getKakaoFriendList(userId, tokenId, 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.elements()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.favoriteCount()).isEqualTo(0);
        
        // 빈 친구 목록인 경우 findUserNamesInOrder가 호출되지 않음을 검증
        verify(userQueryPort, never()).findUserNamesInOrder(anyList());
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 모든 친구가 비밀로그에 가입한 경우")
    void shouldMapAllUserNames_WhenAllFriendsAreRegistered() {
        // Given
        Long userId = 1L;
        Long tokenId = 1L;
        
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .build();
        
        Token token = Token.createTemporaryToken("access-token", "refresh-token");
                
        
        List<KakaoFriendsResponseVO.Friend> friends = Arrays.asList(
                KakaoFriendsResponseVO.Friend.of(1L, "uuid001", "친구1", "https://img1.jpg", false, null),
                KakaoFriendsResponseVO.Friend.of(2L, "uuid002", "친구2", "https://img2.jpg", true, null)
        );
        
        KakaoFriendsResponseVO responseVO = KakaoFriendsResponseVO.of(
                friends, 2, null, null, 1
        );
        
        // 모든 친구가 비밀로그에 가입
        List<String> userNames = Arrays.asList("user1", "user2");

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(token));
        given(kakaoFriendPort.getFriendList("access-token", 0, 10)).willReturn(responseVO);
        given(userQueryPort.findUserNamesInOrder(Arrays.asList("1", "2"))).willReturn(userNames);

        // When
        KakaoFriendsResponseVO result = userFriendService.getKakaoFriendList(userId, tokenId, 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.elements()).hasSize(2);
        assertThat(result.elements().get(0).userName()).isEqualTo("user1");
        assertThat(result.elements().get(1).userName()).isEqualTo("user2");
        
        verify(userQueryPort).findUserNamesInOrder(Arrays.asList("1", "2"));
    }
}