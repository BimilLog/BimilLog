package jaeik.bimillog.domain.auth.application.port.in;

import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.domain.member.event.UserWithdrawnEvent;
import jaeik.bimillog.infrastructure.adapter.in.global.listener.UserWithdrawListener;

/**
 * <h2>소셜 탈퇴 유스케이스</h2>
 * <p>사용자의 회원탈퇴 요청을 처리하는 유스케이스입니다.</p>
 * <p>소셜 플랫폼 회원탈퇴 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialWithdrawUseCase {

    /**
     * <h3>소셜 계정 연동 해제</h3>
     * <p>사용자 차단이나 회원 탈퇴 시 소셜 플랫폼과의 연결을 해제합니다.</p>
     * <p>해제 실패 시에도 사용자 차단/탈퇴 프로세스는 계속 진행됩니다.</p>
     * <p>{@link UserWithdrawnEvent}, {@link UserForcedWithdrawalEvent} 이벤트 발생 시 {@link UserWithdrawListener}에서 호출됩니다.</p>
     *
     * @param provider 소셜 제공자 (KAKAO 등)
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void unlinkSocialAccount(SocialProvider provider, String socialId);
}
