package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.SignUpService;
import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>사용자 정보 저장 포트</h2>
 * <p>소셜 로그인과 회원가입 과정에서 사용자 데이터를 저장하는 포트입니다.</p>
 * <p>기존 사용자 로그인 처리, 신규 사용자 곀4정 생성, Token 엔티티 저장, JWT 쿠키 발급</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SaveUserPort {

    /**
     * <h3>기존 사용자 로그인 처리</h3>
     * <p>기존 회원의 소셜 로그인 시 사용자 정보를 업데이트하고 JWT 인증 쿠키를 생성합니다.</p>
     * <p>최신 소셜 프로필 정보 동기화, Token 엔티티 생성/저장, FCM 토큰 등록을 처리합니다.</p>
     * <p>{@link SocialService}에서 기존 회원 소셜 로그인 완료 처리 시 호출됩니다.</p>
     *
     * @param userProfile 소셜 플랫폼에서 가져온 최신 사용자 프로필 정보 (OAuth 액세스/리프레시 토큰 포함)
     * @param fcmToken Firebase Cloud Messaging 토큰 (푸시 알림용)
     * @return JWT 토큰이 포함된 인증 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<ResponseCookie> handleExistingUserLogin(SocialUserProfile userProfile, String fcmToken);

    /**
     * <h3>신규 사용자 정보 저장</h3>
     * <p>회원가입을 완료하는 신규 사용자의 정보를 데이터베이스에 저장하고 JWT 인증 쿠키를 생성합니다.</p>
     * <p>사용자 엔티티와 기본 설정 생성, Token 엔티티 저장, FCM 토큰 등록, 임시 데이터 정리를 처리합니다.</p>
     * <p>{@link SignUpService}에서 신규 사용자 회원가입 완료 처리 시 호출됩니다.</p>
     *
     * @param userName 사용자가 입력한 닉네임
     * @param uuid 임시 데이터 식별을 위한 UUID
     * @param userProfile Redis에서 복원된 소셜 사용자 프로필 정보 (OAuth 액세스/리프레시 토큰 포함)
     * @param fcmToken Firebase Cloud Messaging 토큰 (푸시 알림용)
     * @return JWT 토큰이 포함된 인증 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<ResponseCookie> saveNewUser(String userName, String uuid, SocialUserProfile userProfile, String fcmToken);



}