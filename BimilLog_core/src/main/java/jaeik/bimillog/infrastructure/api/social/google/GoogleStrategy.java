package jaeik.bimillog.infrastructure.api.social.google;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * <h2>구글 인증 전략</h2>
 * <p>구글 OAuth HTTP/REST 플로우를 처리합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleStrategy implements SocialStrategy {

    private final GoogleKeyVO googleKeyVO;
    private final GoogleAuthClient googleAuthClient;
    private final GoogleUserInfoClient googleUserInfoClient;

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public SocialMemberProfile getSocialToken(String code, String state) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleKeyVO.getCLIENT_ID());
        params.add("client_secret", googleKeyVO.getCLIENT_SECRET());
        params.add("redirect_uri", googleKeyVO.getREDIRECT_URI());
        params.add("grant_type", "authorization_code");

        try {
            GoogleTokenResponse tokenResponse = googleAuthClient.requestToken(params);
            String accessToken = tokenResponse.accessToken();
            if (!StringUtils.hasText(accessToken)) {
                throw new IllegalStateException("구글 응답에 access_token이 없습니다.");
            }

            GoogleUserInfoResponse userInfo = googleUserInfoClient.getUserInfo("Bearer " + accessToken);
            String nickname = resolveDisplayName(userInfo);

            return SocialMemberProfile.of(
                    userInfo.sub(),
                    userInfo.email(),
                    SocialProvider.GOOGLE,
                    nickname,
                    userInfo.picture(),
                    accessToken,
                    tokenResponse.refreshToken()
            );
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("구글 토큰 요청 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_SOCIAL_TOKEN_REQUEST_FAILED);
        }
    }

    @Override
    public void unlink(String socialId, String accessToken) {
        revokeToken(accessToken);
    }

    @Override
    public void logout(String accessToken) {
        log.info("구글 로그아웃 요청 처리 - 액세스 토큰은 유지됩니다. accessToken 존재 여부: {}", StringUtils.hasText(accessToken));
    }

    @Override
    public void forceLogout(String socialId) {
        log.warn("구글은 강제 로그아웃 API를 제공하지 않습니다. socialId: {}", socialId);
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleKeyVO.getCLIENT_ID());
        params.add("client_secret", googleKeyVO.getCLIENT_SECRET());
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        try {
            GoogleTokenResponse response = googleAuthClient.refreshToken(params);
            String newAccessToken = response.accessToken();
            if (!StringUtils.hasText(newAccessToken)) {
                throw new IllegalStateException("구글 토큰 갱신 응답에 access_token이 없습니다.");
            }
            return newAccessToken;
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("구글 토큰 갱신 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_SOCIAL_TOKEN_REFRESH_FAILED);
        }
    }

    private void revokeToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("구글 토큰이 없어 revoke 요청을 생략합니다.");
            return;
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", token);

        try {
            googleAuthClient.revokeToken(params);
        } catch (Exception e) {
            log.error("구글 토큰 폐기 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_SOCIAL_TOKEN_DELETE_FAILED);
        }
    }

    private String resolveDisplayName(GoogleUserInfoResponse userInfo) {
        if (StringUtils.hasText(userInfo.name())) {
            return userInfo.name();
        }
        if (StringUtils.hasText(userInfo.givenName())) {
            return userInfo.givenName();
        }
        if (StringUtils.hasText(userInfo.email())) {
            return userInfo.email();
        }
        return "Google User";
    }
}
