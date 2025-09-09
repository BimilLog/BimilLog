package jaeik.bimillog.domain.notification.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>알림 도메인 오류 응답 클래스</h2>
 * <p>알림 도메인에서 발생한 오류에 대한 응답을 정의하는 클래스</p>
 * <p>NotificationExceptionHandler에서 NotificationCustomException을 처리할 때 클라이언트에게 반환할 표준화된 에러 응답 형식을 정의하는 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@AllArgsConstructor
public class NotificationErrorResponse {
    private int status;
    private String target;
    private String message;
}