package jaeik.bimillog.domain.member.out;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
