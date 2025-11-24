package jaeik.bimillog.infrastructure.exception;

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

    // ===== 기존 글로벌 에러 코드 =====
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰을 찾을 수 없습니다", LogLevel.WARN),
    TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "토큰 불일치 - 보안 위협 감지", LogLevel.ERROR),

    // ===== Admin 도메인 에러 코드 =====
    ADMIN_INVALID_REPORT_TARGET(HttpStatus.BAD_REQUEST, "신고 대상이 유효하지 않습니다.", LogLevel.WARN),
    ADMIN_POST_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "신고 대상 게시글이 이미 삭제되었습니다.", LogLevel.WARN),
    ADMIN_COMMENT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "신고 대상 댓글이 이미 삭제되었습니다.", LogLevel.WARN),
    ADMIN_ANONYMOUS_USER_CANNOT_BE_BANNED(HttpStatus.BAD_REQUEST, "익명 사용자는 제재할 수 없습니다.", LogLevel.WARN),

    // ===== Auth 도메인 에러 코드 =====
    AUTH_NULL_SECURITY_CONTEXT(HttpStatus.UNAUTHORIZED, "유저 인증 정보가 없습니다. 다시 로그인 해주세요", LogLevel.WARN),
    AUTH_ALREADY_LOGIN(HttpStatus.FORBIDDEN, "이미 로그인 된 유저 입니다.", LogLevel.WARN),
    AUTH_BLACKLIST_USER(HttpStatus.FORBIDDEN, "차단된 회원은 회원가입이 불가능합니다", LogLevel.INFO),
    AUTH_SOCIAL_TOKEN_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 로그인 토큰 발급에 실패했습니다. 다시 시도해주세요.", LogLevel.ERROR),
    AUTH_SOCIAL_TOKEN_REFRESH_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인 토큰 갱신에 실패했습니다. 다시 로그인해주세요.", LogLevel.WARN),
    AUTH_SOCIAL_TOKEN_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 로그인 연결 해제에 실패했습니다.", LogLevel.ERROR),
    AUTH_NOT_FIND_TOKEN(HttpStatus.FORBIDDEN, "토큰을 찾을 수 없습니다. 다시 로그인 해주세요 ", LogLevel.WARN),
    AUTH_INVALID_TEMP_DATA(HttpStatus.BAD_REQUEST, "시간이 초과 되었습니다. 다시 카카오 로그인을 진행해주세요.", LogLevel.WARN),
    AUTH_INVALID_TEMP_UUID(HttpStatus.BAD_REQUEST, "임시 사용자 UUID가 유효하지 않습니다.", LogLevel.WARN),
    AUTH_INVALID_USER_DATA(HttpStatus.BAD_REQUEST, "사용자 데이터가 유효하지 않습니다.", LogLevel.WARN),
    AUTH_INVALID_TOKEN_DATA(HttpStatus.BAD_REQUEST, "토큰 데이터가 유효하지 않습니다.", LogLevel.WARN),
    SOCIAL_TOKEN_NOT_FOUNT(HttpStatus.FORBIDDEN, "소셜 토큰을 찾을 수 없습니다", LogLevel.ERROR),

    // ===== Comment 도메인 에러 코드 =====
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 댓글을 찾을 수 없습니다.", LogLevel.INFO),
    COMMENT_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 댓글이 존재하지 않습니다.", LogLevel.INFO),
    COMMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 조회에 실패했습니다.", LogLevel.ERROR),
    COMMENT_WRITE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 작성에 실패했습니다.", LogLevel.ERROR),
    COMMENT_UNAUTHORIZED(HttpStatus.FORBIDDEN, "댓글 수정/삭제 권한이 없습니다.", LogLevel.INFO),

    // ===== Member 도메인 에러 코드 =====
    MEMBER_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", LogLevel.INFO),
    MEMBER_SETTINGS_NOT_FOUND(HttpStatus.NOT_FOUND, "설정 정보를 찾을 수 없습니다.", LogLevel.INFO),
    MEMBER_INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다.", LogLevel.WARN),
    MEMBER_EXISTED_NICKNAME(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다.", LogLevel.INFO),
    MEMBER_KAKAO_FRIEND_CONSENT_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 친구 추가 동의를 해야 합니다.", LogLevel.INFO),
    MEMBER_KAKAO_FRIEND_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 친구 API 호출 실패", LogLevel.ERROR),
    MEMBER_UNSUPPORTED_SOCIAL_FRIEND(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 친구 조회 기능입니다.", LogLevel.WARN),
    MEMBER_BLACKLIST_NOT_FOUND(HttpStatus.BAD_REQUEST, "블랙리스트에 해당 유저가 존재하지 않습니다.", LogLevel.WARN),
    MEMBER_BLACKLIST_FORBIDDEN(HttpStatus.FORBIDDEN, "자신의 블랙리스트만 삭제할 수 있습니다.", LogLevel.WARN),

    // ===== Notification 도메인 에러 코드 =====
    NOTIFICATION_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알림 전송 중 오류가 발생했습니다.", LogLevel.ERROR),
    NOTIFICATION_INVALID_USER_CONTEXT(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자 컨텍스트입니다.", LogLevel.WARN),
    NOTIFICATION_NO_SEND_FCM_TOKEN(HttpStatus.BAD_REQUEST, "fcm토큰이 없습니다.", LogLevel.WARN),
    NOTIFICATION_NO_MEMBER_FCM_TOKEN(HttpStatus.INTERNAL_SERVER_ERROR, "사용자가 존재하지 않습니다.", LogLevel.WARN),
    NOTIFICATION_FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FCM 토큰을 등록할 AuthToken을 찾을 수 없습니다.", LogLevel.WARN),
    NOTIFICATION_INVALID_AUTH_TOKEN(HttpStatus.FORBIDDEN, "본인의 AuthToken에만 FCM 토큰을 등록할 수 있습니다.", LogLevel.WARN),

    // ===== Paper 도메인 에러 코드 =====
    PAPER_USERNAME_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 닉네임의 롤링페이퍼를 찾을 수 없습니다.", LogLevel.INFO),
    PAPER_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다.", LogLevel.INFO),
    PAPER_MESSAGE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 롤링페이퍼의 메시지만 삭제할 수 있습니다.", LogLevel.INFO),
    PAPER_INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다.", LogLevel.WARN),
    PAPER_REDIS_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 읽기 중 오류가 발생했습니다.", LogLevel.ERROR),
    PAPER_REDIS_WRITE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 쓰기 중 오류가 발생했습니다.", LogLevel.ERROR),
    PAPER_REDIS_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 삭제 중 오류가 발생했습니다.", LogLevel.ERROR),
    BLACKLIST_MEMBER_PAPER_FORBIDDEN(HttpStatus.FORBIDDEN, "차단되거나 차단한 회원과 상호작용할 수 없습니다.", LogLevel.WARN),

    // ===== Post 도메인 에러 코드 =====
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 게시글이 존재하지 않습니다.", LogLevel.INFO),
    POST_BLANK_PASSWORD(HttpStatus.BAD_REQUEST, "비회원은 글을 수정/삭제 할 시 비밀번호를 입력해야 합니다.", LogLevel.WARN),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다.", LogLevel.WARN),
    POST_REDIS_WRITE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 작성 중 오류가 발생했습니다.", LogLevel.ERROR),
    POST_REDIS_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 읽기 중 오류가 발생했습니다.", LogLevel.ERROR),
    POST_REDIS_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 삭제 중 오류가 발생했습니다.", LogLevel.ERROR),

    // ===== Friend 도메인 에러 코드 =====
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 친구 요청입니다.", LogLevel.ERROR),
    FRIEND_REQUEST_CANCEL_FORBIDDEN(HttpStatus.FORBIDDEN, "자신의 친구 요청만 취소할 수 있습니다", LogLevel.WARN),
    FRIEND_REQUEST_REJECT_FORBIDDEN(HttpStatus.FORBIDDEN, "자신의 친구 요청만 거절할 수 있습니다", LogLevel.WARN),
    SELF_FRIEND_REQUEST_FORBIDDEN(HttpStatus.FORBIDDEN, "자신에게 친구 요청을 보낼 수 없습니다", LogLevel.WARN),
    FRIEND_REQUEST_ALREADY_SEND(HttpStatus.FORBIDDEN, "이미 친구 요청을 보냈습니다.", LogLevel.WARN),
    FRIEND_REQUEST_ALREADY_RECEIVE(HttpStatus.FORBIDDEN, "이미 상대가 친구 요청을 보냈습니다.", LogLevel.WARN),
    FRIEND_SHIP_ALREADY_EXIST(HttpStatus.FORBIDDEN, "이미 친구입니다.", LogLevel.WARN),
    FRIEND_SHIP_NOT_FOUND(HttpStatus.FORBIDDEN, "친구를 조회할 수 없습니다.", LogLevel.WARN),
    FRIEND_SHIP_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "자신의 친구만 삭제할 수 있습니다", LogLevel.WARN),
    FRIEND_REDIS_INTERACTION_ERROR(HttpStatus.FORBIDDEN, "레디스 상호작용 점수 적용이 실패했습니다.", LogLevel.WARN);


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
