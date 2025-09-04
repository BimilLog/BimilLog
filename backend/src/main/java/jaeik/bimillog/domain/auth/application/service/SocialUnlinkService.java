package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.SocialUnlinkUseCase;
import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <h2>소셜 연결 해제 서비스</h2>
 * <p>SocialUnlinkUseCase의 구현체로, 소셜 로그인 연결 해제 비즈니스 로직을 처리합니다.</p>
 * <p>사용자 차단 시 소셜 플랫폼과의 연결을 해제하는 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialUnlinkService implements SocialUnlinkUseCase {

    private final SocialLoginPort socialLoginPort;

    /**
     * {@inheritDoc}
     * 
     * <p>소셜 플랫폼 API를 호출하여 연결을 해제합니다.</p>
     * <p>해제 실패 시에도 예외를 전파하여 로깅이 가능하도록 합니다.</p>
     */
    @Override
    public void unlinkSocialAccount(SocialProvider provider, String socialId) {
        log.info("소셜 연결 해제 시작 - 제공자: {}, 소셜 ID: {}", provider, socialId);
        
        socialLoginPort.unlink(provider, socialId);
        
        log.info("소셜 연결 해제 완료 - 제공자: {}, 소셜 ID: {}", provider, socialId);
    }
}