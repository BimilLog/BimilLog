package jaeik.bimillog.domain.member.exception;

import lombok.Getter;

/**
 * <h2>사용자 도메인 전용 커스텀 예외</h2>
 * <p>사용자(Member) 도메인에서 발생하는 비즈니스 예외를 처리하는 커스텀 예외 클래스</p>
 * <p>UserErrorCode와 함께 사용되어 사용자 기능 관련 예외를 명확하게 분리 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public class UserCustomException extends RuntimeException {

    private final UserErrorCode userErrorCode;

    /**
     * <h3>사용자 커스텀 예외 생성자</h3>
     * <p>UserErrorCode를 받아 사용자 도메인 전용 예외를 생성합니다.</p>
     *
     * @param userErrorCode 사용자 전용 에러 코드
     * @author Jaeik
     * @since 2.0.0
     */
    public UserCustomException(UserErrorCode userErrorCode) {
        super(userErrorCode.getMessage());
        this.userErrorCode = userErrorCode;
    }

    /**
     * <h3>사용자 커스텀 예외 생성자 (원인 포함)</h3>
     * <p>UserErrorCode와 원인 예외를 받아 사용자 도메인 전용 예외를 생성합니다.</p>
     *
     * @param userErrorCode 사용자 전용 에러 코드
     * @param cause 원인 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public UserCustomException(UserErrorCode userErrorCode, Throwable cause) {
        super(userErrorCode.getMessage(), cause);
        this.userErrorCode = userErrorCode;
    }
}