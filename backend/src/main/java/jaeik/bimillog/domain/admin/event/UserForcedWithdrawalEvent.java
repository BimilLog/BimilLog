package jaeik.bimillog.domain.admin.event;

import jaeik.bimillog.domain.user.entity.SocialProvider;

/**
 * <h2>UserForcedWithdrawalEvent</h2>
 * <p>사용자 강제 탈퇴 시 발생하는 이벤트입니다.</p>
 * <p>AdminCommandService.forceWithdrawUser에서 이 이벤트를 발행합니다.</p>
 * <p>사용자의 모든 데이터를 정리하고 재가입을 차단하는 최종 조치입니다.</p>
 *
 * @param userId 강제 탈퇴 대상 사용자의 ID
 * @author Jaeik
 * @version 2.0.0
 */
public record UserForcedWithdrawalEvent(
        Long userId,
        String socialId,
        SocialProvider provider,
        String reason
) {}