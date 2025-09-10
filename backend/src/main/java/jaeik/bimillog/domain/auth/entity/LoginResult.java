package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.auth.application.port.out.SocialPort;
import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>로그인 결과 값 객체</h2>
 * <p>소셜 로그인 처리 결과를 담는 도메인 계층의 값 객체입니다.</p>
 * <p>신규/기존 사용자 구분, 소셜 로그인 데이터, 임시 사용자 데이터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public sealed interface LoginResult 
    permits LoginResult.NewUser, LoginResult.ExistingUser {

    /**
     * <h3>신규 사용자 로그인 결과</h3>
     * <p>최초 소셜 로그인으로 회원가입이 필요한 사용자의 결과입니다.</p>
     * <p>임시 UUID를 통해 회원가입 페이지로 연결하기 위한 정보를 담고 있습니다.</p>
     *
     * @param uuid 회원가입에 사용할 임시 사용자 UUID 키
     * @param tempCookie 임시 세션 유지용 쿠키
     */
    record NewUser(String uuid, ResponseCookie tempCookie) implements LoginResult {}

    /**
     * <h3>기존 사용자 로그인 결과</h3>
     * <p>이미 회원가입이 완료된 기존 사용자의 로그인 결과입니다.</p>
     * <p>즉시 인증 완료 처리를 위한 JWT 토큰 쿠키를 포함합니다.</p>
     *
     * @param cookies JWT 액세스 토큰과 리프레시 토큰이 포함된 쿠키 목록
     */
    record ExistingUser(List<ResponseCookie> cookies) implements LoginResult {}

    /**
     * <h3>소셜 플랫폼 로그인 원시 데이터</h3>
     * <p>외부 소셜 플랫폼에서 받아온 로그인 처리 결과를 담는 원시 데이터 구조입니다.</p>
     * <p>도메인 LoginResult로 변환되기 전의 포트 계층에서 사용하는 중간 데이터입니다.</p>
     * <p>{@link SocialPort}에서 소셜 플랫폼 API 호출 결과를 담아 반환할 때 사용됩니다.</p>
     *
     * @param userProfile 소셜 플랫폼에서 받은 사용자 프로필 정보
     * @param token 소셜 로그인으로 발급받은 토큰 정보
     * @param isNewUser 시스템에 처음 등록되는 신규 사용자 여부
     * @author Jaeik
     * @since 2.0.0
     */
    record SocialLoginData(SocialUserProfile userProfile, Token token, boolean isNewUser) {}

    /**
     * <h3>기존 사용자 여부 판별</h3>
     * <p>현재 로그인 결과가 기존 사용자의 것인지 타입을 확인합니다.</p>
     * <p>패턴 매칭을 위한 편의 메서드로 비즈니스 로직에서 분기 처리 시 사용됩니다.</p>
     *
     * @return 기존 사용자 로그인 결과인 경우 true, 신규 사용자인 경우 false
     */
    default boolean isExistingUser() {
        return this instanceof ExistingUser;
    }

    /**
     * <h3>소셜 사용자 프로필</h3>
     * <p>소셜 플랫폼에서 받아온 사용자 프로필 정보를 담는 도메인 값 객체입니다.</p>
     * <p>외부 소셜 플랫폼의 API 응답을 도메인 계층에서 사용할 수 있는 순수한 형태로 변환한 모델입니다.</p>
     * <p>카카오, 구글 등 플랫폼별 차이점을 추상화하여 통일된 인터페이스를 제공합니다.</p>
     *
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @param email 사용자 이메일 주소
     * @param provider 소셜 플랫폼 제공자 (KAKAO, GOOGLE 등)
     * @param nickname 소셜 플랫폼에서의 사용자 닉네임
     * @param profileImageUrl 프로필 이미지 URL (선택사항)
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
     * <h3>임시 사용자 데이터</h3>
     * <p>소셜 로그인 성공 후 회원가입 완료까지 임시 저장되는 사용자 데이터입니다.</p>
     * <p>Redis에 UUID 키로 저장되어 회원가입 페이지에서 사용자 이름 입력 완료 시 활용됩니다.</p>
     * <p>신규 사용자의 회원가입 프로세스에서 소셜 인증 정보를 안전하게 전달하기 위한 임시 컨테이너입니다.</p>
     *
     * @param userProfile 소셜 플랫폼에서 받은 사용자 프로필 정보
     * @param token 소셜 로그인으로 발급받은 토큰 정보
     * @param fcmToken 푸시 알림용 Firebase Cloud Messaging 토큰 (선택사항)
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
         * <h3>검증된 임시 사용자 데이터 생성</h3>
         * <p>필수 파라미터 검증을 수행한 후 임시 사용자 데이터 객체를 생성합니다.</p>
         * <p>null 값 검증을 통해 데이터 무결성을 보장하고 안전한 객체 생성을 지원합니다.</p>
         * <p>{@link SocialService}에서 신규 사용자의 임시 데이터 저장 시 호출됩니다.</p>
         *
         * @param userProfile 소셜 플랫폼 사용자 프로필 (필수)
         * @param token 소셜 로그인 토큰 정보 (필수)  
         * @param fcmToken 푸시 알림 토큰 (선택사항, null 가능)
         * @return 검증 완료된 임시 사용자 데이터 객체
         * @throws IllegalArgumentException userProfile이나 token이 null인 경우
         * @author Jaeik
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
}