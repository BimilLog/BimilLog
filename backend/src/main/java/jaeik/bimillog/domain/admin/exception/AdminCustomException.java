package jaeik.bimillog.domain.admin.exception;

import lombok.Getter;

/**
 * <h2>관리자 도메인 전용 커스텀 예외</h2>
 * <p>관리자(Admin) 도메인에서 발생하는 비즈니스 예외를 처리하는 커스텀 예외 클래스</p>
 * <p>AdminErrorCode와 함께 사용되어 관리자 기능 관련 예외를 명확하게 분리 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public class AdminCustomException extends RuntimeException {

    private final AdminErrorCode adminErrorCode;

    /**
     * <h3>관리자 커스텀 예외 생성자</h3>
     * <p>AdminErrorCode를 받아 관리자 도메인 전용 예외를 생성합니다.</p>
     *
     * @param adminErrorCode 관리자 전용 에러 코드
     * @author Jaeik
     * @since 2.0.0
     */
    public AdminCustomException(AdminErrorCode adminErrorCode) {
        super(adminErrorCode.getMessage());
        this.adminErrorCode = adminErrorCode;
    }
}