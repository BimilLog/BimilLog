package jaeik.bimillog.domain.user.application.port.out;

import jaeik.bimillog.domain.user.application.service.WithdrawService;

/**
 * <h2>사용자 삭제 포트</h2>
 * <p>사용자 로그아웃, 탈퇴, 제재 처리를 위한 포트입니다.</p>
 * <p>로그아웃 처리, 회원 탈퇴 처리</p>
 * <p>로그아웃 쿠키 생성, 다중 로그인 지원</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface DeleteUserPort {

    /**
     * <h3>로그아웃 처리</h3>
     * <p>다중 로그인 환경에서 특정 토큰만 삭제하여 해당 기기만 로그아웃 처리합니다.</p>
     * <p>{@link WithdrawService}에서 특정 토큰 정리 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제 - 회원탈퇴용)
     * @since 2.0.0
     * @author Jaeik
     */
    void logoutUser(Long userId, Long tokenId);

    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자를 탈퇴시키고, 소셜 로그아웃을 수행합니다.</p>
     * <p>{@link WithdrawService}에서 회원 탈퇴 처리 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void performWithdrawProcess(Long userId);
}
