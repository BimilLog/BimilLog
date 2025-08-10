package jaeik.growfarm.service.user;

import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.read.UserReadRepository;
import jaeik.growfarm.repository.user.validation.UserValidationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 프로필 서비스</h2>
 * <p>
 * 사용자의 프로필(닉네임) 및 설정 관리를 담당
 * SRP: 사용자 프로필 및 설정 관리 기능만 담당
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserReadRepository userReadRepository;
    private final UserValidationRepository userValidationRepository;
    private final SettingRepository settingRepository;
    private final UserUpdateService userUpdateService;

    /**
     * <h3>닉네임 변경</h3>
     *
     * <p>
     * Dirty Read의 발생을 막기 위해 커밋된 읽기로 격리 수준 조정
     * </p>
     * <p>
     * 유니크 컬럼이기 때문에 Non-repeatable read 발생해도 문제 없음
     * </p>
     *
     * @param userName    새로운 닉네임
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @Transactional
    public void updateUserName(String userName, CustomUserDetails userDetails) {
        validateUserNameAvailable(userName);
        Users user = userReadRepository.findByIdWithSetting(userDetails.getClientDTO().getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userUpdateService.userNameUpdate(userName, user);
    }

    /**
     * <h3>닉네임 중복 확인</h3>
     *
     * <p>
     * 주어진 닉네임이 이미 존재하는지 확인한다.
     * </p>
     *
     * @param userName 닉네임
     * @return 닉네임이 사용 가능한 경우 true
     * @throws CustomException 닉네임이 이미 존재하는 경우
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public boolean isUserNameAvailable(String userName) {
        validateUserNameAvailable(userName);
        return true;
    }

    /**
     * <h3>사용자 설정 조회</h3>
     *
     * <p>
     * 사용자의 현재 설정 정보를 조회한다.
     * </p>
     *
     * @param userDetails 현재 로그인 한 사용자 정보
     * @return 설정 정보 DTO
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public SettingDTO getSetting(CustomUserDetails userDetails) {
        Setting setting = settingRepository.findById(userDetails.getClientDTO().getSettingId())
                .orElseThrow(() -> new CustomException(ErrorCode.SETTINGS_NOT_FOUND));

        return new SettingDTO(setting);
    }

    /**
     * <h3>사용자 설정 업데이트</h3>
     *
     * <p>
     * 사용자의 알림 설정을 업데이트한다.
     * </p>
     *
     * @param settingDTO  설정 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @Transactional
    public void updateSetting(SettingDTO settingDTO, CustomUserDetails userDetails) {
        Setting setting = settingRepository.findById(userDetails.getClientDTO().getSettingId())
                .orElseThrow(() -> new CustomException(ErrorCode.SETTINGS_NOT_FOUND));

        userUpdateService.settingUpdate(settingDTO, setting);
    }

    /**
     * <h3>닉네임 유효성 검사</h3>
     *
     * <p>
     * 닉네임이 이미 존재하는지 검사하는 내부 메서드
     * </p>
     *
     * @param userName 검사할 닉네임
     * @throws CustomException 닉네임이 이미 존재하는 경우
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    private void validateUserNameAvailable(String userName) {
        if (userValidationRepository.existsByUserName(userName)) {
            throw new CustomException(ErrorCode.EXISTED_NICKNAME);
        }
    }
}