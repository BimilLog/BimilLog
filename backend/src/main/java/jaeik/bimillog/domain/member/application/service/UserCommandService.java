package jaeik.bimillog.domain.member.application.service;

import jaeik.bimillog.domain.member.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.member.application.port.out.UserCommandPort;
import jaeik.bimillog.domain.member.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.exception.UserCustomException;
import jaeik.bimillog.domain.member.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.adapter.in.member.web.UserCommandController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 명령 서비스</h2>
 * <p>UserCommandUseCase의 구현체로 사용자 정보 수정 로직을 담당합니다.</p>
 * <p>사용자 알림 설정 수정, 닉네임 변경</p>
 * <p>Race Condition 방지, 데이터베이스 제약조건 예외 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCommandService implements UserCommandUseCase {

    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;

    /**
     * <h3>사용자 설정 수정</h3>
     * <p>사용자의 알림 설정을 수정합니다.</p>
     * <p>{@link UserCommandController}에서 설정 수정 API 요청 시 호출됩니다.</p>
     *
     * @param userId    사용자 ID
     * @param newSetting 수정할 설정 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void updateUserSettings(Long userId, Setting newSetting) {
        Member member = userQueryPort.findById(userId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        member.updateSettings(
                newSetting.isMessageNotification(),
                newSetting.isCommentNotification(),
                newSetting.isPostFeaturedNotification()
        );
        // JPA 변경 감지로 자동 저장
    }

    /**
     * <h3>닉네임 변경</h3>
     * <p>사용자의 닉네임을 변경합니다.</p>
     * <p>Race Condition 방지를 위해 데이터베이스 UNIQUE 제약조건 위반 예외를 처리합니다.</p>
     * <p>{@link UserCommandController}에서 닉네임 변경 API 요청 시 호출됩니다.</p>
     *
     * @param userId      사용자 ID
     * @param newUserName 새로운 닉네임
     * @throws UserCustomException EXISTED_NICKNAME - 닉네임이 중복된 경우
     * @throws UserCustomException USER_NOT_FOUND - 사용자를 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void updateUserName(Long userId, String newUserName) {
        Member member = userQueryPort.findById(userId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        try {
            // 엔티티가 비즈니스 로직 처리. JPA 변경 감지로 자동 저장되므로 명시적 save는 불필요합니다.
            member.changeUserName(newUserName, userQueryPort);
        } catch (DataIntegrityViolationException e) {
            // Race Condition 발생 시: 다른 사용자가 동시에 같은 닉네임으로 변경한 경우
            log.warn("닉네임 경쟁 상태 감지됨 - 사용자 ID: {}, 새 닉네임: {}", userId, newUserName, e);
            throw new UserCustomException(UserErrorCode.EXISTED_NICKNAME);
        }
    }

    /**
     * <h3>사용자 계정 삭제</h3>
     * <p>회원 탈퇴 시 사용자 계정과 설정을 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>Native Query를 통해 User와 Setting을 원자적으로 삭제합니다.</p>
     * <p>UserWithdrawListener에서 회원 탈퇴 이벤트 처리 흐름 중 호출됩니다.</p>
     *
     * @param userId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void removeUserAccount(Long userId) {
        log.info("사용자 계정 삭제 시작 - userId: {}", userId);
        userCommandPort.deleteUserAndSetting(userId);
        log.info("사용자 계정 및 설정 삭제 완료 - userId: {}", userId);
    }
}
