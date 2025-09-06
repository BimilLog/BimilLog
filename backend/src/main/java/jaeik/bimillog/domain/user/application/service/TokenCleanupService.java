package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.application.port.in.TokenCleanupUseCase;
import jaeik.bimillog.domain.user.application.port.out.DeleteUserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>토큰 정리 서비스</h2>
 * <p>TokenCleanupUseCase의 구현체로, 토큰 정리 비즈니스 로직을 처리합니다.</p>
 * <p>다중 기기 로그인 환경에서 특정 토큰만 정리하는 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService implements TokenCleanupUseCase {

    private final DeleteUserPort deleteUserPort;

    /**
     * {@inheritDoc}
     * 
     * <p>다중 기기 로그인 지원을 위해 특정 토큰만 삭제합니다.</p>
     * <p>다른 기기의 로그인 상태는 유지됩니다.</p>
     */
    @Override
    public void cleanupSpecificToken(Long userId, Long tokenId) {
        log.info("특정 토큰 정리 시작 - 사용자 ID: {}, 토큰 ID: {}", userId, tokenId);
        
        // 다중 로그인 지원: 특정 토큰만 삭제 (다른 기기의 로그인 상태 유지)
        deleteUserPort.logoutUser(userId, tokenId);
        
        log.info("특정 토큰 정리 완료 - 사용자 ID: {}, 토큰 ID: {}", userId, tokenId);
    }
}