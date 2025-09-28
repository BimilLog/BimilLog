package jaeik.bimillog.domain.user.application.port.out;

import jaeik.bimillog.domain.user.application.service.UserCommandService;

/**
 * <h2>사용자 명령 포트</h2>
 * <p>사용자 도메인의 명령 작업을 담당하는 아웃바운드 포트입니다.</p>
 * <p>사용자 계정 삭제 및 관련 데이터 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserCommandPort {

    /**
     * <h3>사용자 계정과 설정 삭제</h3>
     * <p>사용자 계정과 연관된 설정을 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>Native Query를 사용하여 User와 Setting을 동시에 삭제합니다.</p>
     * <p>{@link UserCommandService#removeUserAccount}에서 회원 탈퇴 처리 시 호출됩니다.</p>
     *
     * @param userId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteUserAndSetting(Long userId);
}