package jaeik.growfarm.infrastructure.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>에러 코드 열거형</h2>
 * <p>
 * 애플리케이션에서 발생할 수 있는 다양한 에러 코드를 정의하는 열거형
 * </p>
 *
 * @author Jaeik
 * @version  1.0.20
 */
@Getter
public enum ErrorCode {

    /**
     * <h3>인증 관련 에러 코드</h3>
     * <p>
     * 인증 및 권한 관련 에러 코드
     * </p>
     */
    NULL_SECURITY_CONTEXT(HttpStatus.UNAUTHORIZED, "유저 인증 정보가 없습니다. 다시 로그인 해주세요", LogLevel.WARN),
    NOT_FIND_TOKEN(HttpStatus.FORBIDDEN, "토큰을 찾을 수 없습니다. 다시 로그인 해주세요 ", LogLevel.WARN),
    BLACKLIST_USER(HttpStatus.FORBIDDEN, "차단된 회원은 회원가입이 불가능합니다", LogLevel.INFO),
    KAKAO_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 API 호출 실패", LogLevel.ERROR),
    LOGOUT_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "로그아웃 실패", LogLevel.ERROR),
    KAKAO_FRIEND_CONSENT_FAIL(HttpStatus.UNAUTHORIZED, "카카오 친구 추가 동의을 해야 합니다.", LogLevel.INFO),
    ALREADY_LOGIN(HttpStatus.FORBIDDEN, "이미 로그인 된 유저 입니다.", LogLevel.WARN),
    INVALID_TEMP_DATA(HttpStatus.BAD_REQUEST, "시간이 초과 되었습니다. 다시 카카오 로그인을 진행해주세요.", LogLevel.WARN),
    REPEAT_LOGIN(HttpStatus.FORBIDDEN, "다른기기에서 로그아웃 하셨습니다 다시 로그인 해주세요", LogLevel.INFO),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", LogLevel.WARN),
    SOCIAL_UNLINK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 로그인 연결 해제에 실패했습니다.", LogLevel.ERROR),
    
    /**
     * <h3>임시 데이터 관련 에러 코드</h3>
     * <p>
     * 소셜 로그인 임시 데이터 저장, 조회, 삭제 등과 관련된 에러 코드
     * </p>
     * @since 2.0.0
     */
    INVALID_TEMP_UUID(HttpStatus.BAD_REQUEST, "임시 사용자 UUID가 유효하지 않습니다.", LogLevel.WARN),
    INVALID_USER_DATA(HttpStatus.BAD_REQUEST, "사용자 데이터가 유효하지 않습니다.", LogLevel.WARN),  
    INVALID_TOKEN_DATA(HttpStatus.BAD_REQUEST, "토큰 데이터가 유효하지 않습니다.", LogLevel.WARN),


    /**
     * <h3>게시판 관련 에러 코드</h3>
     * <p>
     * 게시판 작성, 수정, 삭제 등과 관련된 에러 코드
     * </p>
     */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 게시글이 존재하지 않습니다.", LogLevel.INFO),
    COMMENT_WRITE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 작성에 실패했습니다.", LogLevel.ERROR),
    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 댓글이 존재하지 않습니다.", LogLevel.INFO),
    COMMENT_PASSWORD_NOT_MATCH(HttpStatus.FORBIDDEN, "댓글 비밀번호가 일치하지 않습니다.", LogLevel.WARN),
    ONLY_COMMENT_OWNER_UPDATE(HttpStatus.FORBIDDEN, "댓글 작성자만 수정할 수 있습니다.", LogLevel.INFO),
    COMMENT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 삭제에 실패했습니다.", LogLevel.ERROR),
    COMMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 조회에 실패했습니다.", LogLevel.ERROR),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 댓글을 찾을 수 없습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다.", LogLevel.WARN),
    REDIS_WRITE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 작성 중 오류가 발생했습니다.", LogLevel.ERROR),
    REDIS_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 읽기 중 오류가 발생했습니다.", LogLevel.ERROR),
    REDIS_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 삭제 중 오류가 발생했습니다.", LogLevel.ERROR),

    /**
     * <h3>유저 관련 에러 코드</h3>
     * <p>
     * 유저 정보 조회, 수정, 삭제 등과 관련된 에러 코드
     * </p>
     */
    EXISTED_NICKNAME(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다.", LogLevel.INFO),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", LogLevel.INFO),
    SETTINGS_NOT_FOUND(HttpStatus.NOT_FOUND, "설정 정보를 찾을 수 없습니다.", LogLevel.INFO),

    /**
     * <h3>롤링페이퍼 관련 에러 코드</h3>
     * <p>
     * 롤링페이퍼 작성, 조회, 수정, 삭제 등과 관련된 에러 코드
     * </p>
     */
    USERNAME_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 닉네임의 롤링페이퍼를 찾을 수 없습니다.", LogLevel.INFO),
    MESSAGE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 롤링페이퍼의 메시지만 삭제할 수 있습니다.", LogLevel.INFO),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),

    /**
     * <h3>관리자 에러 코드</h3>
     * <p>
     * 관리자 관련 에러 코드
     * </p>
     */
    INVALID_REPORT_TARGET(HttpStatus.BAD_REQUEST, "신고 대상이 유효하지 않습니다.", LogLevel.WARN),

    /**
     * <h3>알림 에러 코드</h3>
     * <p>
     * 알림 관련 에러 코드
     * </p>
     */
    FCM_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "FCM 알림 전송 중 오류 발생", LogLevel.ERROR),
    NOTIFICATION_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알림 전송 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
    private final LogLevel logLevel;


    /**
     * <h3>ErrorCode 생성자</h3>
     * <p>HTTP 상태, 메시지, 로그 레벨을 받아 에러 코드를 생성합니다.</p>
     *
     * @param status HTTP 상태 코드
     * @param message 에러 메시지
     * @param logLevel 에러 로그 레벨
     * @author Jaeik
     * @since 2.0.0
     */
    ErrorCode(HttpStatus status, String message, LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }

    /**
     * <h3>ErrorCode 생성자 (로그 레벨 기본값)</h3>
     * <p>HTTP 상태와 메시지를 받아 에러 코드를 생성합니다. 로그 레벨은 기본적으로 ERROR로 설정됩니다.</p>
     *
     * @param status HTTP 상태 코드
     * @param message 에러 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
        this.logLevel = LogLevel.ERROR; // Default to ERROR for new constructors
    }

    /**
     * <h3>로그 레벨 열거형</h3>
     * <p>
     * 에러의 심각도를 나타내는 로그 레벨
     * </p>
     */
    @Getter
    public enum LogLevel {
        /**
         * 정보성 메시지 - 정상적인 동작이지만 기록할 필요가 있는 경우
         */
        INFO,

        /**
         * 경고 메시지 - 잠재적인 문제나 주의가 필요한 상황
         */
        WARN,

        /**
         * 에러 메시지 - 오류 발생으로 기능 실행에 문제가 있는 경우
         */
        ERROR,

        /**
         * 치명적 에러 - 시스템 전체에 영향을 주는 심각한 오류
         */
        FATAL
    }
}
