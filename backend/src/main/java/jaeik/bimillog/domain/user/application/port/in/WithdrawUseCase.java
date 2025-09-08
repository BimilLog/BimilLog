
package jaeik.bimillog.domain.user.application.port.in;

import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>회원 탈퇴 유스케이스</h2>
 * <p>사용자 회원 탈퇴를 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface WithdrawUseCase {

    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자 회원 탈퇴 요청을 처리하고 쿠키를 반환합니다.</p>
     *
     * @param userDetails 사용자 세부 정보
     * @return 쿠키 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    List<ResponseCookie> withdraw(CustomUserDetails userDetails);

    /**
     * <h3>관리자 강제 탈퇴 처리</h3>
     * <p>관리자 권한으로 지정된 사용자를 강제 탈퇴 처리합니다.</p>
     *
     * @param userId 탈퇴시킬 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void forceWithdraw(Long userId);
}
