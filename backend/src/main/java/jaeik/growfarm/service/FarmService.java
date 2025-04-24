package jaeik.growfarm.service;

import jaeik.growfarm.dto.farm.CropDTO;
import jaeik.growfarm.entity.crop.Crop;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.CropRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.FarmUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FarmService {

    private final CropRepository cropRepository;
    private final UserRepository userRepository;
    private final FarmUtil farmUtil;

    public List<CropDTO> myFarm(Long userId) {
        List<Crop> crops = cropRepository.findByUsersId(userId);

        return crops.stream().map(farmUtil::convertToCropDTO).toList();
    }

    public List<CropDTO> visitFarm(String farmName) {
        Users user = userRepository.findByFarmName(farmName);

        if (user == null) {
            throw new IllegalArgumentException("해당 농장을 찾을 수 없습니다.");
        }

        List<Crop> crops = cropRepository.findByUsersId(user.getId());
        return crops.stream().map(farmUtil::convertToCropDTO).toList();
    }

    public void plantCrop(String farmName, CropDTO cropDTO) {
        Users user = userRepository.findByFarmName(farmName);

        if (user == null) {
            throw new IllegalArgumentException("해당 농장을 찾을 수 없습니다.");
        }

        Crop crop = farmUtil.convertToCrop(cropDTO, user);
        cropRepository.save(crop);
    }

    public CropDTO myFarmCrop(Long cropId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("해당 농작물을 찾을 수 없습니다."));
        return farmUtil.convertToCropDTO(crop);
    }

    public void deleteCrop(Long cropId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("해당 농작물을 찾을 수 없습니다."));
        cropRepository.delete(crop);
    }
}
