package jaeik.bimillog.domain.auth.entity;

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
}