package jaeik.bimillog.infrastructure.adapter.user.out.user;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.SettingRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * <h2>사용자 조회 어댑터</h2>
 * <p>사용자 정보 조회를 위한 영속성 어댑터</p>
 * <p>CQRS 패턴에 따라 조회 전용 어댑터로 분리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class UserQueryAdapter implements UserQueryPort {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>주어진 ID로 사용자 정보를 조회합니다.</p>
     *
     * @param id 사용자 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * <h3>ID와 설정을 포함한 사용자 조회</h3>
     * <p>주어진 ID로 사용자 정보를 조회하며, 연관된 설정 정보도 함께 가져옵니다.</p>
     *
     * @param id 사용자 ID
     * @return Optional<User> 조회된 사용자 객체 (설정 정보 포함). 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findByIdWithSetting(Long id) {
        return userRepository.findByIdWithSetting(id);
    }

    /**
     * <h3>소셜 제공자와 소셜 ID로 사용자 조회</h3>
     * <p>주어진 소셜 제공자와 소셜 ID로 사용자 정보를 조회합니다.</p>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     * @return User 조회된 사용자 객체
     * @throws UserCustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return userRepository.findByProviderAndSocialId(provider, socialId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));
    }

    /**
     * <h3>닉네임 존재 여부 확인</h3>
     * <p>주어진 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     *
     * @param userName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>주어진 닉네임으로 사용자 정보를 조회합니다.</p>
     *
     * @param userName 조회할 닉네임
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }


    /**
     * <h3>ID로 설정 조회</h3>
     * <p>주어진 ID로 설정 정보를 조회합니다.</p>
     *
     * @param settingId 조회할 설정 ID
     * @return Optional<Setting> 조회된 설정 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Setting> findSettingById(Long settingId) {
        return settingRepository.findById(settingId);
    }

    /**
     * <h3>주어진 순서대로 사용자 이름 조회</h3>
     * <p>주어진 소셜 ID 목록에 해당하는 사용자 이름들을 요청된 순서대로 조회합니다.</p>
     * <p>카카오 친구 목록 매핑에 사용됩니다.</p>
     *
     * @param socialIds 조회할 소셜 ID 문자열 리스트
     * @return List<String> 조회된 사용자 이름 리스트
     * @author jaeik
     * @since  2.0.0
     */
    @Override
    public List<String> findUserNamesInOrder (List<String> socialIds) {
        return userRepository.findUserNamesInOrder(socialIds);
    }

    /**
     * <h3>ID로 사용자 참조 가져오기</h3>
     * <p>주어진 ID의 사용자 엔티티 참조를 가져옵니다.</p>
     * <p>JPA 연관 관계 설정 시 성능 최적화를 위해 사용됩니다.</p>
     *
     * @param userId 참조를 가져올 사용자 ID
     * @return User 사용자 엔티티 참조
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User getReferenceById(Long userId) {
        return userRepository.getReferenceById(userId);
    }
}
