package jaeik.bimillog.domain.admin.event;

import jaeik.bimillog.infrastructure.adapter.auth.in.listener.JwtBlacklistListener;
import jaeik.bimillog.infrastructure.adapter.comment.in.listener.CommentRemoveListener;
import jaeik.bimillog.infrastructure.adapter.user.in.listener.BlacklistAddListener;

/**
 * <h2>AdminWithdrawEvent</h2>
 * <p>
 * 관리자가 대시보드에서 심각한 위반으로 판단하여 사용자 강제 탈퇴 결정을 내렸을 때 발생하는 도메인 이벤트입니다.
 * </p>
 * <p>
 * 관리자가 신고를 검토한 후 강제 탈퇴 버튼을 클릭하면 AdminCommandService.forceWithdrawUser에서 이 이벤트를 발행합니다.
 * </p>
 * <p>
 * 단순 제재와 달리 사용자의 모든 데이터를 정리하고 재가입을 차단하는 강력한 최종 조치를 위한 이벤트입니다.
 * </p>
 * <p>
 * 이벤트 기반 아키텍처를 통해 Admin 도메인과 Auth 도메인 간의 결합도를 낮추면서
 * 복잡한 탈퇴 처리 프로세스를 비동기적으로 수행할 수 있도록 합니다.
 * </p>
 * <p>
 * Auth 도메인의 AdminWithdrawListener가 이 이벤트를 구독하여 실제 강제 탈퇴 로직을 실행하고,
 * Comment 도메인에서도 해당 사용자의 댓글 정리 작업을 수행합니다.
 * </p>
 * <p>
 * Record 클래스로 구현되어 불변성을 보장하고, 컴팩트 생성자를 통해 데이터 유효성 검증 및 기본값 설정을 수행합니다.
 * </p>
 *
 * @param userId 강제 탈퇴 대상 사용자의 내부 시스템 ID
 * @param reason 강제 탈퇴 사유 (관리자 기록용)
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 * {@link JwtBlacklistListener} JWT 토큰 무효화
 * {@link CommentRemoveListener} 댓글 데이터 정리
 * {@link BlacklistAddListener} 블랙리스트 등록
 */
public record AdminWithdrawEvent(
        Long userId,
        String reason
) {
    public AdminWithdrawEvent {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }
        if (reason == null || reason.isBlank()) {
            reason = "관리자 강제 탈퇴";
        }
    }
}