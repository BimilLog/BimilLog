package jaeik.growfarm.infrastructure.adapter.admin.out.persistence;

import jaeik.growfarm.domain.admin.application.port.out.AdminCommandPort;
import jaeik.growfarm.domain.auth.application.port.in.WithdrawUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>관리자 사용자 인증 어댑터</h2>
 * <p>관리자 권한으로 사용자 인증 관련 작업을 처리하기 위한 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AdminCommandAdapter implements AdminCommandPort {

    private final WithdrawUseCase withdrawUseCase;

    /**
     * <h3>사용자 강제 탈퇴</h3>
     * <p>관리자 권한으로 사용자를 강제 탈퇴 처리합니다.</p>
     * <p>인증 도메인의 WithdrawUseCase에 위임하여 헥사고날 아키텍처 원칙을 준수합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void forceWithdraw(Long userId) {
        withdrawUseCase.forceWithdraw(userId);
    }

}
