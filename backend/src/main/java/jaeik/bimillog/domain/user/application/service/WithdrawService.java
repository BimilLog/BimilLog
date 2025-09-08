package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.application.port.in.WithdrawUseCase;
import jaeik.bimillog.domain.user.application.port.out.DeleteUserPort;
import jaeik.bimillog.domain.user.application.port.out.UserCommandPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.BlackList;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <h2>회원 탈퇴 및 제재 서비스</h2>
 * <p>회원 탈퇴 및 제재 관련 기능을 처리하는 전용 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawService implements WithdrawUseCase {

    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;
    private final DeleteUserPort deleteUserPort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자를 탈퇴시키고, 소셜 로그아웃을 수행하며, 이벤트를 발행합니다.</p>
     * <p>탈퇴 시 해당 사용자의 모든 JWT 토큰을 블랙리스트에 등록하여 즉시 무효화합니다.</p>
     *
     * @param userDetails 현재 사용자 정보
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public List<ResponseCookie> withdraw(CustomUserDetails userDetails) {
        // 사용자 ID가 유효한지 확인하고 사용자 정보를 가져옵니다.
        Long userId = Optional.ofNullable(userDetails)
                .map(CustomUserDetails::getUserId)
                .orElseThrow(() -> new AuthCustomException(AuthErrorCode.NULL_SECURITY_CONTEXT));

        User user = userQueryPort.findById(userId).orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        // 핵심 탈퇴 로직을 수행합니다.
        performCoreWithdrawal(user);

        // 소셜 로그아웃 쿠키를 반환하여 클라이언트 측 로그아웃을 유도합니다.
        return deleteUserPort.getLogoutCookies();
    }

    /**
     * <h3>관리자 강제 탈퇴 처리</h3>
     * <p>관리자 권한으로 지정된 사용자를 강제 탈퇴 처리합니다.</p>
     * <p>강제 탈퇴 시 해당 사용자의 모든 JWT 토큰을 블랙리스트에 등록하여 즉시 차단합니다.</p>
     *
     * @param userId 탈퇴시킬 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void forceWithdraw(Long userId) {
        User user = userQueryPort.findById(userId).orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));


        // 핵심 탈퇴 로직을 수행합니다.
        performCoreWithdrawal(user);
    }

    /**
     * <h3>사용자 블랙리스트 추가</h3>
     * <p>사용자 ID를 기반으로 사용자를 조회하고 해당 사용자의 소셜 정보로 블랙리스트에 추가합니다.</p>
     * <p>중복 등록 방지를 위해 데이터베이스 UNIQUE 제약조건을 활용합니다.</p>
     *
     * @param userId 블랙리스트에 추가할 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void addToBlacklist(Long userId) {
        if (userId == null) {
            throw new UserCustomException(UserErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        BlackList blackList = BlackList.createBlackList(user.getSocialId(), user.getProvider());

        try {
            deleteUserPort.saveBlackList(blackList);
            log.info("사용자 블랙리스트 추가 완료 - userId: {}, socialId: {}, provider: {}",
                    userId, user.getSocialId(), user.getProvider());
        } catch (DataIntegrityViolationException e) {
            log.warn("이미 블랙리스트에 등록된 사용자 - userId: {}, socialId: {}",
                    userId, user.getSocialId());
        }
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
        if (userId == null) {
            throw new UserCustomException(UserErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        // 사용자 역할을 BAN으로 변경
        user.updateRole(UserRole.BAN);
        userCommandPort.save(user);

        log.info("사용자 제재 완료 - userId: {}, userName: {}, 역할 변경: BAN",
                userId, user.getUserName());
    }

    /**
     * <h3>공통 탈퇴 로직</h3>
     * <p>소셜 계정 연결 해제, DB에서 사용자 삭제, 그리고 탈퇴 이벤트 발행 등 핵심적인 탈퇴 절차를 수행합니다.</p>
     *
     * @param user 탈퇴 대상 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    private void performCoreWithdrawal(User user) {

        // DB에서 사용자 정보 삭제
        deleteUserPort.performWithdrawProcess(user.getId());

        // 탈퇴 이벤트 발행 (JWT 토큰 무효화, 데이터 정리 등은 이벤트 리스너가 처리)
        eventPublisher.publishEvent(new UserWithdrawnEvent(user.getId(), user.getSocialId(), user.getProvider()));
    }
}
