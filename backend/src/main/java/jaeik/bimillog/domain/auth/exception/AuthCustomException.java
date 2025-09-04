package jaeik.bimillog.domain.auth.exception;

import lombok.Getter;

/**
 * <h2>인증 도메인 전용 커스텀 예외</h2>
 * <p>인증(Auth) 도메인에서 발생하는 비즈니스 예외를 처리하는 커스텀 예외 클래스</p>
 * <p>AuthErrorCode와 함께 사용되어 인증 기능 관련 예외를 명확하게 분리 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public class AuthCustomException extends RuntimeException {

    private final AuthErrorCode authErrorCode;

    /**
     * <h3>인증 커스텀 예외 생성자</h3>
     * <p>AuthErrorCode를 받아 인증 도메인 전용 예외를 생성합니다.</p>
     *
     * @param authErrorCode 인증 전용 에러 코드
     * @author Jaeik
     * @since 2.0.0
     */
    public AuthCustomException(AuthErrorCode authErrorCode) {
        super(authErrorCode.getMessage());
        this.authErrorCode = authErrorCode;
    }

    /**
     * <h3>인증 커스텀 예외 생성자 (메시지 포함)</h3>
     * <p>AuthErrorCode와 추가 메시지를 받아 인증 도메인 전용 예외를 생성합니다.</p>
     *
     * @param authErrorCode 인증 전용 에러 코드
     * @param message 추가 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    public AuthCustomException(AuthErrorCode authErrorCode, String message) {
        super(message);
        this.authErrorCode = authErrorCode;
    }

    /**
     * <h3>인증 커스텀 예외 생성자 (원인 포함)</h3>
     * <p>AuthErrorCode와 원인 예외를 받아 인증 도메인 전용 예외를 생성합니다.</p>
     *
     * @param authErrorCode 인증 전용 에러 코드
     * @param cause 원인 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public AuthCustomException(AuthErrorCode authErrorCode, Throwable cause) {
        super(authErrorCode.getMessage(), cause);
        this.authErrorCode = authErrorCode;
    }

    /**
     * <h3>인증 커스텀 예외 생성자 (전체)</h3>
     * <p>AuthErrorCode, 추가 메시지, 원인 예외를 모두 받아 인증 도메인 전용 예외를 생성합니다.</p>
     *
     * @param authErrorCode 인증 전용 에러 코드
     * @param message 추가 메시지
     * @param cause 원인 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public AuthCustomException(AuthErrorCode authErrorCode, String message, Throwable cause) {
        super(message, cause);
        this.authErrorCode = authErrorCode;
    }
}