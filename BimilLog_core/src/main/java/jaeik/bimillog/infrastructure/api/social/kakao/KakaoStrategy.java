package jaeik.bimillog.infrastructure.api.social.kakao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * <h2>카카오 인증 전략</h2>
 * <p>카카오 OAuth 플로우를 처리하는 인증 전략 구현체입니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoStrategy implements SocialStrategy {

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
     * @param state OAuth 2.0 state 파라미터 (카카오는 사용 안 함, 무시됨)
     * @return 소셜 토큰 및 사용자 프로필 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialMemberProfile getSocialToken(String code, String state) {
        // 카카오는 state 파라미터를 사용하지 않으므로 무시
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
            throw new CustomException(ErrorCode.AUTH_SOCIAL_TOKEN_REQUEST_FAILED, e);
        }
    }

    /**
     * <h3>카카오 계정 연결 해제</h3>
     * <p>관리자 키를 사용하여 사용자의 카카오 계정 연결을 해제합니다.</p>
     * <p>회원 탈퇴 또는 사용자 차단 시 호출됩니다.</p>
     * <p>카카오는 관리자 키로 연결 해제가 가능하므로 accessToken은 사용하지 않습니다.</p>
     *
     * @param socialId 카카오 사용자 고유 ID
     * @param accessToken 소셜 플랫폼 액세스 토큰 (카카오는 사용하지 않음)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void unlink(String socialId, String accessToken) {
        Map<String, String> params = new HashMap<>();
        params.put("target_id_type", "user_id");
        params.put("target_id", socialId);

        try {
            kakaoApiClient.unlink("KakaoAK " + kakaoKeyVO.getADMIN_KEY(), params);
        } catch (Exception e) {
            log.error("카카오 연결 해제 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_SOCIAL_TOKEN_DELETE_FAILED);
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
            log.error("카카오 강제 로그아웃 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_SOCIAL_TOKEN_DELETE_FAILED);
        }
    }

    /**
     * <h3>카카오 액세스 토큰 갱신</h3>
     * <p>리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.</p>
     * <p>TODO: 카카오 토큰 갱신 API 구현 필요</p>
     *
     * @param refreshToken 카카오 리프레시 토큰
     * @return 갱신된 액세스 토큰
     * @throws Exception 토큰 갱신 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String refreshAccessToken(String refreshToken) throws Exception {
        // TODO: 카카오 토큰 갱신 API 구현 필요
        // https://kauth.kakao.com/oauth/token
        // grant_type=refresh_token
        throw new UnsupportedOperationException("카카오 토큰 갱신 기능은 아직 구현되지 않았습니다.");
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
