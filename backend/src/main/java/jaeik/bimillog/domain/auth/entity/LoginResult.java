package jaeik.bimillog.domain.auth.entity;

import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>로그인 결과 값 객체</h2>
 * <p>소셜 로그인 처리 결과를 담는 도메인 계층의 값 객체입니다.</p>
 * <p>신규/기존 사용자 구분을 위한 API 응답 전용 클래스</p>
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
}