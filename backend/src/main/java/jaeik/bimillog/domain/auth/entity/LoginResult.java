package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h3>로그인 결과 값 객체</h3>
 * <p>
 * 소셜 로그인 처리 결과를 담는 도메인 순수 값 객체
 * infrastructure의 LoginResponse DTO와 독립적인 도메인 모델
 * </p>
 * <p>
 * sealed interface를 사용하여 타입 안전성 보장
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
public sealed interface LoginResult 
    permits LoginResult.NewUser, LoginResult.ExistingUser {

    /**
     * <h3>신규 사용자 로그인 결과</h3>
     * <p>회원가입이 필요한 신규 사용자를 위한 결과</p>
     *
     * @param uuid       임시 사용자 UUID
     * @param tempCookie 임시 쿠키
     */
    record NewUser(String uuid, ResponseCookie tempCookie) implements LoginResult {}

    /**
     * <h3>기존 사용자 로그인 결과</h3>
     * <p>이미 등록된 사용자를 위한 결과</p>
     *
     * @param cookies JWT 액세스 토큰과 리프레시 토큰을 포함한 쿠키 리스트
     */
    record ExistingUser(List<ResponseCookie> cookies) implements LoginResult {}

    /**
     * <h3>로그인 타입 확인</h3>
     * <p>현재 결과가 기존 사용자인지 확인</p>
     *
     * @return 기존 사용자인 경우 true
     */
    default boolean isExistingUser() {
        return this instanceof ExistingUser;
    }

    /**
     * <h3>소셜 사용자 프로필</h3>
     * <p>
     * 소셜 로그인으로부터 받은 사용자 프로필 정보를 담는 순수 도메인 모델
     * 헥사고날 아키텍처에서 도메인 계층의 순수 값 객체
     * </p>
     *
     * @param socialId 소셜 ID
     * @param email 이메일 주소
     * @param provider 소셜 제공자
     * @param nickname 사용자 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @author Jaeik
     * @since 2.0.0
     */
    record SocialUserProfile(
            String socialId, 
            String email, 
            SocialProvider provider,
            String nickname, 
            String profileImageUrl
    ) {}

    /**
     * <h2>임시 사용자 데이터</h2>
     * <p>회원가입 프로세스에서 사용하는 순수 도메인 모델</p>
     * <p>소셜 로그인 후 회원가입 완료까지의 임시 데이터를 담고 있습니다.</p>
     *
     * @param userProfile 소셜 사용자 프로필 정보
     * @param token 소셜 로그인 토큰 정보
     * @param fcmToken FCM 푸시 알림 토큰 (선택적)
     * 
     * @author Jaeik
     * @version 2.0.0
     */
    record TempUserData(
            SocialUserProfile userProfile,
            Token token,
            String fcmToken
    ) {
        /**
         * <h3>임시 사용자 데이터 생성</h3>
         * <p>FCM 토큰 검증 후 임시 데이터 객체를 생성합니다.</p>
         *
         * @param userProfile 소셜 사용자 프로필 (필수)
         * @param token 토큰 정보 (필수)  
         * @param fcmToken FCM 토큰 (선택적)
         * @return 검증된 임시 사용자 데이터
         * @throws IllegalArgumentException userProfile이나 token이 null인 경우
         * @since 2.0.0
         */
        public static TempUserData of(SocialUserProfile userProfile, Token token, String fcmToken) {
            if (userProfile == null) {
                throw new IllegalArgumentException("유저 프로파일은 null이 될 수 없습니다");
            }
            if (token == null) {
                throw new IllegalArgumentException("token은 null이 될 수 없습니다");
            }
            return new TempUserData(userProfile, token, fcmToken);
        }
    }

    /**
     * <h3>소셜 로그인 포트 결과</h3>
     * <p>
     * SocialLoginPort에서 사용하는 로그인 처리 결과를 담는 레코드 클래스
     * 도메인 LoginResult와 구분되는 포트 계층용 원시 데이터 구조
     * </p>
     *
     * @param userProfile 소셜 사용자 프로필
     * @param token 토큰 정보
     * @param isNewUser 신규 사용자 여부
     * @author Jaeik
     * @since 2.0.0
     */
    record SocialLoginData(SocialUserProfile userProfile, Token token, boolean isNewUser) {}
}