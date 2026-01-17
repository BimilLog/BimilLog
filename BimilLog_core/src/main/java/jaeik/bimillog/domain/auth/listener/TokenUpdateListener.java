package jaeik.bimillog.domain.auth.listener;

import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.member.event.MemberTokenUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jaeik.bimillog.infrastructure.log.Log;

/**
 * <h2>토큰 업데이트 이벤트 리스너</h2>
 * <p>JWT Refresh 토큰 업데이트 이벤트를 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, message = "토큰 업데이트 이벤트")
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
