package jaeik.growfarm.dto.farm;

import jaeik.growfarm.entity.crop.CropType;
import lombok.Getter;
import lombok.Setter;

// 농작물 DTO
@Getter
@Setter
public class CropDTO {

    private Long id;

    private String farmName;

    private CropType cropType;

    private String nickname;

    private String message;

    private int width;

    private int height;
}
