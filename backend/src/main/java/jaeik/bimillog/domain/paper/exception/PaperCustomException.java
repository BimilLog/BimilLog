package jaeik.bimillog.domain.paper.exception;

import lombok.Getter;

/**
 * <h2>PaperCustomException</h2>
 * <p>
 * 롤링페이퍼 도메인에서 발생하는 비즈니스 로직 예외를 처리하는 커스텀 예외 클래스입니다.
 * 도메인 레이어에서 발생하는 업무적 오류 상황을 명확하게 정의하고 처리하기 위해 사용됩니다.
 * </p>
 * <p>
 * 이 예외 클래스는 다음과 같은 상황에서 발생합니다:
 * - 존재하지 않는 사용자명으로 롤링페이퍼 접근 시도
 * - 존재하지 않는 메시지 ID로 메시지 조회/삭제 시도
 * - 권한이 없는 메시지 삭제 시도 (타인의 롤링페이퍼 메시지 삭제)
 * - 잘못된 입력값으로 인한 비즈니스 규칙 위반
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 예외가 필요한 이유:
 * 1. 명확한 오류 구분 - 시스템 오류와 비즈니스 오류의 분리
 * 2. 사용자 경험 개선 - 구체적이고 이해하기 쉬운 오류 메시지 제공
 * 3. 로깅 전략 지원 - 오류 레벨에 따른 적절한 로그 기록
 * </p>
 * <p>
 * PaperQueryService와 PaperCommandService에서 비즈니스 규칙 위반 시 발생됩니다.
 * PaperExceptionHandler에서 이 예외를 잡아 적절한 HTTP 응답으로 변환합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public class PaperCustomException extends RuntimeException {

    private final PaperErrorCode paperErrorCode;

    /**
     * <h3>PaperCustomException 생성자</h3>
     * <p>특정 롤링페이퍼 에러 코드를 받아 커스텀 예외를 생성합니다.</p>
     * <p>에러 코드에 정의된 메시지와 HTTP 상태 코드, 로그 레벨 정보를 포함합니다.</p>
     * <p>PaperQueryService와 PaperCommandService에서 비즈니스 로직 검증 실패 시 호출됩니다.</p>
     *
     * @param paperErrorCode 발생한 오류에 해당하는 PaperErrorCode
     * @author Jaeik
     * @since 2.0.0
     */
    public PaperCustomException(PaperErrorCode paperErrorCode) {
        super(paperErrorCode.getMessage());
        this.paperErrorCode = paperErrorCode;
    }
}