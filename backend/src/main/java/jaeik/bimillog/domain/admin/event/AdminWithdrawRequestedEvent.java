package jaeik.bimillog.domain.admin.event;

/**
 * <h2>관리자 강제 탈퇴 요청 이벤트</h2>
 * <p>관리자가 사용자를 강제 탈퇴시킬 때 발생하는 이벤트입니다.</p>
 * <p>이 이벤트는 Auth 도메인에서 처리되어 실제 탈퇴 프로세스를 수행합니다.</p>
 *
 * @param userId 탈퇴 대상 사용자 ID
 * @param reason 탈퇴 사유
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public record AdminWithdrawRequestedEvent(
        Long userId,
        String reason
) {
    public AdminWithdrawRequestedEvent {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            reason = "관리자 강제 탈퇴";
        }
    }
}