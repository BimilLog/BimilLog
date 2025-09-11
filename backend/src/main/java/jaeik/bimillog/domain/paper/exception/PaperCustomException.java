package jaeik.bimillog.domain.paper.exception;

import lombok.Getter;

/**
 * <h2>롤링페이퍼 커스텀 예외</h2>
 * <p>롤링페이퍼 도메인에서 발생하는 비즈니스 로직 예외를 처리하는 커스텀 예외 클래스입니다.</p>
 * <p>존재하지 않는 사용자명으로 롤링페이퍼 접근 시도</p>
 * <p>존재하지 않는 메시지 ID로 메시지 조회/삭제 시도</p>
 * <p>권한이 없는 메시지 삭제 시도 (타인의 롤링페이퍼 메시지 삭제)</p>
 * <p>잘못된 입력값으로 인한 비즈니스 규칙 위반</p>
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
     * <p>{@link PaperQueryService}, {@link PaperCommandService}에서 비즈니스 로직 검증 실패 시 호출됩니다.</p>
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