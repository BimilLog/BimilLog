package jaeik.bimillog.domain.paper.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>PaperErrorResponse</h2>
 * <p>
 * 롤링페이퍼 도메인에서 발생한 오류에 대한 HTTP 응답을 정의하는 클래스입니다.
 * 클라이언트에게 일관된 형태의 오류 정보를 제공하여 프론트엔드에서 적절한 오류 처리를 할 수 있도록 지원합니다.
 * </p>
 * <p>
 * 응답 필드 구성:
 * - status: HTTP 상태 코드 (400, 403, 404 등)
 * - target: 오류가 발생한 도메인 구분자 ("PaperError")
 * - message: 사용자에게 표시할 구체적인 오류 메시지
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 응답 클래스가 필요한 이유:
 * 1. 사용자 경험 향상 - 명확하고 이해하기 쉬운 오류 메시지 제공
 * 2. 프론트엔드 개발 지원 - 일관된 오류 응답 형태로 처리 로직 단순화
 * 3. 디버깅 효율성 - 도메인별 오류 구분을 통한 빠른 문제 파악
 * </p>
 * <p>
 * PaperExceptionHandler에서 PaperCustomException 처리 시 생성되어 클라이언트로 반환됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@AllArgsConstructor
public class PaperErrorResponse {
    private int status;
    private String target;
    private String message;
}