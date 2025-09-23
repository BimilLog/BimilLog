package jaeik.bimillog.infrastructure.adapter.out.api.social.kakao;

import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.global.vo.KakaoKeyVO;
import org.springframework.stereotype.Component;

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
public class KakaoStrategyAdapter implements SocialStrategyPort {

    private final KakaoKeyVO kakaoKeyVO;
    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoApiClient kakaoApiClient;

    public KakaoStrategyAdapter(KakaoKeyVO kakaoKeyVO, KakaoAuthClient kakaoAuthClient, KakaoApiClient kakaoApiClient) {
        this.kakaoKeyVO = kakaoKeyVO;
        this.kakaoAuthClient = kakaoAuthClient;
        this.kakaoApiClient = kakaoApiClient;
    }

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
     * <h3>카카오 소셜 로그인 전체 처리</h3>
     * <p>카카오 OAuth 2.0 인증 코드를 받아 토큰 발급부터 사용자 정보 조회까지 전체 로그인 플로우를 처리합니다.</p>
     * <p>{@link SocialService}에서 소셜 로그인 인증 단계 처리 시 호출됩니다.</p>
     * <p>내부적으로 getToken()과 getUserInfo() 메서드를 순차적으로 호출하여 처리합니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO)
     * @param code 카카오 OAuth 2.0 인증 코드
     * @return SocialUserProfile 사용자 프로필과 OAuth 토큰(액세스/리프레시) 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialUserProfile authenticate(SocialProvider provider, String code) {
        Token token = getToken(code);
        return getUserInfo(token);
    }

    /**
     * <h3>카카오 OAuth 토큰 발급 (private)</h3>
     * <p>카카오 OAuth 2.0 인증 코드를 사용하여 카카오 인증 서버로부터 액세스 토큰과 리프레시 토큰을 발급받습니다.</p>
     * <p>카카오 소셜 로그인 처리 내부에서만 사용되는 private 메서드로, authenticate() 메서드에서 내부적으로 호출합니다.</p>
     * <p>Authorization Code Grant 플로우를 사용하여 카카오 인증 서버에 token exchange 요청을 전송합니다.</p>
     *
     * @param code 카카오 OAuth 2.0 인증 코드
     * @return Token 도메인 Token 엔티티 (액세스/리프레시 토큰 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    private Token getToken(String code) {
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

            return Token.createTemporaryToken(
                    (String) responseBody.get("access_token"),
                    (String) responseBody.get("refresh_token")
            );
        } catch (Exception e) {
            throw new RuntimeException("Kakao token request failed: " + e.getMessage(), e);
        }
    }



    /**
     * <h3>카카오 사용자 정보 조회 (private)</h3>
     * <p>내부적으로 발급받은 액세스 토큰으로 카카오 사용자 정보 API에서 프로필 데이터를 조회합니다.</p>
     * <p>카카오 소셜 로그인 처리 내부에서만 사용되는 private 메서드로, authenticate() 메서드에서 내부적으로 호출합니다.</p>
     * <p>카카오 API 응답에서 필요한 사용자 정보를 추출하여 도메인 SocialUserProfile 로 변환합니다.</p>
     *
     * @param token OAuth 토큰 정보 (액세스/리프레시)
     * @return SocialUserProfile 도메인 소셜 사용자 프로필 (OAuth 토큰 포함, 이메일은 null)
     * @author Jaeik
     * @since 2.0.0
     */
    @SuppressWarnings("unchecked")
    private SocialUserProfile getUserInfo(Token token) {
        try {
            Map<String, Object> responseBody = kakaoApiClient.getUserInfo("Bearer " + token.getAccessToken());

            Map<String, Object> kakaoAccount = (Map<String, Object>) responseBody.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String socialId = String.valueOf(responseBody.get("id"));
            String nickname = (String) profile.get("nickname");
            String thumbnailImage = (String) profile.get("thumbnail_image_url");

            return new SocialUserProfile(
                    socialId,
                    null, // 카카오는 보안정책상 이메일을 제공하지 않음
                    SocialProvider.KAKAO,
                    nickname,
                    thumbnailImage,
                    token
            );
        } catch (Exception e) {
            throw new RuntimeException("Kakao user info request failed: " + e.getMessage(), e);
        }
    }

    /**
     * <h3>카카오 계정 연결 해제</h3>
     * <p>카카오 관리자 API를 사용하여 특정 사용자의 카카오 계정 연결을 완전히 해제합니다.</p>
     * <p>{@link SocialService}에서 회원 탈퇴 또는 계정 차단 시 호출됩니다.</p>
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
     * <p>{@link SocialService}에서 사용자 로그아웃 요청 처리 시 호출됩니다.</p>
     * <p>카카오 서버 오류 시에도 로그아웃 플로우를 방해하지 않도록 예외를 무시합니다.</p>
     *
     * @param provider 로그아웃할 소셜 제공자 (KAKAO)
     * @param accessToken 사용자의 카카오 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void logout(SocialProvider provider, String accessToken) {
        try {
            kakaoApiClient.logout("Bearer " + accessToken, "application/x-www-form-urlencoded;charset=utf-8");
        } catch (Exception e) {
            // 로그아웃 실패 시에도 플로우를 계속 진행
        }
    }



}
