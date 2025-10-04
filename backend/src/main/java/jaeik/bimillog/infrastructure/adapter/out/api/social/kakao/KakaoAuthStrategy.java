package jaeik.bimillog.infrastructure.adapter.out.api.social.kakao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.global.application.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.global.vo.KakaoKeyVO;
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

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

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
                    refreshToken,
                    null
            );
        } catch (Exception e) {
            throw new RuntimeException("Kakao token request failed: " + e.getMessage(), e);
        }
    }

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

    @Override
    public void logout(String accessToken) throws Exception {
        kakaoApiClient.logout("Bearer " + accessToken, "application/x-www-form-urlencoded;charset=utf-8");
    }

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
