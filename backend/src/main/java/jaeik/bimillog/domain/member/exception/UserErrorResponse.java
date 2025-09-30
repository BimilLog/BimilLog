package jaeik.bimillog.domain.member.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>사용자 도메인 오류 응답 클래스</h2>
 * <p>사용자 도메인에서 발생한 오류에 대한 응답을 정의하는 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@AllArgsConstructor
public class UserErrorResponse {
    private int status;
    private String target;
    private String message;
}