package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.in.SettingQueryUseCase;
import jaeik.growfarm.domain.user.application.port.out.UserPort;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>설정 조회 서비스</h2>
 * <p>설정 조회 유스케이스를 구현하는 Application Service</p>
 * <p>헥사고날 아키텍처에서 비즈니스 로직을 담당하며, 설정 조회 기능을 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class SettingQueryService implements SettingQueryUseCase {

    private final UserPort userPort;

    /**
     * <h3>설정 ID로 설정 조회</h3>
     * <p>JWT 토큰의 settingId를 활용하여 효율적으로 설정 정보를 조회</p>
     * <p>User 엔티티 전체 조회 없이 Setting만 직접 조회하여 성능 최적화</p>
     *
     * @param settingId 설정 ID
     * @return 설정 DTO
     * @throws CustomException 설정을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public SettingDTO findBySettingId(Long settingId) {
        log.debug("Finding setting by settingId: {}", settingId);
        
        Setting setting = userPort.findSettingById(settingId)
                .orElseThrow(() -> {
                    log.warn("Setting not found for settingId: {}", settingId);
                    return new CustomException(ErrorCode.SETTINGS_NOT_FOUND);
                });

        log.debug("Successfully found setting for settingId: {}", settingId);
        return new SettingDTO(setting);
    }
}