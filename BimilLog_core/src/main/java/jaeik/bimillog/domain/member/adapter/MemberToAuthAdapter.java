package jaeik.bimillog.domain.member.adapter;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberToAuthAdapter {
    private final SocialTokenService socialTokenService;
    private final AuthTokenService authTokenService;

    public Optional<SocialToken> getSocialToken(Long memberId) {
        return socialTokenService.getSocialToken(memberId);
    }

    public AuthToken saveAuthToken(AuthToken authToken) {
        return authTokenService.save(authToken);
    }

    public SocialToken saveSocialToken(SocialToken socialToken) {
        return socialTokenService.save(socialToken);
    }

    /**
     * <h3>JWT 리프레시 토큰 업데이트</h3>
     * <p>JWT 리프레시 토큰을 갱신합니다.</p>
     *
     * @param tokenId 토큰 ID
     * @param newJwtRefreshToken 새로운 JWT 리프레시 토큰
     */
    @Transactional
    public void updateJwtRefreshToken(Long tokenId, String newJwtRefreshToken) {
        authTokenService.updateJwtRefreshToken(tokenId, newJwtRefreshToken);
    }
}
