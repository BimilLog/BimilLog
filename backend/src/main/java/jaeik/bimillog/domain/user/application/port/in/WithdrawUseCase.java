
package jaeik.bimillog.domain.user.application.port.in;

import jaeik.bimillog.infrastructure.adapter.in.admin.web.AdminCommandController;
import jaeik.bimillog.infrastructure.adapter.in.user.web.UserCommandController;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>회원 탈퇴 유스케이스</h2>
 * <p>회원 탈퇴, 제재, 로그아웃 관련 기능을 처리하는 입력 포트입니다.</p>
 * <p>회원 탈퇴 처리, 관리자 강제 탈퇴, 사용자 제재</p>
 * <p>블랙리스트 추가, 특정 토큰 정리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface WithdrawUseCase {

    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자 회원 탈퇴 요청을 처리하고 쿠키를 반환합니다.</p>
     * <p>{@link UserCommandController}에서 회원 탈퇴 API 요청 시 호출됩니다.</p>
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
     * <p>{@link AdminCommandController}에서 관리자 강제 탈퇴 API 요청 시 호출됩니다.</p>
     *
     * @param userId 탈퇴시킬 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void forceWithdraw(Long userId);


    /**
     * <h3>사용자 역할을 BAN으로 변경</h3>
     * <p>사용자 제재 시 해당 사용자의 역할을 BAN으로 변경하여 서비스 이용을 제한합니다.</p>
     * <p>{@link AdminCommandController}에서 사용자 제재 API 요청 시 호출됩니다.</p>
     *
     * @param userId 제재할 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void banUser(Long userId);

    /**
     * <h3>특정 토큰 정리</h3>
     * <p>사용자 로그아웃 시 특정 토큰만 정리합니다.</p>
     * <p>다중 기기 로그인 환경에서 다른 기기의 로그인 상태는 유지됩니다.</p>
     *
     * @param userId  사용자 ID
     * @param tokenId 정리할 토큰 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void cleanupSpecificToken(Long userId, Long tokenId);
}
