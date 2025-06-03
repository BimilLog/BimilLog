package jaeik.growfarm.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>오류 응답 클래스</h2>
 * <p>API 요청 처리 중 발생한 오류에 대한 응답을 정의하는 클래스</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String target;
    private String message;
}