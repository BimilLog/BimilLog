package jaeik.bimillog.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * <h2>글로벌 예외 처리기</h2>
 * <p>
 * 애플리케이션 전역에서 발생하는 공통 예외를 처리하는 클래스
 * 도메인별 예외는 각 도메인의 ExceptionHandler에서 처리
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * <h3>커스텀 예외 처리</h3>
     * <p>
     * 애플리케이션에서 발생하는 커스텀 예외를 처리하여 적절한 응답을 반환
     * </p>
     *
     * @param e 발생한 커스텀 예외
     * @return 커스텀 예외에 대한 응답 엔티티
     * @since 2.0.0
     * @author Jaeik
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorResponse response = new ErrorResponse(
                e.getErrorCode().getStatus().value(),
                e.getMessage());

        // ErrorCode의 LogLevel에 따라 적절한 로그 레벨로 기록
        if (e.getErrorCode() != null) {
            String logMessage = "CustomException: 코드: {}, 메시지: {}";
            ErrorCode.LogLevel logLevel = e.getErrorCode().getLogLevel();

            switch (logLevel) {
                case INFO -> log.info(logMessage, e.getErrorCode().name(), e.getMessage(), e);
                case WARN -> log.warn(logMessage, e.getErrorCode().name(), e.getMessage(), e);
                case ERROR -> log.error(logMessage, e.getErrorCode().name(), e.getMessage(), e);
                case FATAL ->
                    log.error("FATAL - " + logMessage, e.getErrorCode().name(), e.getMessage(), e);
            }
        } else {
            // ErrorCode가 없는 경우 기본적으로 ERROR 레벨로 로깅
            log.error("CustomException: 메시지: {}", e.getMessage(), e);
        }

        return new ResponseEntity<>(response, e.getErrorCode().getStatus());
    }

    /**
     * <h3>일반 예외 처리</h3>
     * <p>
     * 애플리케이션에서 발생하는 일반 예외를 처리하여 적절한 응답을 반환
     * 보안을 위해 내부 구현 세부사항은 로그에만 기록하고 사용자에게는 일반적인 메시지를 제공
     * </p>
     *
     * @param e 발생한 일반 예외
     * @return 일반 예외에 대한 응답 엔티티
     * @since 2.0.0
     * @author Jaeik
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String userMessage = "서버에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";

        if (e instanceof ResponseStatusException ex) {
            status = (HttpStatus) ex.getStatusCode();
        } else if (e.getClass().isAnnotationPresent(ResponseStatus.class)) {
            status = e.getClass().getAnnotation(ResponseStatus.class).value();
        }

        // 특정 예외 타입에 따른 사용자 친화적 메시지 제공
        if (isDataAccessException(e)) {
            userMessage = "데이터 처리 중 오류가 발생했습니다. 입력하신 정보를 확인해주세요.";
        } else if (isValidationException(e)) {
            userMessage = "입력하신 정보에 문제가 있습니다. 다시 확인해주세요.";
        } else if (isNetworkException(e)) {
            userMessage = "네트워크 연결에 문제가 발생했습니다. 인터넷 연결을 확인해주세요.";
        }

        ErrorResponse response = new ErrorResponse(
                status.value(),
                userMessage);

        // 실제 에러 정보는 로그에만 기록 (보안상 사용자에게 노출하지 않음)
        log.error("Exception occurred: 타입={}, 메시지={}, 스택트레이스 상위 3개={}", 
                e.getClass().getSimpleName(), 
                e.getMessage(),
                getTopStackTrace(e),
                e);
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * <h3>데이터베이스/JPA 관련 예외인지 확인</h3>
     */
    private boolean isDataAccessException(Exception e) {
        String exceptionName = e.getClass().getName();
        return exceptionName.contains("DataAccessException") ||
               exceptionName.contains("SQLException") ||
               exceptionName.contains("JpaException") ||
               exceptionName.contains("PersistenceException") ||
               exceptionName.contains("TransactionException") ||
               exceptionName.contains("ConstraintViolationException") ||
               e.getMessage() != null && (
                   e.getMessage().contains("Field") && e.getMessage().contains("doesn't have a default value") ||
                   e.getMessage().contains("Duplicate entry") ||
                   e.getMessage().contains("cannot be null") ||
                   e.getMessage().contains("foreign key constraint")
               );
    }

    /**
     * <h3>유효성 검증 관련 예외인지 확인</h3>
     */
    private boolean isValidationException(Exception e) {
        String exceptionName = e.getClass().getName();
        return exceptionName.contains("ValidationException") ||
               exceptionName.contains("BindException") ||
               exceptionName.contains("ConstraintViolation");
    }

    /**
     * <h3>네트워크 관련 예외인지 확인</h3>
     */
    private boolean isNetworkException(Exception e) {
        String exceptionName = e.getClass().getName();
        return exceptionName.contains("ConnectException") ||
               exceptionName.contains("SocketException") ||
               exceptionName.contains("TimeoutException") ||
               exceptionName.contains("UnknownHostException");
    }

    /**
     * <h3>스택 트레이스 상위 3개 항목만 추출</h3>
     * <p>로그 가독성을 위해 스택 트레이스를 제한적으로 기록</p>
     */
    private String getTopStackTrace(Exception e) {
        StackTraceElement[] elements = e.getStackTrace();
        if (elements == null || elements.length == 0) {
            return "스택트레이스 없음";
        }
        
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(3, elements.length);
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(" -> ");
            sb.append(elements[i].toString());
        }
        return sb.toString();
    }

    /**
     * <h3>시큐리티 권한 부족 예외 처리</h3>
     * 시큐리티의 권한 에러를 잡는다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                e.getMessage()
        );
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * <h3>Spring Security 인증 실패 예외 처리</h3>
     * 인증되지 않은 사용자가 접근했을 때 401 Unauthorized 반환
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                e.getMessage()
        );
        log.warn("AuthenticationException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * <h3>Bean Validation 예외 처리</h3>
     * <p>@Valid 검증 실패 시 400 Bad Request 반환</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        // 첫 번째 validation 에러 메시지 사용
        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Validation failed");

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorMessage
        );

        log.warn("ValidationException: {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * <h3>IllegalArgumentException 예외 처리</h3>
     * <p>잘못된 인자값 (예: 잘못된 소셜 제공자) 전달 시 400 Bad Request 반환</p>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage()
        );

        // TODO: 비즈니스 로직 문제 해결 - 잘못된 provider 값에 대한 적절한 에러 처리
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}
