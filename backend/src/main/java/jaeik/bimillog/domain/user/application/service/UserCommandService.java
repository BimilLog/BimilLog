
package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.application.port.out.UserCommandPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.BlackList;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SettingVO;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 명령 서비스</h2>
 * <p>사용자 관련 명령 유스케이스를 구현하는 애플리케이션 서비스</p>
 * <p>헥사고날 아키텍처에서 비즈니스 로직을 담당하며, 사용자 설정 업데이트 및 이벤트 처리 기능을 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
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
     * @param settingVO 수정할 설정 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void updateUserSettings(Long userId, SettingVO settingVO) {
        if (settingVO == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Setting setting = user.getSetting();
        setting.updateSetting(settingVO);
        userCommandPort.save(user);
    }

    /**
     * <h3>닉네임 변경</h3>
     * <p>사용자의 닉네임을 변경하는 메서드</p>
     * <p>Race Condition 방지를 위해 데이터베이스 UNIQUE 제약조건 위반 예외를 처리합니다.</p>
     *
     * @param userId      사용자 ID
     * @param newUserName 새로운 닉네임
     * @throws CustomException EXISTED_NICKNAME - 닉네임이 중복된 경우
     * @throws CustomException USER_NOT_FOUND - 사용자를 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void updateUserName(Long userId, String newUserName) {
        // 1차 중복 확인 (성능 최적화를 위한 사전 검사)
        if (userQueryPort.existsByUserName(newUserName)) {
            throw new CustomException(ErrorCode.EXISTED_NICKNAME);
        }
        
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        user.updateUserName(newUserName);
        
        try {
            // 데이터베이스 저장 시도
            userCommandPort.save(user);
        } catch (DataIntegrityViolationException e) {
            // Race Condition 발생 시: 다른 사용자가 동시에 같은 닉네임으로 변경한 경우
            // 데이터베이스 UNIQUE 제약조건 위반으로 인한 예외를 커스텀 예외로 변환
            log.warn("닉네임 경쟁 상태 감지됨 - 사용자 ID: {}, 새 닉네임: {}", userId, newUserName, e);
            throw new CustomException(ErrorCode.EXISTED_NICKNAME);
        }
    }

    /**
     * <h3>사용자 삭제</h3>
     * <p>ID를 통해 사용자를 삭제하는 메서드</p>
     *
     * @param userId 삭제할 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void deleteById(Long userId) {
        userCommandPort.deleteById(userId);
    }

    /**
     * <h3>사용자 저장</h3>
     * <p>사용자 정보를 저장하거나 업데이트하는 메서드</p>
     *
     * @param user 저장할 사용자 엔티티
     * @return User 저장된 사용자 엔티티
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public User save(User user) {
        return userCommandPort.save(user);
    }

    /**
     * {@inheritDoc}
     *
     * <p>사용자 ID를 기반으로 사용자를 조회하고 해당 사용자의 소셜 정보로 블랙리스트에 추가합니다.</p>
     * <p>중복 등록 방지를 위해 데이터베이스 UNIQUE 제약조건을 활용합니다.</p>
     */
    @Override
    public void addToBlacklist(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        BlackList blackList = BlackList.createBlackList(user.getSocialId(), user.getProvider());
        
        try {
            userCommandPort.save(blackList);
            log.info("사용자 블랙리스트 추가 완료 - userId: {}, socialId: {}, provider: {}", 
                    userId, user.getSocialId(), user.getProvider());
        } catch (DataIntegrityViolationException e) {
            log.warn("이미 블랙리스트에 등록된 사용자 - userId: {}, socialId: {}", 
                    userId, user.getSocialId());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>사용자의 역할을 BAN으로 변경하여 일정 기간 서비스 이용을 제한합니다.</p>
     * <p>BAN 상태의 사용자는 로그인은 가능하지만 주요 기능 이용이 제한됩니다.</p>
     */
    @Override
    public void banUser(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateRole(UserRole.BAN);
        userCommandPort.save(user);
        
        log.info("사용자 제재 완료 - userId: {}, userName: {}, 이전 역할: {}, 현재 역할: BAN", 
                userId, user.getUserName(), user.getRole());
    }

}
