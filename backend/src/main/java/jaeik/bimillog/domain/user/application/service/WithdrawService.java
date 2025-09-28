package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.application.port.in.WithdrawUseCase;
import jaeik.bimillog.domain.user.application.port.out.DeleteUserPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>회원 탈퇴 서비스</h2>
 * <p>WithdrawUseCase의 구현체로 회원 탈퇴 및 제재 로직을 담당합니다.</p>
 * <p>회원 탈퇴, 관리자 강제 탈퇴, 사용자 제재</p>
 * <p>블랙리스트 추가, JWT 토큰 무효화, 이벤트 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawService implements WithdrawUseCase {

    private final DeleteUserPort deleteUserPort;
    private final UserQueryPort userQueryPort;
    private final GlobalCookiePort globalCookiePort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자를 탈퇴시키고, 소셜 로그아웃을 수행하며, 이벤트를 발행합니다.</p>
     *
     * @param userDetails 현재 사용자 정보
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public List<ResponseCookie> withdraw(CustomUserDetails userDetails) {
        User user = userQueryPort.findById(userDetails.getUserId()).orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));
        // 핵심 탈퇴 로직을 수행합니다.
        performCoreWithdrawal(userDetails);

        // 소셜 로그아웃 쿠키를 반환하여 클라이언트 측 로그아웃을 유도합니다.
        return globalCookiePort.getLogoutCookies();
    }

    /**
     * <h3>사용자 역할을 BAN으로 변경</h3>
     * <p>사용자의 역할을 BAN으로 변경하여 일정 기간 서비스 이용을 제한합니다.</p>
     * <p>JWT 토큰 무효화는 JwtBlacklistEventListener가 이벤트를 통해 처리합니다.</p>
     *
     * @param userId 제재할 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void banUser(Long userId) {
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        // 사용자 역할을 BAN으로 변경 (JPA 변경 감지로 자동 저장)
        user.updateRole(UserRole.BAN);

        log.info("사용자 제재 완료 - userId: {}, userName: {}, 역할 변경: BAN",
                userId, user.getUserName());
    }

    /**
     * <h3>공통 탈퇴 로직</h3>
     * <p>소셜 계정 연결 해제, DB에서 사용자 삭제, 그리고 탈퇴 이벤트 발행 등 핵심적인 탈퇴 절차를 수행합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    private void performCoreWithdrawal(CustomUserDetails userDetails) {

        // DB에서 사용자 정보 삭제
        deleteUserPort.performWithdrawProcess(userDetails.getUserId());

        // 탈퇴 이벤트 발행 (JWT 토큰 무효화, 데이터 정리 등은 이벤트 리스너가 처리)
        eventPublisher.publishEvent(new UserWithdrawnEvent(userDetails));
    }


}
