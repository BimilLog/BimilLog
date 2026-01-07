package jaeik.bimillog.domain.auth.entity;

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
     * <p>임시 UUID 값으로 회원가입 세션을 유지합니다.</p>
     *
     * @param tempUserId 임시 UUID 값
     */
    record NewUser(String tempUserId) implements LoginResult {}

    /**
     * <h3>기존 사용자 로그인 결과</h3>
     * <p>이미 회원가입이 완료된 기존 사용자의 로그인 결과입니다.</p>
     * <p>즉시 인증 완료 처리를 위한 JWT 토큰 값을 포함합니다.</p>
     *
     * @param tokens JWT 액세스/리프레시 토큰 값
     */
    record ExistingUser(AuthTokens tokens) implements LoginResult {}
}
