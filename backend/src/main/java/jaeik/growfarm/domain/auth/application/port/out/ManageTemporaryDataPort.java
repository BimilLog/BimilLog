package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.service.auth.TempUserDataManager;

/**
 * <h2>임시 데이터 관리 포트</h2>
 * <p>신규 사용자의 임시 데이터 관리를 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ManageTemporaryDataPort {

    /**
     * <h3>임시 사용자 데이터 저장</h3>
     *
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @param fcmToken FCM 토큰
     * @return UUID 키
     */
    String saveTempData(SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken);

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     *
     * @param uuid UUID 키
     * @return 임시 사용자 데이터
     */
    TempUserDataManager.TempUserData getTempData(String uuid);
}