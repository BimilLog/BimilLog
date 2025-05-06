package jaeik.growfarm.dto.farm;

import jaeik.growfarm.entity.crop.CropType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 다른 사람의 농장에 갔을 때 전달받는 농작물 DTO
@Getter
@Setter
public class VisitCropDTO {

    private Long id;

    @Size(max = 8, message = "농장 이름은 최대 8글자 까지 입력 가능합니다.")
    private String farmName;

    private CropType cropType;

    private int width;

    private int height;
}
