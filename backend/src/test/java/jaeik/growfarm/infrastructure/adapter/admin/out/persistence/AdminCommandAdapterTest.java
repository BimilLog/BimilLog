package jaeik.growfarm.infrastructure.adapter.admin.out.persistence;

import jaeik.growfarm.domain.auth.application.port.in.WithdrawUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * <h2>AdminCommandAdapter 테스트</h2>
 * <p>관리자 명령 어댑터의 외부 시스템 통합 테스트</p>
 * <p>WithdrawUseCase와의 연동을 검증하는 통합 테스트</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class AdminCommandAdapterTest {

    @Mock
    private WithdrawUseCase withdrawUseCase;

    @InjectMocks
    private AdminCommandAdapter adminCommandAdapter;

    /**
     * <h3>사용자 강제 탈퇴 처리 테스트</h3>
     * <p>관리자 권한으로 사용자 강제 탈퇴가 올바르게 WithdrawUseCase에 위임되는지 검증</p>
     */
    @Test
    void shouldDelegateToWithdrawUseCase_WhenForceWithdraw() {
        // Given: 탈퇴시킬 사용자 ID
        Long userId = 1L;

        // When: 강제 탈퇴 실행
        adminCommandAdapter.forceWithdraw(userId);

        // Then: WithdrawUseCase.forceWithdraw가 올바른 파라미터로 호출되었는지 검증
        verify(withdrawUseCase).forceWithdraw(userId);
    }

    /**
     * <h3>null 사용자 ID로 강제 탈퇴 처리 테스트</h3>
     * <p>null 값이 그대로 WithdrawUseCase에 전달되는지 검증</p>
     * <p>어댑터는 비즈니스 로직을 포함하지 않으므로 validation은 하위 계층에서 처리</p>
     */
    @Test
    void shouldPassNullUserId_WhenForceWithdrawWithNull() {
        // Given: null 사용자 ID
        Long userId = null;

        // When: 강제 탈퇴 실행
        adminCommandAdapter.forceWithdraw(userId);

        // Then: WithdrawUseCase.forceWithdraw가 null과 함께 호출되었는지 검증
        verify(withdrawUseCase).forceWithdraw(null);
    }
}