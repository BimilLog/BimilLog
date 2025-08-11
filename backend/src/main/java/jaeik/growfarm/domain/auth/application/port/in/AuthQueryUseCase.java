package jaeik.growfarm.domain.auth.application.port.in;

import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.global.auth.CustomUserDetails;

/**
 * <h2>인증 조회 유스케이스</h2>
 * <p>사용자 정보 조회 관련 기능</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AuthQueryUseCase {

    /**
     * <h3>현재 로그인한 사용자 정보 조회</h3>
     * <p>현재 로그인한 사용자의 정보를 조회하여 반환</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 현재 로그인한 사용자 정보
     */
    User getUserFromUserDetails(CustomUserDetails userDetails);
}