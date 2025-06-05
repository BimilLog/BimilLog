package jaeik.growfarm.service;

import jaeik.growfarm.dto.farm.CropDTO;
import jaeik.growfarm.dto.farm.VisitCropDTO;
import jaeik.growfarm.entity.crop.Crop;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.event.FarmPlantEvent;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.farm.CropRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.notification.NotificationService;
import jaeik.growfarm.util.FarmUtil;
import jaeik.growfarm.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

    // 이벤트 발행을 위한 ApplicationEventPublisher 🚀
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>내 농장 조회</h3>
     *
     * <p>
     * 사용자 ID를 통해 해당 사용자의 농작물 목록을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userId 사용자 ID
     * @return 농작물 목록
     */
    public List<CropDTO> myFarm(Long userId) {
        List<Crop> crops = cropRepository.findByUsersId(userId);

        return crops.stream().map(farmUtil::convertToCropDTO).toList();
    }

    /**
     * <h3>다른 농장 방문</h3>
     *
     * <p>
     * 농장 이름을 통해 해당 농장의 농작물 목록을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param farmName 농장 이름
     * @return 방문 농장의 농작물 목록
     */
    public List<VisitCropDTO> visitFarm(String farmName) {
        Users user = userRepository.findByFarmName(farmName);

        if (user == null) {
            throw new IllegalArgumentException("해당 농장을 찾을 수 없습니다.");
        }

        List<Crop> crops = cropRepository.findByUsersId(user.getId());
        return crops.stream().map(farmUtil::convertToVisitFarmDTO).toList();
    }

    /**
     * <h3>농작물 심기</h3>
     *
     * <p>
     * 다른 사용자의 농장에 농작물을 심고 농장 주인에게 알림을 발송한다.
     * 이벤트 기반 아키텍처로 SSE와 FCM 알림을 비동기 처리한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param farmName 농장 이름
     * @param cropDTO  심을 농작물 정보 DTO
     * @throws IOException FCM 메시지 발송 오류 시 발생
     */
    public void plantCrop(String farmName, CropDTO cropDTO) throws IOException {
        Users user = userRepository.findByFarmName(farmName);

        if (user == null) {
            throw new IllegalArgumentException("해당 농장을 찾을 수 없습니다.");
        }

        // 농작물 저장 (동기)
        Crop crop = farmUtil.convertToCrop(cropDTO, user);
        cropRepository.save(crop);

        // 이벤트 발행 🚀 (알림은 이벤트 리스너에서 비동기로 처리)
        eventPublisher.publishEvent(new FarmPlantEvent(
                user.getId(),
                farmName,
                user));
    }

    /**
     * <h3>농작물 삭제</h3>
     *
     * <p>
     * 농작물 소유자만 해당 농작물을 삭제할 수 있다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @param cropId      농작물 ID
     */
    public void deleteCrop(CustomUserDetails userDetails, Long cropId) {
        if (userDetails == null) {
            throw new RuntimeException("다시 로그인 해 주세요.");
        }

        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("해당 농작물을 찾을 수 없습니다."));

        if (!crop.getUsers().getId().equals(userDetails.getClientDTO().getUserId())) {
            throw new RuntimeException("본인 농장의 농작물만 삭제할 수 있습니다.");
        }

        cropRepository.delete(crop);
    }
}
