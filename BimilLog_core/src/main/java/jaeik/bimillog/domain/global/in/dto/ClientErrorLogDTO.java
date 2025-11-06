package jaeik.bimillog.domain.global.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>클라이언트 에러 로그 DTO</h2>
 * <p>프론트엔드/안드로이드에서 발생한 에러를 백엔드로 전송하기 위한 DTO입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClientErrorLogDTO {

    /**
     * 에러가 발생한 플랫폼 (web, android, ios)
     */
    @NotBlank(message = "플랫폼은 필수입니다")
    @Size(max = 20, message = "플랫폼은 20자를 초과할 수 없습니다")
    private String platform;

    /**
     * 에러 메시지
     */
    @NotBlank(message = "에러 메시지는 필수입니다")
    @Size(max = 1000, message = "에러 메시지는 1000자를 초과할 수 없습니다")
    private String errorMessage;

    /**
     * 에러 스택 트레이스
     */
    @Size(max = 5000, message = "스택 트레이스는 5000자를 초과할 수 없습니다")
    private String stackTrace;

    /**
     * 에러가 발생한 URL 또는 화면 경로
     */
    @Size(max = 500, message = "URL은 500자를 초과할 수 없습니다")
    private String url;

    /**
     * 사용자 에이전트 (브라우저 정보)
     */
    @Size(max = 500, message = "사용자 에이전트는 500자를 초과할 수 없습니다")
    private String userAgent;

    /**
     * 추가 정보 (JSON 형식)
     */
    @Size(max = 2000, message = "추가 정보는 2000자를 초과할 수 없습니다")
    private String additionalInfo;
}
