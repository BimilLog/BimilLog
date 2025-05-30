package jaeik.growfarm.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * <h3>농장 이름 등록 요청 DTO</h3>
 * <p>
 * 회원 가입 시 농장 이름 등록할 때 전달받는 데이터 전송 객체
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@Getter
@Setter
public class FarmNameReqDTO {

    private Long tokenId;

    @Size(max = 8, message = "농장 이름은 최대 8글자 까지 입력 가능합니다.")
    private String farmName;
}