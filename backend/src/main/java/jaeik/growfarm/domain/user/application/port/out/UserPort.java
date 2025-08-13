package jaeik.growfarm.domain.user.application.port.out;

import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.domain.user.entity.User;

import java.util.Optional;

/**
 * <h2>User Persistence Port</h2>
 * <p>사용자 정보 영속성 처리를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 1.0
 */
public interface UserPort {
    Optional<User> findById(Long id);
    Optional<User> findByIdWithSetting(Long id);
    Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId);
    boolean existsByUserName(String userName);
    Optional<User> findByUserName(String userName);
    void deleteById(Long id);
    Setting save(Setting setting);
    User save(User user);
    
    // Setting 조회 기능 추가
    Optional<Setting> findSettingById(Long settingId);
    
    /**
     * <h3>사용자 이름 목록을 순서대로 조회</h3>
     * <p>소셜 ID 목록을 받아서 순서대로 사용자 이름을 반환합니다.</p>
     * <p>카카오 친구 목록 매핑에 사용됩니다.</p>
     *
     * @param socialIds 소셜 ID 목록
     * @return 사용자 이름 목록 (순서 유지)
     * @author jaeik
     * @version 2.0.0
     */
    java.util.List<String> findUserNamesInOrder(java.util.List<String> socialIds);
}
