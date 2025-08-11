package jaeik.growfarm.repository.token;

import jaeik.growfarm.domain.user.domain.Token;
import jaeik.growfarm.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>토큰 Jpa Repository</h2>
 * <p>JpaRepository를 상속받은 인터페이스입니다.</p>
 * <p>토큰 관련 데이터베이스 작업을 수행합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByUser(User user);
}
