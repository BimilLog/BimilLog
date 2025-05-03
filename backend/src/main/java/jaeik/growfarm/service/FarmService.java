package jaeik.growfarm.service;

import jaeik.growfarm.dto.farm.CropDTO;
import jaeik.growfarm.dto.farm.VisitCropDTO;
import jaeik.growfarm.dto.notification.FcmSendDTO;
import jaeik.growfarm.entity.crop.Crop;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.farm.CropRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.FarmUtil;
import jaeik.growfarm.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/*
 * FarmService 클래스
 * 농장 관련 서비스 클래스
 * 수정일 : 2025-05-03
 */
@Service
@RequiredArgsConstructor
public class FarmService {

    private final CropRepository cropRepository;
    private final UserRepository userRepository;
    private final FarmUtil farmUtil;
    private final NotificationService notificationService;
    private final NotificationUtil notificationUtil;
    private final FcmTokenRepository fcmTokenRepository;

    public List<CropDTO> myFarm(Long userId) {
        List<Crop> crops = cropRepository.findByUsersId(userId);

        return crops.stream().map(farmUtil::convertToCropDTO).toList();
    }

    public List<VisitCropDTO> visitFarm(String farmName) {
        Users user = userRepository.findByFarmName(farmName);

        if (user == null) {
            throw new IllegalArgumentException("해당 농장을 찾을 수 없습니다.");
        }

        List<Crop> crops = cropRepository.findByUsersId(user.getId());
        return crops.stream().map(farmUtil::convertToVisitFarmDTO).toList();
    }

    public void plantCrop(String farmName, CropDTO cropDTO) throws IOException {
        Users user = userRepository.findByFarmName(farmName);

        if (user == null) {
            throw new IllegalArgumentException("해당 농장을 찾을 수 없습니다.");
        }

        Crop crop = farmUtil.convertToCrop(cropDTO, user);
        cropRepository.save(crop);

        notificationService.send(user.getId(), notificationUtil.createEventDTO(NotificationType.FARM, "누군가가 농장에 농작물을 심었습니다!", "http://localhost:3000/farm/" + farmName));


        if (user.getSetting().isFarmNotification()) {

            // farmName으로 유저의 fcmToken을 가져와서 알림 전송
            List<FcmToken> fcmTokens = fcmTokenRepository.findByUsers(user);

            for (FcmToken fcmToken : fcmTokens) {
                notificationService.sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title("누군가가 농장에 농작물을 심었습니다!")
                        .body("지금 확인해보세요!")
                        .build()
                );
            }
        }
    }

    public void deleteCrop(Long cropId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("해당 농작물을 찾을 수 없습니다."));
        cropRepository.delete(crop);
    }
}
