package jaeik.bimillog.domain.global.listener;

import jaeik.bimillog.domain.admin.event.MemberBannedEvent;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jaeik.bimillog.infrastructure.log.Log;

/**
 * <h2>사용자 차단 이벤트 리스너</h2>
 * <p>관리자에 의한 사용자 차단 시 발생하는 {@link MemberBannedEvent}를 처리합니다.</p>
 * <p>SSE 연결 종료, 소셜 계정 강제 로그아웃, JWT 토큰 삭제, 소셜 토큰 삭제를 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, message = "회원 차단 이벤트")
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberBannedListener {
    private final SocialLogoutService socialLogoutService;
    private final AuthTokenService authTokenService;
    private final SseService sseService;

    /**
     * <h3>사용자 차단 이벤트 처리</h3>
     * <p>관리자가 사용자를 차단할 때 발생하는 이벤트를 처리합니다.</p>
     * <p>소셜 플랫폼 강제 로그아웃, JWT 토큰 무효화</p>
     *
     * @param memberBannedEvent 사용자 차단 이벤트 (memberId, socialId, provider 포함)
     * @author Jaeik
     * @since 2.1.0
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
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public void memberBanned(MemberBannedEvent memberBannedEvent) {
        Long memberId = memberBannedEvent.memberId();
        String socialId = memberBannedEvent.socialId();
        SocialProvider provider = memberBannedEvent.provider();

        sseService.deleteEmitters(memberId, null);
        socialLogoutService.forceLogout(socialId, provider);
        authTokenService.deleteTokens(memberId, null);
    }

    /**
     * <h3>회원 차단 처리 최종 실패 복구</h3>
     * <p>모든 재시도가 실패한 후 호출됩니다.</p>
     *
     * @param e 발생한 예외
     * @param event 회원 차단 이벤트
     */
    @Recover
    public void recoverMemberBanned(Exception e, MemberBannedEvent event) {
        log.error("회원 차단 처리 최종 실패: memberId={}", event.memberId(), e);
    }
}
