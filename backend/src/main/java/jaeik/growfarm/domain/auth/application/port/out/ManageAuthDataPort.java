package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.global.domain.SocialProvider;
import org.springframework.http.ResponseCookie;

import java.util.List;
import java.util.Optional;

/**
 * <h2>인증 데이터 관리 포트</h2>
 * <p>사용자 데이터 저장, 업데이트, 삭제를 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ManageAuthDataPort {

    /**
     * <h3>기존 사용자 로그인 처리 및 쿠키 생성</h3>
     * <p>기존 사용자 정보를 업데이트하고 JWT가 삽입된 쿠키를 생성합니다.</p>
     *
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @param fcmToken FCM 토큰
     * @return JWT가 삽입된 쿠키 리스트
     * @since 2.1.0
     * @author Jaeik
     */
    List<ResponseCookie> handleExistingUserLogin(SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken);

    /**
     * <h3>신규 사용자 정보 저장</h3>
     *
     * @param userName 사용자 닉네임
     * @param uuid     임시 데이터 UUID
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @param fcmToken FCM 토큰
     * @return JWT가 삽입된 쿠키 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    List<ResponseCookie> saveNewUser(String userName, String uuid, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken);



    void logoutUser(Long userId);

    /**
     * <h3>회원탈퇴 처리</h3>
     *
     * @param userId 사용자 ID
     */
    void performWithdrawProcess(Long userId);

    Optional<Token> findTokenByUserId(Long userId);

    // 새로운 사용자 조회 메서드 추가 (AuthDataAdapter에서 호출될 수 있도록)
    Optional<User> findUserByProviderAndSocialId(SocialProvider provider, String socialId);
}