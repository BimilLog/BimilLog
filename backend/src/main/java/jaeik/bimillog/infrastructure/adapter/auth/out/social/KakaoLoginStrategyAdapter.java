package jaeik.bimillog.infrastructure.adapter.auth.out.social;

import jaeik.bimillog.domain.auth.application.port.out.SocialLoginStrategyPort;
import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.auth.entity.SocialAuthData;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.global.vo.KakaoKeyVO;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>카카오 소셜 로그인 전략</h2>
 * <p>카카오 소셜 로그인 처리 전략 구현체입니다.</p>
 * <p>카카오 OAuth 2.0 플로우 처리, 인증 코드로 토큰 발급, 사용자 정보 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class KakaoLoginStrategyAdapter implements SocialLoginStrategyPort {

    private final KakaoKeyVO kakaoKeyVO;
    private final KakaoAuthClient kakaoAuthClient;

    public KakaoLoginStrategyAdapter(KakaoKeyVO kakaoKeyVO, KakaoAuthClient kakaoAuthClient) {
        this.kakaoKeyVO = kakaoKeyVO;
        this.kakaoAuthClient = kakaoAuthClient;
    }

    /**
     * <h3>카카오 소셜 로그인 전체 처리</h3>
     * <p>카카오 OAuth 2.0 인증 코드를 받아 토큰 발급부터 사용자 정보 조회까지 전체 로그인 플로우를 처리합니다.</p>
     * <p>{@link SocialService}에서 소셜 로그인 인증 단계 처리 시 호출됩니다.</p>
     * <p>내부적으로 getToken()과 getUserInfo() 메서드를 순차적으로 호출하여 처리합니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO만 지원)
     * @param code 카카오 OAuth 2.0 인증 코드
     * @return StrategyLoginResult 로그인 결과
     * @throws IllegalArgumentException KAKAO가 아닌 제공자인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialLoginStrategyPort.StrategyLoginResult authenticate(SocialProvider provider, String code) {
        if (provider != SocialProvider.KAKAO) {
            throw new IllegalArgumentException("KakaoSocialLoginStrategy는 KAKAO 제공자만 지원합니다: " + provider);
        }
        Token token = getToken(code);
        SocialAuthData.SocialUserProfile userProfile = getUserInfo(token.getAccessToken());
        return new SocialLoginStrategyPort.StrategyLoginResult(userProfile, token);
    }

    /**
     * <h3>카카오 계정 연결 해제</h3>
     * <p>카카오 관리자 API를 사용하여 특정 사용자의 카카오 계정 연결을 완전히 해제합니다.</p>
     * <p>{@link SocialService}에서 회원 탈퇴 또는 계정 차단 시 호출됩니다.</p>
     * <p>카카오 관리자 키(Admin Key)를 사용하여 서버 측에서 강제로 연결을 해제하므로 사용자가 다시 로그인하려면 새로 인증 과정을 거쳐야 합니다.</p>
     *
     * @param provider 연결을 해제할 소셜 제공자 (KAKAO만 지원)
     * @param socialId 연결 해제할 카카오 사용자 ID
     * @throws IllegalArgumentException KAKAO가 아닌 제공자인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void unlink(SocialProvider provider, String socialId) {
        if (provider != SocialProvider.KAKAO) {
            throw new IllegalArgumentException("KakaoSocialLoginStrategy는 KAKAO 제공자만 지원합니다: " + provider);
        }
        Map<String, String> params = new HashMap<>();
        params.put("target_id_type", "user_id");
        params.put("target_id", socialId);

        try {
            kakaoAuthClient.unlink("KakaoAK " + kakaoKeyVO.getADMIN_KEY(), params);
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
     * @param provider 로그아웃할 소셜 제공자 (KAKAO만 지원)
     * @param accessToken 사용자의 카카오 액세스 토큰
     * @throws IllegalArgumentException KAKAO가 아닌 제공자인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void logout(SocialProvider provider, String accessToken) {
        if (provider != SocialProvider.KAKAO) {
            throw new IllegalArgumentException("KakaoSocialLoginStrategy는 KAKAO 제공자만 지원합니다: " + provider);
        }
        try {
            kakaoAuthClient.logout("Bearer " + accessToken, "application/x-www-form-urlencoded;charset=utf-8");
        } catch (Exception e) {
            // 로그아웃 실패 시에도 플로우를 계속 진행
        }
    }

    /**
     * <h3>카카오 액세스 토큰 발급</h3>
     * <p>카카오 OAuth 2.0 인증 코드를 사용하여 카카오 인증 서버로부터 액세스 토큰과 리프레시 토큰을 발급받습니다.</p>
     * <p>카카오 소셜 로그인 처리 내부에서 사용자 정보 조회를 위한 선행 단계로 authenticate() 메서드에서 내부적으로 호출합니다.</p>
     * <p>Authorization Code Grant 플로우를 사용하여 카카오 인증 서버에 token exchange 요청을 전송합니다.</p>
     *
     * @param code 카카오 OAuth 2.0 인증 코드
     * @return Token 도메인 Token 엔티티
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
     * <h3>카카오 사용자 정보 조회</h3>
     * <p>내부적으로 발급받은 액세스 토큰으로 카카오 사용자 정보 API에서 프로필 데이터를 조회합니다.</p>
     * <p>카카오 소셜 로그인 처리 내부에서 토큰 발급 후 사용자 정보를 획득하기 위해 authenticate() 메서드에서 내부적으로 호출합니다.</p>
     * <p>카카오 API 응답에서 필요한 사용자 정보를 추출하여 도메인 SocialUserProfile 로 변환합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @return SocialAuthData.SocialUserProfile 도메인 소셜 사용자 프로필
     * @author Jaeik
     * @since 2.0.0
     */
    @SuppressWarnings("unchecked")
    private SocialAuthData.SocialUserProfile getUserInfo(String accessToken) {
        try {
            Map<String, Object> responseBody = kakaoAuthClient.getUserInfo("Bearer " + accessToken);
            
            Map<String, Object> kakaoAccount = (Map<String, Object>) responseBody.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String socialId = String.valueOf(responseBody.get("id"));
            String nickname = (String) profile.get("nickname");
            String thumbnailImage = (String) profile.get("thumbnail_image_url");

            return new SocialAuthData.SocialUserProfile(
                    socialId,
                    null, // 카카오는 이메일을 제공하지 않음
                    SocialProvider.KAKAO,
                    nickname,
                    thumbnailImage
            );
        } catch (Exception e) {
            throw new RuntimeException("Kakao user info request failed: " + e.getMessage(), e);
        }
    }

}
