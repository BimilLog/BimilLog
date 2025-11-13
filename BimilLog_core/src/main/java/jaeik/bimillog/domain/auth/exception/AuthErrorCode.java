package jaeik.bimillog.domain.auth.exception;

import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>인증 도메인 에러 코드</h2>
 * <p>
 * Auth 도메인에서 발생할 수 있는 모든 비즈니스 예외 상황을 정의한 열거형입니다.
 * </p>
 * <p>로그인, 로그아웃, 소셜 인증, 토큰 관리, 회원가입 등 인증 관련 모든 에러를 체계적으로 분류합니다.</p>
 * <p>HTTP 상태 코드와 로그 레벨을 포함하여 일관된 에러 처리와 적절한 로깅 수준을 보장합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public enum AuthErrorCode {

    /**
     * <h3>인증 및 권한 관련 에러 코드</h3>
     */
    NULL_SECURITY_CONTEXT(HttpStatus.UNAUTHORIZED, "유저 인증 정보가 없습니다. 다시 로그인 해주세요", ErrorCode.LogLevel.WARN),
    ALREADY_LOGIN(HttpStatus.FORBIDDEN, "이미 로그인 된 유저 입니다.", ErrorCode.LogLevel.WARN),

    /**
     * <h3>소셜 로그인 관련 에러 코드</h3>
     */
    BLACKLIST_USER(HttpStatus.FORBIDDEN, "차단된 회원은 회원가입이 불가능합니다", ErrorCode.LogLevel.INFO),
    SOCIAL_TOKEN_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 로그인 토큰 발급에 실패했습니다. 다시 시도해주세요.", ErrorCode.LogLevel.ERROR),
    SOCIAL_TOKEN_REFRESH_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인 토큰 갱신에 실패했습니다. 다시 로그인해주세요.", ErrorCode.LogLevel.WARN),
    SOCIAL_TOKEN_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 로그인 연결 해제에 실패했습니다.", ErrorCode.LogLevel.ERROR),

    /**
     * <h3>토큰 관련 에러 코드</h3>
     */
    NOT_FIND_TOKEN(HttpStatus.FORBIDDEN, "토큰을 찾을 수 없습니다. 다시 로그인 해주세요 ", ErrorCode.LogLevel.WARN),
    
    /**
     * <h3>임시 데이터 관련 에러 코드</h3>
     */
    INVALID_TEMP_DATA(HttpStatus.BAD_REQUEST, "시간이 초과 되었습니다. 다시 카카오 로그인을 진행해주세요.", ErrorCode.LogLevel.WARN),
    INVALID_TEMP_UUID(HttpStatus.BAD_REQUEST, "임시 사용자 UUID가 유효하지 않습니다.", ErrorCode.LogLevel.WARN),
    INVALID_USER_DATA(HttpStatus.BAD_REQUEST, "사용자 데이터가 유효하지 않습니다.", ErrorCode.LogLevel.WARN),
    INVALID_TOKEN_DATA(HttpStatus.BAD_REQUEST, "토큰 데이터가 유효하지 않습니다.", ErrorCode.LogLevel.WARN);

    private final HttpStatus status;
    private final String message;
    private final ErrorCode.LogLevel logLevel;

    /**
     * <h3>인증 에러 코드 생성</h3>
     * <p>HTTP 상태 코드, 사용자 메시지, 로깅 레벨을 조합하여 에러 코드를 정의합니다.</p>
     * <p>각 비즈니스 예외 상황에 맞는 적절한 HTTP 응답과 로깅 수준을 설정합니다.</p>
     * <p>시스템 전반의 일관된 에러 응답 체계를 구축하기 위한 기본 구조입니다.</p>
     *
     * @param status 클라이언트에 반환할 HTTP 상태 코드
     * @param message 사용자에게 표시될 에러 메시지
     * @param logLevel 시스템 로깅 시 적용할 로그 레벨
     * @author Jaeik
     * @since 2.0.0
     */
    AuthErrorCode(HttpStatus status, String message, ErrorCode.LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }
}