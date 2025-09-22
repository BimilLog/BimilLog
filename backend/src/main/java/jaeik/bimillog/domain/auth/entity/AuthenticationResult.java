package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.user.entity.Token;

/**
 * <h3>인증 결과 데이터</h3>
 * <p>소셜 플랫폼 OAuth 인증 완료 후 반환되는 결과 데이터입니다.</p>
 * <p>소셜 사용자 프로필과 토큰 정보를 포함하여 인증 플로우에서 사용됩니다.</p>
 * <p>{@link jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort}의 authenticate 메서드 반환값으로 사용됩니다.</p>
 *
 * @param userProfile 소셜 플랫폼에서 받은 사용자 프로필 정보
 * @param token 소셜 로그인으로 발급받은 토큰 정보
 * @author Jaeik
 * @since 2.0.0
 */
public record AuthenticationResult(
        SocialAuthData.SocialUserProfile userProfile,
        Token token
) {}