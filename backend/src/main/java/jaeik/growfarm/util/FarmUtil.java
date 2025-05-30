package jaeik.growfarm.util;

import jaeik.growfarm.dto.farm.CropDTO;
import jaeik.growfarm.dto.farm.VisitCropDTO;
import jaeik.growfarm.entity.crop.Crop;
import jaeik.growfarm.entity.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/*
 * FarmUtil 클래스
 * 농장 관련 유틸리티 클래스
 * 수정일 : 2025-05-03
 */
@Component
@RequiredArgsConstructor
public class FarmUtil {

    /**
     * <h3>Crop 엔티티를 CropDTO로 변환</h3>
     *
     * <p>
     * Crop 엔티티를 CropDTO 객체로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param crop Crop 엔티티
     * @return 농작물 DTO
     */
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

    /**
     * <h3>CropDTO를 Crop 엔티티로 변환</h3>
     *
     * <p>
     * CropDTO 객체를 Crop 엔티티로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param cropDTO 농작물 DTO
     * @param user    농장 소유자 정보
     * @return Crop 엔티티
     */
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

    /**
     * <h3>Crop 엔티티를 VisitCropDTO로 변환</h3>
     *
     * <p>
     * 농장 방문 시 사용하는 농작물 DTO로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param crop Crop 엔티티
     * @return 방문 농장 농작물 DTO
     */
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
