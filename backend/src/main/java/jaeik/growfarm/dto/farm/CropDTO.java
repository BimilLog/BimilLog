package jaeik.growfarm.dto.farm;

import jaeik.growfarm.entity.crop.CropType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 농작물 DTO
@Getter
@Setter
public class CropDTO {

    private Long id;

    @Size(max = 8, message = "농장 이름은 최대 8글자 까지 입력 가능합니다.")
    private String farmName;

    private CropType cropType;

    @Size(max = 8, message = "익명 이름은 최대 8글자 까지 입력 가능합니다.")
    private String nickname;

    @Size(max = 255, message = "내용은 최대 255자 까지 입력 가능합니다.")
    private String message;

    private int width;

    private int height;
}
