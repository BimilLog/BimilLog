package jaeik.bimillog.domain.post.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>게시글 도메인 오류 응답 클래스</h2>
 * <p>게시글 도메인에서 발생한 비즈니스 예외에 대한 HTTP 응답 형식을 정의하는 클래스</p>
 * <p>PostExceptionHandler에서 PostCustomException 처리 시 생성되어 클라이언트에게 반환됩니다.</p>
 * <p>HTTP 상태 코드, 대상 도메인, 에러 메시지를 포함하여 일관된 에러 응답 형식을 제공합니다.</p>
 * <p>프론트엔드에서 게시글 관련 에러를 식별하고 적절한 UI/UX로 처리할 수 있도록 도움니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@AllArgsConstructor
public class PostErrorResponse {
    private int status;
    private String target;
    private String message;
}