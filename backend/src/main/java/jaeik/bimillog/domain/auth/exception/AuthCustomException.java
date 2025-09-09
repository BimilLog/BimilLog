package jaeik.bimillog.domain.auth.exception;

import lombok.Getter;

/**
 * <h2>인증 도메인 커스텀 예외</h2>
 * <p>
 * Auth 도메인에서 발생하는 비즈니스 규칙 위반 및 인증 관련 예외를 처리하는 전용 예외 클래스입니다.
 * </p>
 * <p>AuthErrorCode와 결합하여 인증 도메인의 예외 상황을 명확하고 일관성 있게 처리합니다.</p>
 * <p>도메인 계층의 순수성을 유지하면서 비즈니스 예외를 표현하는 도메인 모델입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public class AuthCustomException extends RuntimeException {

    private final AuthErrorCode authErrorCode;

    /**
     * <h3>인증 예외 생성</h3>
     * <p>AuthErrorCode를 기반으로 인증 도메인 예외를 생성합니다.</p>
     * <p>비즈니스 규칙 위반 시 도메인 서비스에서 적절한 에러 코드와 함께 호출됩니다.</p>
     * <p>로그인 실패, 권한 부족, 토큰 무효 등의 인증 관련 예외 상황에서 사용됩니다.</p>
     *
     * @param authErrorCode 발생한 인증 예외의 세부 정보를 담은 에러 코드
     * @author Jaeik
     * @since 2.0.0
     */
    public AuthCustomException(AuthErrorCode authErrorCode) {
        super(authErrorCode.getMessage());
        this.authErrorCode = authErrorCode;
    }

    /**
     * <h3>원인 예외 포함 인증 예외 생성</h3>
     * <p>AuthErrorCode와 근본 원인 예외를 포함하여 인증 도메인 예외를 생성합니다.</p>
     * <p>외부 시스템 연동 실패나 예상치 못한 예외를 래핑하여 도메인 예외로 변환할 때 사용됩니다.</p>
     * <p>소셜 로그인 API 호출 실패, 토큰 처리 중 예외 등의 상황에서 근본 원인 추적을 위해 활용됩니다.</p>
     *
     * @param authErrorCode 발생한 인증 예외의 세부 정보를 담은 에러 코드
     * @param cause 예외의 근본 원인이 되는 하위 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public AuthCustomException(AuthErrorCode authErrorCode, Throwable cause) {
        super(authErrorCode.getMessage(), cause);
        this.authErrorCode = authErrorCode;
    }
}