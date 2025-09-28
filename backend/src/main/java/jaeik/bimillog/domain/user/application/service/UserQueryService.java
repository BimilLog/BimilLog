
package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.infrastructure.adapter.in.user.web.UserQueryController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>사용자 조회 서비스</h2>
 * <p>UserQueryUseCase의 구현체로 사용자 정보 조회 로직을 담당합니다.</p>
 * <p>사용자 엔티티 조회, 설정 조회, 닉네임 검증</p>
 * <p>소셜 로그인 사용자 조회, 토큰 기반 인증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class UserQueryService implements UserQueryUseCase {

    private final UserQueryPort userQueryPort;
    private final GlobalTokenQueryPort globalTokenQueryPort;

    /**
     * <h3>소셜 정보로 사용자 조회</h3>
     * <p>제공자(Provider)와 소셜 ID를 사용하여 사용자를 조회합니다.</p>
     * <p>{@link UserQueryUseCase}에서 소셜 로그인 사용자 조회 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자
     * @param socialId 사용자의 소셜 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return userQueryPort.findByProviderAndSocialId(provider, socialId);
    }

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     * <p>{@link UserQueryUseCase}에서 기본 사용자 조회 시 호출됩니다.</p>
     *
     * @param id 사용자의 고유 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userQueryPort.findById(id);
    }

    /**
     * <h3>닉네임 중복 확인</h3>
     * <p>해당 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     * <p>{@link UserQueryController}에서 닉네임 중복 확인 API 시 호출됩니다.</p>
     *
     * @param userName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserName(String userName) {
        return userQueryPort.existsByUserName(userName);
    }

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>닉네임을 사용하여 사용자를 조회합니다.</p>
     * <p>{@link UserQueryUseCase}에서 닉네임 기반 사용자 조회 시 호출됩니다.</p>
     *
     * @param userName 사용자 닉네임
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUserName(String userName) {
        return userQueryPort.findByUserName(userName);
    }


    /**
     * <h3>ID로 사용자 프록시 조회</h3>
     * <p>실제 쿼리 없이 ID를 가진 사용자의 프록시(참조) 객체를 반환합니다.</p>
     * <p>JPA 연관 관계 설정 시 사용됩니다.</p>
     * <p>{@link UserQueryUseCase}에서 사용자 엔티티 참조 생성 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return User 프록시 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public User getReferenceById(Long userId) {
        return userQueryPort.getReferenceById(userId);
    }

    /**
     * <h3>설정 ID로 설정 조회</h3>
     * <p>JWT 토큰의 settingId를 활용하여 설정 정보를 조회합니다.</p>
     * <p>User 엔티티 전체 조회 없이 Setting만 직접 조회합니다.</p>
     * <p>{@link UserQueryController}에서 사용자 설정 조회 API 시 호출됩니다.</p>
     *
     * @param settingId 설정 ID
     * @return 설정 엔티티
     * @throws UserCustomException 설정을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional(readOnly = true)
    public Setting findBySettingId(Long settingId) {
        return userQueryPort.findSettingById(settingId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.SETTINGS_NOT_FOUND));
    }
}
