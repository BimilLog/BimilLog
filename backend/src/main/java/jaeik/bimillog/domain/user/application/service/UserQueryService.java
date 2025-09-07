
package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.application.port.out.TokenPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>사용자 조회 서비스</h2>
 * <p>UserQueryUseCase의 구현체. 사용자 정보 조회 로직을 담당합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryService implements UserQueryUseCase {

    private final UserQueryPort userQueryPort;
    private final TokenPort tokenPort;

    /**
     * <h3>소셜 정보로 사용자 조회</h3>
     * <p>제공자(Provider)와 소셜 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param provider 소셜 로그인 제공자
     * @param socialId 사용자의 소셜 ID
     * @return User 조회된 사용자 객체
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public User findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return userQueryPort.findByProviderAndSocialId(provider, socialId);
    }

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param id 사용자의 고유 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findById(Long id) {
        return userQueryPort.findById(id);
    }

    /**
     * <h3>닉네임 중복 확인</h3>
     * <p>해당 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     *
     * @param userName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByUserName(String userName) {
        return userQueryPort.existsByUserName(userName);
    }

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>닉네임을 사용하여 사용자를 조회합니다.</p>
     *
     * @param userName 사용자 닉네임
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findByUserName(String userName) {
        return userQueryPort.findByUserName(userName);
    }


    /**
     * <h3>ID로 사용자 프록시 조회</h3>
     * <p>실제 쿼리 없이 ID를 가진 사용자의 프록시(참조) 객체를 반환합니다.</p>
     * <p>JPA 연관 관계 설정 시 성능 최적화를 위해 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return User 프록시 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User getReferenceById(Long userId) {
        return userQueryPort.getReferenceById(userId);
    }

    /**
     * <h3>토큰 ID로 토큰 조회</h3>
     * <p>다중 로그인 환경에서 JWT에서 파싱된 tokenId로 정확한 기기의 토큰을 조회합니다.</p>
     *
     * @param tokenId 토큰 ID (UserDetails.getTokenId()에서 추출)
     * @return Optional<Token> 조회된 토큰 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Token> findTokenById(Long tokenId) {
        return tokenPort.findById(tokenId);
    }
}
