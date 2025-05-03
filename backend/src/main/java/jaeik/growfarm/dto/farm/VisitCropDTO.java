package jaeik.growfarm.dto.farm;

import jaeik.growfarm.entity.crop.CropType;
import lombok.Getter;
import lombok.Setter;

// 다른 사람의 농장에 갔을 때 전달받는 농작물 DTO
@Getter
@Setter
public class VisitCropDTO {

    private Long id;

    private String farmName;

    private CropType cropType;

    private int width;

    private int height;
}
