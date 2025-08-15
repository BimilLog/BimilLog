
package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import jaeik.growfarm.domain.user.application.port.out.UserCommandPort;
import jaeik.growfarm.domain.user.application.port.out.UserQueryPort;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.event.UserSignedUpEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 명령 서비스</h2>
 * <p>사용자 관련 명령 유스케이스를 구현하는 Application Service</p>
 * <p>헥사고날 아키텍처에서 비즈니스 로직을 담당하며, 사용자 설정 업데이트 및 이벤트 처리 기능을 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserCommandService implements UserCommandUseCase {

    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;

/**
     * <h3>사용자 설정 수정</h3>
     * <p>사용자의 설정을 수정하는 메서드</p>
     *
     * @param userId    사용자 ID
     * @param settingDTO 수정할 설정 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void updateUserSettings(Long userId, SettingDTO settingDTO) {
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Setting setting = user.getSetting();
        setting.updateSetting(settingDTO);
        userCommandPort.save(user);
    }

    /**
     * <h3>닉네임 변경</h3>
     * <p>사용자의 닉네임을 변경하는 메서드</p>
     *
     * @param userId      사용자 ID
     * @param newUserName 새로운 닉네임
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void updateUserName(Long userId, String newUserName) {
        if (userQueryPort.existsByUserName(newUserName)) {
            throw new CustomException(ErrorCode.EXISTED_NICKNAME);
        }
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateUserName(newUserName);
        userCommandPort.save(user);
    }


    /**
     * <h3>사용자 가입 이벤트 처리</h3>
     * <p>새로운 사용자가 가입했을 때 기본 설정을 초기화하는 메서드입니다.</p>
     *
     * @param event 사용자 가입 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @Transactional
    @EventListener
    public void handleUserSignedUpEvent(UserSignedUpEvent event) {
        User user = userQueryPort.findById(event.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        Setting setting = Setting.createSetting();
        user.updateSetting(setting);
        userCommandPort.save(user);
        log.info("Initialized default settings for new user (ID: {})", event.userId());
    }
}
