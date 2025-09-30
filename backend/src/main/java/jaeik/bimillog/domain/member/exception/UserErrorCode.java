package jaeik.bimillog.domain.member.exception;

import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>사용자 도메인 전용 에러 코드</h2>
 * <p>사용자(Member) 도메인에서 발생할 수 있는 전용 에러 코드를 정의하는 열거형</p>
 * <p>사용자 정보 조회, 수정, 삭제 등 사용자와 관련된 에러 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public enum UserErrorCode {

    /**
     * <h3>사용자 조회 관련 에러 코드</h3>
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", ErrorCode.LogLevel.INFO),
    SETTINGS_NOT_FOUND(HttpStatus.NOT_FOUND, "설정 정보를 찾을 수 없습니다.", ErrorCode.LogLevel.INFO),
    
    /**
     * <h3>사용자 입력값 검증 관련 에러 코드</h3>
     */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다.", ErrorCode.LogLevel.WARN),
    EXISTED_NICKNAME(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다.", ErrorCode.LogLevel.INFO),
    
    /**
     * <h3>카카오 친구 API 관련 에러 코드</h3>
     */
    KAKAO_FRIEND_CONSENT_FAIL(HttpStatus.UNAUTHORIZED, "카카오 친구 추가 동의를 해야 합니다.", ErrorCode.LogLevel.INFO),
    KAKAO_FRIEND_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 친구 API 호출 실패", ErrorCode.LogLevel.ERROR);

    private final HttpStatus status;
    private final String message;
    private final ErrorCode.LogLevel logLevel;

    /**
     * <h3>UserErrorCode 생성자</h3>
     * <p>HTTP 상태, 메시지, 로그 레벨을 받아 사용자 전용 에러 코드를 생성합니다.</p>
     *
     * @param status HTTP 상태 코드
     * @param message 에러 메시지
     * @param logLevel 에러 로그 레벨
     * @author Jaeik
     * @since 2.0.0
     */
    UserErrorCode(HttpStatus status, String message, ErrorCode.LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }
}