package jaeik.bimillog.domain.auth.listener;

import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.member.event.MemberTokenUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenUpdateListener {
    private final AuthTokenService authTokenService;

    @EventListener
    public void updateJwtRefreshToken(MemberTokenUpdateEvent memberTokenUpdateEvent) {
        String newJwtRefreshToken = memberTokenUpdateEvent.getNewJwtRefreshToken();
        Long tokenId = memberTokenUpdateEvent.getTokenId();
        authTokenService.updateJwtRefreshToken(tokenId, newJwtRefreshToken);
    }
}
