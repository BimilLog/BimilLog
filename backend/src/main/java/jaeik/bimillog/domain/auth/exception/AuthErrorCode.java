package jaeik.bimillog.domain.auth.exception;

import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>인증 도메인 전용 에러 코드</h2>
 * <p>인증(Auth) 도메인에서 발생할 수 있는 전용 에러 코드를 정의하는 열거형</p>
 * <p>로그인, 로그아웃, 소셜 인증, 토큰 관리 등 인증과 관련된 에러 처리</p>
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
    NOT_FIND_TOKEN(HttpStatus.FORBIDDEN, "토큰을 찾을 수 없습니다. 다시 로그인 해주세요 ", ErrorCode.LogLevel.WARN),
    REPEAT_LOGIN(HttpStatus.FORBIDDEN, "다른기기에서 로그아웃 하셨습니다 다시 로그인 해주세요", ErrorCode.LogLevel.INFO),
    ALREADY_LOGIN(HttpStatus.FORBIDDEN, "이미 로그인 된 유저 입니다.", ErrorCode.LogLevel.WARN),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", ErrorCode.LogLevel.WARN),
    
    /**
     * <h3>소셜 로그인 관련 에러 코드</h3>
     */
    BLACKLIST_USER(HttpStatus.FORBIDDEN, "차단된 회원은 회원가입이 불가능합니다", ErrorCode.LogLevel.INFO),
    KAKAO_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 API 호출 실패", ErrorCode.LogLevel.ERROR),
    KAKAO_FRIEND_CONSENT_FAIL(HttpStatus.UNAUTHORIZED, "카카오 친구 추가 동의을 해야 합니다.", ErrorCode.LogLevel.INFO),
    SOCIAL_UNLINK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 로그인 연결 해제에 실패했습니다.", ErrorCode.LogLevel.ERROR),
    
    /**
     * <h3>로그아웃 관련 에러 코드</h3>
     */
    LOGOUT_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "로그아웃 실패", ErrorCode.LogLevel.ERROR),
    
    /**
     * <h3>임시 데이터 관련 에러 코드</h3>
     */
    INVALID_TEMP_DATA(HttpStatus.BAD_REQUEST, "시간이 초과 되었습니다. 다시 카카오 로그인을 진행해주세요.", ErrorCode.LogLevel.WARN),
    INVALID_TEMP_UUID(HttpStatus.BAD_REQUEST, "임시 사용자 UUID가 유효하지 않습니다.", ErrorCode.LogLevel.WARN),
    INVALID_USER_DATA(HttpStatus.BAD_REQUEST, "사용자 데이터가 유효하지 않습니다.", ErrorCode.LogLevel.WARN),
    INVALID_TOKEN_DATA(HttpStatus.BAD_REQUEST, "토큰 데이터가 유효하지 않습니다.", ErrorCode.LogLevel.WARN),
    
    /**
     * <h3>입력값 관련 에러 코드</h3>
     */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다.", ErrorCode.LogLevel.WARN);

    private final HttpStatus status;
    private final String message;
    private final ErrorCode.LogLevel logLevel;

    /**
     * <h3>AuthErrorCode 생성자</h3>
     * <p>HTTP 상태, 메시지, 로그 레벨을 받아 인증 전용 에러 코드를 생성합니다.</p>
     *
     * @param status HTTP 상태 코드
     * @param message 에러 메시지
     * @param logLevel 에러 로그 레벨
     * @author Jaeik
     * @since 2.0.0
     */
    AuthErrorCode(HttpStatus status, String message, ErrorCode.LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }
}