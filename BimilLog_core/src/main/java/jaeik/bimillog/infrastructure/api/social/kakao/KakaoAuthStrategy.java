package jaeik.bimillog.infrastructure.api.social.kakao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.global.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * <h2>카카오 인증 전략</h2>
 * <p>카카오 OAuth 플로우를 처리하는 인증 전략 구현체입니다.</p>
 */
@Component
@RequiredArgsConstructor
public class KakaoAuthStrategy implements SocialAuthStrategy {

    private final KakaoKeyVO kakaoKeyVO;
    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoApiClient kakaoApiClient;
    private final ObjectMapper objectMapper;

    /**
     * <h3>소셜 제공자 반환</h3>
     * <p>이 전략이 지원하는 소셜 제공자를 반환합니다.</p>
     *
     * @return KAKAO 제공자
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    /**
     * <h3>카카오 토큰 및 사용자 프로필 조회</h3>
     * <p>OAuth 인증 코드를 사용하여 카카오 액세스/리프레시 토큰을 발급받고,</p>
     * <p>ID 토큰을 파싱하여 사용자 프로필 정보를 추출합니다.</p>
     *
     * @param code OAuth 2.0 인증 코드
     * @return 소셜 토큰 및 사용자 프로필 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialMemberProfile getSocialToken(String code) {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", kakaoKeyVO.getCLIENT_ID());
        params.put("client_secret", kakaoKeyVO.getCLIENT_SECRET());
        params.put("redirect_uri", kakaoKeyVO.getREDIRECT_URI());
        params.put("code", code);

        try {
            Map<String, Object> responseBody = kakaoAuthClient.getToken(
                    "application/x-www-form-urlencoded;charset=utf-8",
                    params
            );

            String accessToken = (String) responseBody.get("access_token");
            String refreshToken = (String) responseBody.get("refresh_token");
            String idToken = (String) responseBody.get("id_token");

            Map<String, Object> payload = parseIdTokenPayload(idToken);

            String socialId = String.valueOf(payload.get("sub"));
            String nickname = (String) payload.get("nickname");
            String profileImageUrl = (String) payload.get("picture");

            return SocialMemberProfile.of(
                    socialId,
                    null,
                    SocialProvider.KAKAO,
                    nickname,
                    profileImageUrl,
                    accessToken,
                    refreshToken
            );
        } catch (Exception e) {
            throw new RuntimeException("Kakao token request failed: " + e.getMessage(), e);
        }
    }

    /**
     * <h3>카카오 사용자 정보 조회</h3>
     * <p>액세스 토큰을 사용하여 카카오 사용자 정보를 조회합니다.</p>
     * <p>현재는 정보 조회만 수행하며, 반환값은 사용되지 않습니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void getUserInfo(String accessToken) {
        try {
            Map<String, Object> responseBody = kakaoApiClient.getUserInfo("Bearer " + accessToken);

            Map<String, Object> kakaoAccount = (Map<String, Object>) responseBody.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String socialId = String.valueOf(responseBody.get("id"));
            String nickname = (String) profile.get("nickname");
            String thumbnailImage = (String) profile.get("thumbnail_image_url");

//            KakaoMemberInfo.of(
//                    socialId,
//                    null, // 이메일이 필요없어서 받지 않고 있음
//                    SocialProvider.KAKAO,
//                    nickname,
//                    thumbnailImage
//            );
        } catch (Exception e) {
            throw new RuntimeException("Kakao member info request failed: " + e.getMessage(), e);
        }
    }

    /**
     * <h3>카카오 계정 연결 해제</h3>
     * <p>관리자 키를 사용하여 사용자의 카카오 계정 연결을 해제합니다.</p>
     * <p>회원 탈퇴 또는 사용자 차단 시 호출됩니다.</p>
     *
     * @param socialId 카카오 사용자 고유 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void unlink(String socialId) {
        Map<String, String> params = new HashMap<>();
        params.put("target_id_type", "user_id");
        params.put("target_id", socialId);

        try {
            kakaoApiClient.unlink("KakaoAK " + kakaoKeyVO.getADMIN_KEY(), params);
        } catch (Exception e) {
            throw new RuntimeException("Kakao unlink failed: " + e.getMessage(), e);
        }
    }

    /**
     * <h3>카카오 로그아웃</h3>
     * <p>액세스 토큰을 사용하여 카카오 세션을 로그아웃 처리합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @throws Exception 로그아웃 처리 중 예외 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void logout(String accessToken) throws Exception {
        kakaoApiClient.logout("Bearer " + accessToken);
    }

    /**
     * <h3>카카오 강제 로그아웃</h3>
     * <p>관리자 키를 사용하여 특정 사용자를 카카오에서 강제 로그아웃 처리합니다.</p>
     * <p>사용자 차단 등의 관리 작업 시 호출되며, 카카오 API를 통해 해당 사용자의 세션을 강제 종료합니다.</p>
     *
     * @param socialId 카카오 사용자 고유 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void forceLogout(String socialId) {
        Map<String, String> params = new HashMap<>();
        params.put("target_id_type", "user_id");
        params.put("target_id", socialId);

        try {
            kakaoApiClient.forceLogout("KakaoAK " + kakaoKeyVO.getADMIN_KEY(), params);
        } catch (Exception e) {
            throw new RuntimeException("카카오 강제 로그아웃 실패: " + e.getMessage(), e);
        }
    }

    /**
     * <h3>ID 토큰 파싱</h3>
     * <p>JWT 형식의 ID 토큰을 파싱하여 페이로드 정보를 추출합니다.</p>
     * <p>Base64 URL 디코딩 후 JSON 파싱을 수행합니다.</p>
     *
     * @param idToken 카카오 ID 토큰 (JWT 형식)
     * @return 파싱된 페이로드 맵 (sub, nickname, picture 등)
     * @author Jaeik
     * @since 2.0.0
     */
    private Map<String, Object> parseIdTokenPayload(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse id_token: " + e.getMessage(), e);
        }
    }
}
