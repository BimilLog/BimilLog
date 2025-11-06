package jaeik.bimillog.domain.member.in.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>회원가입 요청 DTO</h2>
 * <p>회원가입 API 요청을 위한 DTO입니다.</p>
 * <p>사용자 이름 유효성 검증</p>
 * <p>UUID는 HttpOnly 쿠키를 통해 서버로 전달되므로 요청 본문에 포함하지 않습니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDTO {

    @NotBlank(message = "사용자 이름은 필수입니다.")
    @Size(min = 1, max = 8, message = "사용자 이름은 1자 이상 8자 이하여야 합니다.")
    private String memberName;

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
    public boolean isMemberNameValidAfterTrim() {
        if (memberName == null) return false;
        String trimmed = memberName.trim();
        return !trimmed.isEmpty() && trimmed.length() >= 1 && trimmed.length() <= 8;
    }

    /**
     * <h3>사용자 이름 특수문자 검증</h3>
     * <p>사용자 이름에 금지된 특수문자가 포함되어 있는지 검증합니다.</p>
     * <p>한글, 영문, 숫자만 허용합니다. 공백 및 특수문자는 허용하지 않습니다.</p>
     *
     * @return 허용된 문자로만 구성되어 있으면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "특수문자는 사용할 수 없습니다.")
    public boolean isMemberNameCharacterValid() {
        if (memberName == null) return false;
        return memberName.matches("^[가-힣a-zA-Z0-9]+$");
    }
}