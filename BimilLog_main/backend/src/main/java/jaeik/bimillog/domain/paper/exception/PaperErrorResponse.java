package jaeik.bimillog.domain.paper.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>롤링페이퍼 에러 응답</h2>
 * <p>롤링페이퍼 도메인에서 발생한 오류에 대한 HTTP 응답을 정의하는 클래스입니다.</p>
 * <p>status: HTTP 상태 코드 (400, 403, 404 등)</p>
 * <p>target: 오류가 발생한 도메인 구분자 ("PaperError")</p>
 * <p>message: 사용자에게 표시할 구체적인 오류 메시지</p>
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