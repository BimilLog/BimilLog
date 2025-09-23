package jaeik.bimillog.testutil;

import jaeik.bimillog.infrastructure.adapter.out.api.dto.KakaoFriendsDTO;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>카카오 API 테스트 데이터 빌더</h2>
 * <p>카카오 API 관련 테스트에서 사용되는 DTO와 응답 객체 생성 유틸리티</p>
 * <p>반복적인 Mock 데이터 생성 코드를 중앙화하여 테스트 가독성 향상</p>
 * 
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>KakaoFriendsDTO 및 Friend 객체 생성</li>
 *   <li>카카오 토큰 응답 생성</li>
 *   <li>카카오 사용자 정보 응답 생성</li>
 * </ul>
 *
 * @author Jaeik
 * @version 1.0.0
 */
public class KakaoTestDataBuilder {

    /**
     * KakaoFriendsDTO.Friend 객체 생성
     * @param id 친구 ID
     * @param uuid 친구 UUID
     * @param nickname 친구 닉네임
     * @param image 프로필 이미지 URL
     * @param favorite 즐겨찾기 여부
     * @return KakaoFriendsDTO.Friend 객체
     */
    public static KakaoFriendsDTO.Friend createKakaoFriend(Long id, String uuid, 
                                                          String nickname, String image, 
                                                          Boolean favorite) {
        try {
            KakaoFriendsDTO.Friend friend = new KakaoFriendsDTO.Friend();
            setField(friend, "id", id);
            setField(friend, "uuid", uuid);
            setField(friend, "profileNickname", nickname);
            setField(friend, "profileThumbnailImage", image);
            setField(friend, "favorite", favorite);
            return friend;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create KakaoFriend", e);
        }
    }

    /**
     * 기본 KakaoFriendsDTO.Friend 객체 생성
     * @param id 친구 ID
     * @param nickname 친구 닉네임
     * @return KakaoFriendsDTO.Friend 객체
     */
    public static KakaoFriendsDTO.Friend createSimpleKakaoFriend(Long id, String nickname) {
        return createKakaoFriend(id, "uuid" + id, nickname, 
                               "http://example.com/image" + id + ".jpg", false);
    }

    /**
     * KakaoFriendsDTO 응답 객체 생성
     * @param elements 친구 목록
     * @param totalCount 전체 친구 수
     * @param beforeUrl 이전 페이지 URL
     * @param afterUrl 다음 페이지 URL
     * @param favoriteCount 즐겨찾기 친구 수
     * @return KakaoFriendsDTO 객체
     */
    public static KakaoFriendsDTO createKakaoFriendsResponse(List<KakaoFriendsDTO.Friend> elements,
                                                            Integer totalCount,
                                                            String beforeUrl, 
                                                            String afterUrl,
                                                            Integer favoriteCount) {
        try {
            KakaoFriendsDTO response = new KakaoFriendsDTO();
            setField(response, "elements", elements);
            setField(response, "totalCount", totalCount);
            setField(response, "beforeUrl", beforeUrl);
            setField(response, "afterUrl", afterUrl);
            setField(response, "favoriteCount", favoriteCount);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create KakaoFriendsResponse", e);
        }
    }

    /**
     * 빈 KakaoFriendsDTO 응답 생성
     * @return 빈 친구 목록을 가진 KakaoFriendsDTO
     */
    public static KakaoFriendsDTO createEmptyKakaoFriendsResponse() {
        return createKakaoFriendsResponse(List.of(), 0, null, null, 0);
    }

    /**
     * 카카오 토큰 응답 Map 생성
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @return 토큰 응답 Map
     */
    public static Map<String, Object> createTokenResponse(String accessToken, String refreshToken) {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", accessToken);
        tokenResponse.put("refresh_token", refreshToken);
        tokenResponse.put("token_type", "Bearer");
        tokenResponse.put("expires_in", 43199);
        tokenResponse.put("refresh_token_expires_in", 5183999);
        return tokenResponse;
    }

    /**
     * 카카오 사용자 정보 응답 Map 생성
     * @param socialId 소셜 ID
     * @param nickname 닉네임
     * @param email 이메일
     * @param profileImage 프로필 이미지 URL
     * @return 사용자 정보 응답 Map
     */
    public static Map<String, Object> createUserInfoResponse(String socialId, String nickname,
                                                            String email, String profileImage) {
        Map<String, Object> userInfoResponse = new HashMap<>();
        // socialId가 숫자로만 구성된 경우 Long으로 변환, 아니면 문자열 그대로 사용
        try {
            userInfoResponse.put("id", Long.parseLong(socialId));
        } catch (NumberFormatException e) {
            // 테스트에서는 일반적으로 숫자 ID를 사용하므로 기본값 설정
            userInfoResponse.put("id", 123456789L);
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", nickname);
        profile.put("thumbnail_image_url", profileImage);
        profile.put("profile_image_url", profileImage);
        profile.put("is_default_image", false);

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", email);
        kakaoAccount.put("profile", profile);
        kakaoAccount.put("has_email", email != null);
        kakaoAccount.put("email_needs_agreement", false);
        kakaoAccount.put("is_email_valid", true);
        kakaoAccount.put("is_email_verified", true);

        userInfoResponse.put("kakao_account", kakaoAccount);
        userInfoResponse.put("connected_at", "2024-01-01T00:00:00Z");

        return userInfoResponse;
    }

    /**
     * 간단한 카카오 사용자 정보 응답 생성 (이메일 없음)
     * @param socialId 소셜 ID
     * @param nickname 닉네임
     * @param profileImage 프로필 이미지 URL
     * @return 사용자 정보 응답 Map
     */
    public static Map<String, Object> createSimpleUserInfoResponse(String socialId, 
                                                                  String nickname,
                                                                  String profileImage) {
        return createUserInfoResponse(socialId, nickname, null, profileImage);
    }

    /**
     * 카카오 인증 코드 요청 파라미터 생성
     * @param clientId 클라이언트 ID
     * @param code 인증 코드
     * @param redirectUri 리다이렉트 URI
     * @param clientSecret 클라이언트 시크릿
     * @return 인증 파라미터 Map
     */
    public static Map<String, String> createAuthParams(String clientId, String code,
                                                      String redirectUri, String clientSecret) {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", clientId);
        params.put("code", code);
        params.put("redirect_uri", redirectUri);
        if (clientSecret != null) {
            params.put("client_secret", clientSecret);
        }
        return params;
    }

    /**
     * 카카오 언링크 파라미터 생성
     * @param targetId 대상 ID
     * @return 언링크 파라미터 Map
     */
    public static Map<String, String> createUnlinkParams(String targetId) {
        Map<String, String> params = new HashMap<>();
        params.put("target_id_type", "user_id");
        params.put("target_id", targetId);
        return params;
    }

    /**
     * Mock KakaoFriendsDTO 생성 (Mockito 사용)
     * @param friendCount 친구 수
     * @return Mock KakaoFriendsDTO
     */
    public static KakaoFriendsDTO createMockKakaoFriends(int friendCount) {
        List<KakaoFriendsDTO.Friend> friends = java.util.stream.IntStream
            .rangeClosed(1, friendCount)
            .mapToObj(i -> createSimpleKakaoFriend((long) i, "친구" + i))
            .collect(java.util.stream.Collectors.toList());

        return createKakaoFriendsResponse(friends, friendCount, null, null, 0);
    }

    /**
     * Reflection을 사용한 private 필드 값 설정
     * @param target 대상 객체
     * @param fieldName 필드명
     * @param value 설정할 값
     */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Private constructor to prevent instantiation
    private KakaoTestDataBuilder() {}
}