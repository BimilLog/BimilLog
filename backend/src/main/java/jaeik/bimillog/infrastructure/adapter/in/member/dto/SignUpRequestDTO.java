package jaeik.bimillog.infrastructure.adapter.in.member.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>회원가입 요청 DTO</h2>
 * <p>회원가입 API 요청을 위한 DTO입니다.</p>
 * <p>사용자 이름 유효성 검증, UUID 형식 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDTO {

    @NotBlank(message = "사용자 이름은 필수입니다.")
    @Size(min = 1, max = 20, message = "사용자 이름은 1자 이상 20자 이하여야 합니다.")
    private String userName;

    @NotBlank(message = "임시 UUID는 필수입니다.")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", 
             message = "올바른 UUID 형식이 아닙니다.")
    private String uuid;

    /**
     * <h3>사용자 이름 유효성 검증</h3>
     * <p>trim() 후에도 유효한 문자열인지 검증합니다.</p>
     * <p>공백으로만 이루어진 문자열을 차단합니다.</p>
     *
     * @return trim() 후 유효한 문자열이면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "사용자 이름에 유효한 문자가 포함되어야 합니다.")
    public boolean isUserNameValidAfterTrim() {
        if (userName == null) return false;
        String trimmed = userName.trim();
        return !trimmed.isEmpty() && trimmed.length() <= 20;
    }

    /**
     * <h3>사용자 이름 특수문자 검증</h3>
     * <p>사용자 이름에 금지된 특수문자가 포함되어 있는지 검증합니다.</p>
     * <p>한글, 영문, 숫자, 공백, 일부 특수문자만 허용합니다.</p>
     *
     * @return 허용된 문자로만 구성되어 있으면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "사용자 이름에 허용되지 않은 문자가 포함되어 있습니다.")
    public boolean isUserNameCharacterValid() {
        if (userName == null) return false;
        return userName.matches("^[가-힣a-zA-Z0-9\\s._-]*$");
    }
}