package jaeik.bimillog.domain.paper.exception;

import lombok.Getter;

/**
 * <h2>롤링페이퍼 도메인 커스텀 예외</h2>
 * <p>
 * 롤링페이퍼 도메인에서 발생하는 비즈니스 로직 예외를 처리하는 커스텀 예외 클래스
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
     * <p>롤링페이퍼 에러 코드를 받아 예외를 생성합니다.</p>
     *
     * @param paperErrorCode 롤링페이퍼 에러 코드
     * @author Jaeik
     * @since 2.0.0
     */
    public PaperCustomException(PaperErrorCode paperErrorCode) {
        super(paperErrorCode.getMessage());
        this.paperErrorCode = paperErrorCode;
    }
}