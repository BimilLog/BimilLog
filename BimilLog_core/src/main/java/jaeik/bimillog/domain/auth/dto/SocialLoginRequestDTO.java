package jaeik.bimillog.domain.auth.dto;

import jaeik.bimillog.domain.member.entity.SocialProvider;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>소셜 로그인 요청 DTO</h2>
 * <p>소셜 로그인 API 요청을 위한 DTO입니다.</p>
 * <p>provider, code 유효성 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequestDTO {

    @NotBlank(message = "소셜 제공자는 필수입니다.")
    private SocialProvider provider;

    @NotBlank(message = "인증 코드는 필수입니다.")
    private String code;

    /**
     * OAuth state 파라미터 (CSRF 방지용)
     * 일부 제공자(네이버 등)에서 필수, 카카오는 선택
     */
    private String state;
}