package jaeik.growfarm.domain.user.application.port.in;

import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;

import java.util.Optional;

/**
 * <h2>사용자 조회 유스케이스</h2>
 * <p>사용자 정보 조회를 위한 In-Port</p>
 *
 * @author jaeik
 * @version 1.0.0
 */
public interface UserQueryUseCase {

    /**
     * <h3>소셜 정보로 사용자 조회</h3>
     * <p>제공자(Provider)와 소셜 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param provider 소셜 로그인 제공자
     * @param socialId 사용자의 소셜 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author jaeik
     * @version 1.0.0
     */
    Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId);

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param id 사용자의 고유 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author jaeik
     * @version 1.0.0
     */
    Optional<User> findById(Long id);

    /**
     * <h3>닉네임 중복 확인</h3>
     * <p>해당 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     *
     * @param userName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author jaeik
     * @version 1.0.0
     */
    boolean existsByUserName(String userName);
}
