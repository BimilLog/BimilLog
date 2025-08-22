package jaeik.growfarm.infrastructure.adapter.auth.in.web.dto;

import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>로그인 응답 인터페이스</h2>
 * <p>타입 안전성을 보장하는 sealed interface를 사용한 로그인 응답 모델</p>
 * <p>기존 LoginResponseDTO의 unsafe casting 문제를 해결</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public sealed interface LoginResponse 
    permits LoginResponse.NewUser, LoginResponse.ExistingUser {

    /**
     * <h3>신규 사용자 로그인 응답</h3>
     * <p>회원가입이 필요한 신규 사용자를 위한 응답</p>
     *
     * @param uuid       임시 사용자 UUID
     * @param tempCookie 임시 쿠키
     */
    record NewUser(String uuid, ResponseCookie tempCookie) implements LoginResponse {}

    /**
     * <h3>기존 사용자 로그인 응답</h3>
     * <p>이미 등록된 사용자를 위한 응답</p>
     *
     * @param cookies JWT 액세스 토큰과 리프레시 토큰을 포함한 쿠키 리스트
     */
    record ExistingUser(List<ResponseCookie> cookies) implements LoginResponse {}

    /**
     * <h3>로그인 타입 확인</h3>
     * <p>현재 응답이 신규 사용자인지 확인</p>
     *
     * @return 신규 사용자인 경우 true
     */
    default boolean isNewUser() {
        return this instanceof NewUser;
    }

    /**
     * <h3>로그인 타입 확인</h3>
     * <p>현재 응답이 기존 사용자인지 확인</p>
     *
     * @return 기존 사용자인 경우 true
     */
    default boolean isExistingUser() {
        return this instanceof ExistingUser;
    }
}