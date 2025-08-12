package jaeik.growfarm.domain.paper.infrastructure.adapter.out;

import jaeik.growfarm.domain.user.application.port.out.UserPort;
import jaeik.growfarm.domain.user.domain.Setting;
import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>사용자 JPA 어댑터</h2>
 * <p>
 * Secondary Adapter: Paper 도메인에서 필요한 사용자 관련 데이터 조회를 위한 JPA 구현
 * 기존 UserRepository의 필요한 기능들을 위임하여 완전히 보존
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@Component
@RequiredArgsConstructor
public class PaperUserJpaAdapter implements UserPort {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findById(Long id) {
        throw new UnsupportedOperationException("PaperUserJpaAdapter는 findById를 지원하지 않습니다.");
    }

    @Override
    public Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        throw new UnsupportedOperationException("PaperUserJpaAdapter는 findByProviderAndSocialId를 지원하지 않습니다.");
    }

    @Override
    public boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    @Override
    public User findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException("PaperUserJpaAdapter는 deleteById를 지원하지 않습니다.");
    }

    @Override
    public Setting save(Setting setting) {
        throw new UnsupportedOperationException("PaperUserJpaAdapter는 save(Setting)를 지원하지 않습니다.");
    }

    @Override
    public User save(User user) {
        throw new UnsupportedOperationException("PaperUserJpaAdapter는 save(User)를 지원하지 않습니다.");
    }
}