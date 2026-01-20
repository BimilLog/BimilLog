package jaeik.bimillog.domain.global.listener;

import jaeik.bimillog.domain.auth.event.MemberLoggedOutEvent;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialLogoutService;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.notification.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jaeik.bimillog.infrastructure.log.Log;

/**
 * <h2>사용자 로그아웃 이벤트 리스너</h2>
 * <p>사용자 로그아웃 시 발생하는 {@link MemberLoggedOutEvent}를 비동기로 처리합니다.</p>
 * <p>SSE 연결 종료, 소셜 플랫폼 로그아웃, FCM/JWT 토큰 삭제, 소셜 토큰 삭제를 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, message = "로그아웃 이벤트")
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberLogoutListener {
    private final SocialLogoutService socialLogoutService;
    private final SseService sseService;
    private final AuthTokenService authTokenService;

    /**
     * <h3>사용자 로그아웃 이벤트 처리</h3>
     * <p>사용자가 로그아웃할 때 발생하는 이벤트를 비동기로 처리합니다.</p>
     * <p>SSE 연결 종료, 소셜 플랫폼 로그아웃, AuthToken 삭제(FCM 토큰 포함)를 순차적으로 수행합니다.</p>
     *
     * @param memberLoggedOutEvent 로그아웃 이벤트 (memberId, authTokenId, provider 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async("memberEventExecutor")
    @EventListener
    @Transactional
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    RedisConnectionFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void memberLogout(MemberLoggedOutEvent memberLoggedOutEvent) {
        Long memberId = memberLoggedOutEvent.memberId();
        Long AuthTokenId = memberLoggedOutEvent.authTokenId();
        SocialProvider provider = memberLoggedOutEvent.provider();

        sseService.deleteEmitters(memberId, AuthTokenId);
        try {
            socialLogoutService.socialLogout(memberId, provider);
        } catch (Exception ex) {
            log.warn("소셜 로그아웃 실패 - provider: {}, memberId: {}. 이후 정리 작업은 계속 진행합니다.", provider, memberId, ex);
        }

        authTokenService.deleteTokens(memberId, AuthTokenId);
        SecurityContextHolder.clearContext();
    }

    /**
     * <h3>회원 로그아웃 처리 최종 실패 복구</h3>
     * <p>모든 재시도가 실패한 후 호출됩니다.</p>
     *
     * @param e 발생한 예외
     * @param event 회원 로그아웃 이벤트
     */
    @Recover
    public void recoverMemberLogout(Exception e, MemberLoggedOutEvent event) {
        log.error("회원 로그아웃 처리 최종 실패: memberId={}", event.memberId(), e);
    }
}
