package jaeik.growfarm.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>에러 코드 열거형</h2>
 * <p>애플리케이션에서 발생할 수 있는 다양한 에러 코드를 정의하는 열거형</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Getter
public enum ErrorCode {

    // 유저 관련
    NULL_SECURITY_CONTEXT(HttpStatus.UNAUTHORIZED, "유저 인증 정보가 없습니다. 다시 로그인 해주세요"),
    NOT_MATCH_USER(HttpStatus.FORBIDDEN, "시큐리티 콘텍스트의 유저 정보가 DB에 없습니다"),
    NOT_FIND_TOKEN(HttpStatus.FORBIDDEN, "토큰을 찾을 수 없습니다. 다시 로그인 해주세요 "),
    BLACKLIST_USER(HttpStatus.FORBIDDEN, "차단된 회원은 회원가입이 불가능합니다"),
    KAKAO_GET_USERINFO_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 유저정보 가져오기 실패"),
    LOGOUT_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "로그아웃 실패"),
    WITHDRAW_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "회원탈퇴 실패"),
    NOT_FIND_KAKAO_FRIEND_FARM(HttpStatus.FORBIDDEN, "해당 카카오 친구의 농장을 찾을 수 없습니다."),
    KAKAO_FRIEND_CONSENT_FAIL(HttpStatus.UNAUTHORIZED, "카카오 친구 추가 동의을 해야 합니다."),
    INVALID_USER_ID(HttpStatus.INTERNAL_SERVER_ERROR, "유저 아이디가 잘못되었습니다."),
    ALREADY_LOGIN(HttpStatus.FORBIDDEN, "이미 로그인 된 유저 입니다."),
    INVALID_TEMP_DATA(HttpStatus.BAD_REQUEST, "시간이 초과 되었습니다. 다시 카카오 로그인을 진행해주세요."),
    AUTH_JWT_ACCESS_TOKEN_ERROR(HttpStatus.FORBIDDEN, "JWT 엑세스 토큰 인증 중 오류 발생"),
    RENEWAL_JWT_ACCESS_TOKEN_ERROR(HttpStatus.FORBIDDEN, "JWT 엑세스 토큰 갱신 중 오류 발생"),
    INVALID_JWT_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT 리프레시 토큰입니다. 다시 로그인 해주세요."),

    // 게시판 관련
    NOT_FIND_POST(HttpStatus.FORBIDDEN, "해당 게시글이 존재하지 않습니다.");


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
