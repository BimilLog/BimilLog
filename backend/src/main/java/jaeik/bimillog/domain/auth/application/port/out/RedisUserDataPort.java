package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.Token;
import org.springframework.http.ResponseCookie;

import java.util.Optional;

/**
 * <h2>임시 데이터 관리 포트</h2>
 * <p>신규 사용자의 임시 데이터 관리를 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface RedisUserDataPort {

    /**
     * <h3>임시 사용자 데이터 저장</h3>
     *
     * @param uuid UUID 키
     * @param userProfile 소셜 사용자 프로필 (순수 도메인 모델)
     * @param token 토큰 정보
     * @param fcmToken Firebase Cloud Messaging 토큰 (선택적)
     */
    void saveTempData(String uuid, LoginResult.SocialUserProfile userProfile, Token token, String fcmToken);

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     *
     * @param uuid UUID 키
     * @return 순수 도메인 모델로 변환된 임시 사용자 데이터
     */
    Optional<LoginResult.TempUserData> getTempData(String uuid);

    /**
     * <h3>임시 사용자 데이터 삭제</h3>
     * <p>UUID를 사용하여 임시 사용자 데이터를 삭제합니다.</p>
     *
     * @param uuid UUID 키
     * @since 2.0.0
     * @author Jaeik
     */
    void removeTempData(String uuid);

    /**
     * <h3>임시 사용자 ID 쿠키 생성</h3>
     * <p>신규 회원가입 시 사용자의 임시 UUID를 담는 쿠키를 생성</p>
     *
     * @param uuid 임시 사용자 ID
     * @return 임시 사용자 ID 쿠키
     * @since 2.0.0
     * @author Jaeik
     */
    ResponseCookie createTempCookie(String uuid);
}