package jaeik.growfarm.util;

import jaeik.growfarm.dto.farm.CropDTO;
import jaeik.growfarm.dto.farm.VisitCropDTO;
import jaeik.growfarm.entity.crop.Crop;
import jaeik.growfarm.entity.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FarmUtil {

    // Crop -> CropDTO
    public CropDTO convertToCropDTO(Crop crop) {
        CropDTO cropDTO = new CropDTO();
        cropDTO.setId(crop.getId());
        cropDTO.setFarmName(crop.getUsers().getFarmName());
        cropDTO.setCropType(crop.getCropType());
        cropDTO.setNickname(crop.getNickname());
        cropDTO.setMessage(crop.getMessage());
        cropDTO.setWidth(crop.getWidth());
        cropDTO.setHeight(crop.getHeight());
        return cropDTO;
    }

    // CropDTO -> Crop
    public Crop convertToCrop(CropDTO cropDTO, Users user) {
        return Crop.builder()
                .users(user)
                .cropType(cropDTO.getCropType())
                .nickname(cropDTO.getNickname())
                .message(cropDTO.getMessage())
                .width(cropDTO.getWidth())
                .height(cropDTO.getHeight())
                .build();
    }

    // Crop -> VisitFarmDTO
    public VisitCropDTO convertToVisitFarmDTO(Crop crop) {
        VisitCropDTO visitCropDTO = new VisitCropDTO();
        visitCropDTO.setId(crop.getId());
        visitCropDTO.setFarmName(crop.getUsers().getFarmName());
        visitCropDTO.setCropType(crop.getCropType());
        visitCropDTO.setWidth(crop.getWidth());
        visitCropDTO.setHeight(crop.getHeight());
        return visitCropDTO;
    }






}
