package jaeik.bimillog.infrastructure.api.social.naver;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.global.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>네이버 인증 전략</h2>
 * <p>네이버 OAuth 플로우를 처리하는 인증 전략 구현체입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverAuthStrategy implements SocialAuthStrategy {

    private final NaverKeyVO naverKeyVO;
    private final NaverAuthClient naverAuthClient;
    private final NaverApiClient naverApiClient;

    /**
     * <h3>소셜 제공자 반환</h3>
     * <p>이 전략이 지원하는 소셜 제공자를 반환합니다.</p>
     *
     * @return NAVER 제공자
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialProvider getProvider() {
        return SocialProvider.NAVER;
    }

    /**
     * <h3>네이버 토큰 및 사용자 프로필 조회</h3>
     * <p>OAuth 인증 코드를 사용하여 네이버 액세스/리프레시 토큰을 발급받고,</p>
     * <p>액세스 토큰으로 사용자 프로필 정보를 추출합니다.</p>
     *
     * @param code OAuth 2.0 인증 코드
     * @param state OAuth 2.0 state 파라미터 (CSRF 방지용, 네이버에서 필수)
     * @return 소셜 토큰 및 사용자 프로필 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialMemberProfile getSocialToken(String code, String state) {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", naverKeyVO.getCLIENT_ID());
        params.put("client_secret", naverKeyVO.getCLIENT_SECRET());
        params.put("code", code);
        params.put("state", state); // 프론트엔드에서 전달받은 state 값 사용
        params.put("redirect_uri", naverKeyVO.getREDIRECT_URI());

        try {
            // 1. 토큰 발급 요청
            Map<String, Object> tokenResponse = naverAuthClient.getToken(params);
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");

            // 2. 사용자 프로필 조회
            Map<String, Object> userResponse = naverApiClient.getUserInfo("Bearer " + accessToken);

            // 네이버 API 응답 구조: { "resultcode": "00", "message": "success", "response": { ... } }
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) userResponse.get("response");

            String socialId = (String) response.get("id");
            String nickname = (String) response.get("nickname");
            String profileImage = (String) response.get("profile_image");
            String email = (String) response.get("email");

            return SocialMemberProfile.of(
                    socialId,
                    email,
                    SocialProvider.NAVER,
                    nickname,
                    profileImage,
                    accessToken,
                    refreshToken
            );
        } catch (Exception e) {
            log.error("네이버 토큰 요청 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_SOCIAL_TOKEN_REQUEST_FAILED);
        }
    }

    /**
     * <h3>네이버 연결 해제</h3>
     * <p>네이버 액세스 토큰을 삭제하여 연결을 해제합니다.</p>
     * <p>회원 탈퇴 시 호출되며, DB 데이터 삭제는 상위 서비스에서 처리합니다.</p>
     *
     * @param socialId 네이버 사용자 고유 ID
     * @param accessToken 네이버 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void unlink(String socialId, String accessToken) {
        deleteAccessToken(accessToken);
        log.info("네이버 연결 해제 완료 - socialId: {}", socialId);
    }

    /**
     * <h3>네이버 로그아웃</h3>
     * <p>액세스 토큰을 삭제하여 네이버 세션을 종료합니다.</p>
     * <p>DB 데이터는 유지됩니다.</p>
     *
     * @param accessToken 네이버 액세스 토큰
     * @throws Exception 로그아웃 처리 중 예외 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void logout(String accessToken) throws Exception {
    }

    /**
     * <h3>네이버 강제 로그아웃</h3>
     * <p>네이버는 관리자 키를 제공하지 않으므로 강제 로그아웃을 지원하지 않습니다.</p>
     * <p>사용자 차단 시에는 DB에서만 처리합니다.</p>
     *
     * @param socialId 네이버 사용자 고유 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void forceLogout(String socialId) {
        log.warn("네이버는 강제 로그아웃을 지원하지 않습니다. socialId: {}", socialId);
    }

    /**
     * <h3>네이버 액세스 토큰 갱신</h3>
     * <p>리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.</p>
     * <p>액세스 토큰 만료 시 자동으로 갱신하여 사용자 재로그인을 방지합니다.</p>
     *
     * @param refreshToken 네이버 리프레시 토큰
     * @return 갱신된 액세스 토큰
     * @throws Exception 토큰 갱신 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String refreshAccessToken(String refreshToken) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("client_id", naverKeyVO.getCLIENT_ID());
        params.put("client_secret", naverKeyVO.getCLIENT_SECRET());
        params.put("refresh_token", refreshToken);

        try {
            Map<String, Object> response = naverAuthClient.refreshToken(params);
            String newAccessToken = (String) response.get("access_token");

            if (newAccessToken == null || newAccessToken.isEmpty()) {
                throw new RuntimeException("네이버 토큰 갱신 응답에 access_token이 없습니다.");
            }

            log.info("네이버 액세스 토큰 갱신 성공");
            return newAccessToken;
        } catch (Exception e) {
            log.error("네이버 토큰 갱신 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_SOCIAL_TOKEN_REFRESH_FAILED);
        }
    }

    /**
     * <h3>액세스 토큰 삭제 (내부 유틸 메서드)</h3>
     * <p>네이버 토큰 삭제 API를 호출하여 액세스 토큰을 무효화합니다.</p>
     * <p>로그아웃과 연결 해제에서 공통으로 사용됩니다.</p>
     *
     * @param accessToken 삭제할 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteAccessToken(String accessToken) {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "delete");
        params.put("client_id", naverKeyVO.getCLIENT_ID());
        params.put("client_secret", naverKeyVO.getCLIENT_SECRET());
        params.put("access_token", accessToken);
        params.put("service_provider", "NAVER");

        try {
            naverAuthClient.deleteToken(params);
            log.info("네이버 액세스 토큰 삭제 완료");
        } catch (Exception e) {
            log.error("네이버 토큰 삭제 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_SOCIAL_TOKEN_DELETE_FAILED);
        }
    }
}
