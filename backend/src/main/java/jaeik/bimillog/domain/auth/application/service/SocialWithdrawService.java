package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.admin.event.UserForcedWithdrawalEvent;
import jaeik.bimillog.domain.auth.application.port.in.SocialWithdrawUseCase;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocialWithdrawService implements SocialWithdrawUseCase {

    private final SocialStrategyRegistryPort strategyRegistryPort;


    /**
     * <h3>소셜 계정 연동 해제</h3>
     * <p>사용자의 소셜 플랫폼 계정 연동을 해제합니다.</p>
     * <p>소셜 플랫폼 API를 호출하여 앱 연동을 완전히 차단합니다.</p>
     * <p>{@link UserWithdrawnEvent}, {@link UserForcedWithdrawalEvent} 이벤트 발생 시 소셜 계정 정리를 위해 호출됩니다.</p>
     *
     * @param provider 연동 해제할 소셜 플랫폼 제공자
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void unlinkSocialAccount(SocialProvider provider, String socialId) {
        log.info("소셜 연결 해제 시작 - 제공자: {}, 소셜 ID: {}", provider, socialId);

        SocialStrategyPort strategy = strategyRegistryPort.getStrategy(provider);
        strategy.unlink(provider, socialId);

        log.info("소셜 연결 해제 완료 - 제공자: {}, 소셜 ID: {}", provider, socialId);
    }
}
