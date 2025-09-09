package jaeik.bimillog.domain.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>인증 도메인 예외 처리기</h2>
 * <p>
 * Auth 도메인에서 발생하는 AuthCustomException을 처리하여 적절한 HTTP 응답으로 변환하는 핸들러입니다.
 * </p>
 * <p>도메인 예외를 클라이언트가 이해할 수 있는 표준화된 에러 응답으로 변환합니다.</p>
 * <p>에러 코드별 로그 레벨에 따라 적절한 수준의 로깅을 수행하여 시스템 모니터링을 지원합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    /**
     * <h3>인증 도메인 예외 처리</h3>
     * <p>AuthCustomException을 캐치하여 표준화된 에러 응답으로 변환합니다.</p>
     * <p>에러 코드에 정의된 HTTP 상태 코드와 메시지를 기반으로 응답을 구성합니다.</p>
     * <p>Spring의 전역 예외 처리 메커니즘을 통해 모든 Auth 컨트롤러에서 발생하는 예외를 일괄 처리합니다.</p>
     * <p>에러 코드별 로그 레벨에 따라 INFO, WARN, ERROR, FATAL 로깅을 차등 적용합니다.</p>
     *
     * @param e 발생한 Auth 도메인 커스텀 예외
     * @return AuthErrorResponse를 담은 HTTP 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @ExceptionHandler(AuthCustomException.class)
    public ResponseEntity<AuthErrorResponse> handleAuthCustomException(AuthCustomException e) {
        AuthErrorResponse response = new AuthErrorResponse(
                e.getAuthErrorCode().getStatus().value(),
                "AuthError",
                e.getMessage());

        String logMessage = "AuthCustomException: 코드: {}, 메시지: {}";
        switch (e.getAuthErrorCode().getLogLevel()) {
            case INFO -> log.info(logMessage, e.getAuthErrorCode().name(), e.getMessage());
            case WARN -> log.warn(logMessage, e.getAuthErrorCode().name(), e.getMessage());
            case ERROR -> log.error(logMessage, e.getAuthErrorCode().name(), e.getMessage());
            case FATAL -> log.error("FATAL - " + logMessage, e.getAuthErrorCode().name(), e.getMessage());
        }

        return new ResponseEntity<>(response, e.getAuthErrorCode().getStatus());
    }
}