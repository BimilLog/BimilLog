package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.global.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>사용자 Repository</h2>
 * <p>
 * 사용자 관련 데이터베이스 작업을 위한 Repository
 * </p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, UserCustomRepository {

    Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId);

    Optional<User> findByUserName(String userName);

    boolean existsByUserName(String userName);
}