package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.Token;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>사용자 정보 저장 포트</h2>
 * <p>
 * 소셜 로그인과 회원가입 과정에서 사용자 데이터를 저장하고 인증 쿠키를 생성하는 포트입니다.
 * </p>
 * <p>기존 회원의 로그인 처리와 신규 회원의 회원가입 완료 처리를 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SaveUserPort {

    /**
     * <h3>기존 사용자 로그인 처리 및 쿠키 생성</h3>
     * <p>기존 회원의 소셜 로그인 시 사용자 정보를 업데이트하고 인증 쿠키를 생성합니다.</p>
     * <p>최신 소셜 프로필 정보 동기화와 FCM 토큰 업데이트를 함께 처리합니다.</p>
     * <p>SocialService에서 기존 회원 소셜 로그인 완료 처리 시 호출됩니다.</p>
     *
     * @param userProfile 소셜 플랫폼에서 가져온 최신 사용자 프로필 정보
     * @param token 생성된 JWT 토큰 정보
     * @param fcmToken Firebase Cloud Messaging 토큰 (푸시 알림용)
     * @return JWT 토큰이 포함된 인증 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<ResponseCookie> handleExistingUserLogin(LoginResult.SocialUserProfile userProfile, Token token, String fcmToken);

    /**
     * <h3>신규 사용자 정보 저장</h3>
     * <p>회원가입을 완료하는 신규 사용자의 정보를 데이터베이스에 저장하고 인증 쿠키를 생성합니다.</p>
     * <p>임시 데이터의 소셜 프로필 정보와 사용자 입력 닉네임을 결합하여 최종 계정을 생성합니다.</p>
     * <p>SignUpService에서 신규 사용자 회원가입 완료 처리 시 호출됩니다.</p>
     *
     * @param userName 사용자가 입력한 닉네임
     * @param uuid 임시 데이터 식별을 위한 UUID
     * @param userProfile Redis에서 복원된 소셜 사용자 프로필 정보
     * @param token 생성된 JWT 토큰 정보
     * @param fcmToken Firebase Cloud Messaging 토큰 (푸시 알림용)
     * @return JWT 토큰이 포함된 인증 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<ResponseCookie> saveNewUser(String userName, String uuid, LoginResult.SocialUserProfile userProfile, Token token, String fcmToken);



}