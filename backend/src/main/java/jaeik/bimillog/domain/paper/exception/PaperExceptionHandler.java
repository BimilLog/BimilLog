package jaeik.bimillog.domain.paper.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>PaperExceptionHandler</h2>
 * <p>
 * 롤링페이퍼 도메인에서 발생하는 커스텀 예외를 처리하는 글로벌 예외 처리기입니다.
 * Spring의 @RestControllerAdvice를 사용하여 애플리케이션 전체에서 발생하는 PaperCustomException을 일관되게 처리합니다.
 * </p>
 * <p>
 * 주요 처리 기능:
 * - PaperCustomException을 HTTP 응답으로 변환
 * - 에러 코드별 적절한 로그 레벨로 로깅 수행
 * - 클라이언트에게 구조화된 오류 응답 제공
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 핸들러가 필요한 이유:
 * 1. 사용자 경험 통일 - 모든 롤링페이퍼 관련 오류에 대해 일관된 응답 형태 제공
 * 2. 운영 효율성 향상 - 오류 유형별 적절한 로그 레벨로 모니터링 지원
 * 3. 개발 생산성 향상 - 각 컨트롤러에서 개별적으로 예외 처리할 필요 제거
 * </p>
 * <p>
 * PaperQueryController와 PaperCommandController에서 발생하는 PaperCustomException을 자동으로 처리합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class PaperExceptionHandler {

    /**
     * <h3>롤링페이퍼 도메인 커스텀 예외 처리</h3>
     * <p>PaperCustomException을 받아 적절한 HTTP 응답으로 변환합니다.</p>
     * <p>예외에 포함된 에러 코드 정보를 기반으로 응답 구조를 생성하고, 로그 레벨에 따라 적절한 로깅을 수행합니다.</p>
     * <p>PaperQueryController와 PaperCommandController에서 비즈니스 로직 실행 중 예외 발생 시 자동으로 호출됩니다.</p>
     *
     * @param e 발생한 PaperCustomException 인스턴스
     * @return ResponseEntity<PaperErrorResponse> 클라이언트에게 반환할 구조화된 오류 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @ExceptionHandler(PaperCustomException.class)
    public ResponseEntity<PaperErrorResponse> handlePaperCustomException(PaperCustomException e) {
        PaperErrorResponse response = new PaperErrorResponse(
                e.getPaperErrorCode().getStatus().value(),
                "PaperError",
                e.getMessage());

        String logMessage = "PaperCustomException: 코드: {}, 메시지: {}";
        switch (e.getPaperErrorCode().getLogLevel()) {
            case INFO -> log.info(logMessage, e.getPaperErrorCode().name(), e.getMessage());
            case WARN -> log.warn(logMessage, e.getPaperErrorCode().name(), e.getMessage());
            case ERROR -> log.error(logMessage, e.getPaperErrorCode().name(), e.getMessage());
            case FATAL -> log.error("FATAL - " + logMessage, e.getPaperErrorCode().name(), e.getMessage());
        }

        return new ResponseEntity<>(response, e.getPaperErrorCode().getStatus());
    }
}