package jaeik.bimillog.infrastructure.adapter.out.api.social.kakao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.service.SocialLoginService;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.global.vo.KakaoKeyVO;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * <h2>카카오 소셜 로그인 전략</h2>
 * <p>카카오 소셜 로그인 처리 전략 구현체입니다.</p>
 * <p>카카오 OAuth 2.0 플로우 처리, 인증 코드로 OAuth 토큰 발급, 사용자 정보 조회</p>
 * <p>참고: 카카오는 이메일 정보를 제공하지 않습니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class KakaoStrategyAdapter implements SocialStrategyPort {

    private final KakaoKeyVO kakaoKeyVO;
    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoApiClient kakaoApiClient;
    private final ObjectMapper objectMapper;

    /**
     * <h3>지원하는 소셜 제공자 반환</h3>
     * <p>카카오 전략이 지원하는 소셜 제공자를 반환합니다.</p>
     * <p>레지스트리에서 전략 자동 등록 시 사용됩니다.</p>
     *
     * @return SocialProvider.KAKAO
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialProvider getSupportedProvider() {
        return SocialProvider.KAKAO;
    }

    /**
     * <h3>카카오 OAuth 토큰 발급 및 사용자 정보 조회</h3>
     * <p>카카오 OAuth 2.0 인증 코드를 사용하여 카카오 인증 서버로부터 액세스 토큰과 리프레시 토큰을 발급받습니다.</p>
     * <p>응답에 포함된 id_token의 페이로드를 파싱하여 사용자 정보(소셜ID, 닉네임, 프로필 이미지)를 추출합니다.</p>
     * <p>별도의 사용자 정보 API 호출 없이 한 번의 요청으로 모든 필요한 정보를 얻습니다.</p>
     *
     * @param code 카카오 OAuth 2.0 인증 코드
     * @return SocialMemberProfile 소셜 토큰 및 사용자 정보를 담은 프로필 객체 (fcmToken은 null)
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
                    refreshToken,
                    null
            );
        } catch (Exception e) {
            throw new RuntimeException("Kakao token request failed: " + e.getMessage(), e);
        }
    }

    /**
     * <h3>카카오 사용자 정보</h3>
     * <p>내부적으로 발급받은 액세스 토큰으로 카카오 사용자 정보 API에서 프로필 데이터를 조회합니다.</p>
     * <p>카카오 API 응답에서 필요한 사용자 정보를 추출하여 DTO로 변환합니다.</p>
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
     * <p>카카오 관리자 API를 사용하여 특정 사용자의 카카오 계정 연결을 완전히 해제합니다.</p>
     * <p>{@link SocialLoginService}에서 회원 탈퇴 또는 계정 차단 시 호출됩니다.</p>
     * <p>카카오 관리자 키(Admin Key)를 사용하여 서버 측에서 강제로 연결을 해제하므로 사용자가 다시 로그인하려면 새로 인증 과정을 거쳐야 합니다.</p>
     *
     * @param provider 연결을 해제할 소셜 제공자 (KAKAO)
     * @param socialId 연결 해제할 카카오 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void unlink(SocialProvider provider, String socialId) {
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
     * <h3>카카오 로그아웃 처리</h3>
     * <p>사용자의 카카오 액세스 토큰을 사용하여 카카오 서버에서 로그아웃 처리를 수행합니다.</p>
     * <p>{@link SocialLoginService}에서 사용자 로그아웃 요청 처리 시 호출됩니다.</p>
     * <p>카카오 API 호출 실패 시 예외를 상위로 전파하여 서비스 레이어에서 처리하도록 합니다.</p>
     *
     * @param provider 로그아웃할 소셜 제공자 (KAKAO)
     * @param accessToken 사용자의 카카오 액세스 토큰
     * @throws Exception 카카오 API 호출 실패 시 (네트워크 오류, 인증 오류, API 서버 오류 등)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void logout(SocialProvider provider, String accessToken) throws Exception {
        kakaoApiClient.logout("Bearer " + accessToken, "application/x-www-form-urlencoded;charset=utf-8");
    }

    /**
     * <h3>ID Token 페이로드 파싱</h3>
     * <p>JWT 형식의 id_token을 파싱하여 페이로드 데이터를 추출합니다.</p>
     * <p>JWT는 header.payload.signature 구조로 되어 있으며, payload 부분을 Base64 디코딩하여 JSON 파싱합니다.</p>
     *
     * @param idToken JWT 형식의 id_token
     * @return 페이로드 데이터 맵 (sub, nickname, picture 등 포함)
     * @throws RuntimeException JWT 파싱 실패 시
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
