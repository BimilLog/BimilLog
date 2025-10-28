package jaeik.bimillog.domain.member.exception;

import lombok.Getter;

/**
 * <h2>회원 도메인 전용 커스텀 예외</h2>
 * <p>회원(Member) 도메인에서 발생하는 비즈니스 예외를 처리하는 커스텀 예외 클래스</p>
 * <p>MemberErrorCode와 함께 사용되어 회원 기능 관련 예외를 명확하게 분리 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public class MemberCustomException extends RuntimeException {

    private final MemberErrorCode memberErrorCode;

    /**
     * <h3>회원 커스텀 예외 생성자</h3>
     * <p>MemberErrorCode를 받아 회원 도메인 전용 예외를 생성합니다.</p>
     *
     * @param memberErrorCode 회원 전용 에러 코드
     * @author Jaeik
     * @since 2.0.0
     */
    public MemberCustomException(MemberErrorCode memberErrorCode) {
        super(memberErrorCode.getMessage());
        this.memberErrorCode = memberErrorCode;
    }

    /**
     * <h3>회원 커스텀 예외 생성자 (원인 포함)</h3>
     * <p>MemberErrorCode와 원인 예외를 받아 회원 도메인 전용 예외를 생성합니다.</p>
     *
     * @param memberErrorCode 회원 전용 에러 코드
     * @param cause 원인 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public MemberCustomException(MemberErrorCode memberErrorCode, Throwable cause) {
        super(memberErrorCode.getMessage(), cause);
        this.memberErrorCode = memberErrorCode;
    }
}