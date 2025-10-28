package jaeik.bimillog.domain.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>인증 도메인 에러 응답</h2>
 * <p>
 * Auth 도메인에서 발생한 예외를 클라이언트에게 전달하기 위한 응답 데이터 클래스입니다.
 * </p>
 * <p>인증 관련 에러 발생 시 일관된 형태의 응답 구조를 제공하여 클라이언트의 에러 처리를 표준화합니다.</p>
 * <p>HTTP 상태 코드, 에러 대상, 사용자 메시지를 포함하여 구체적인 에러 정보를 전달합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@AllArgsConstructor
public class AuthErrorResponse {
    private int status;
    private String target;
    private String message;
}