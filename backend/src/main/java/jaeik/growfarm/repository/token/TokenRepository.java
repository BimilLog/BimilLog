package jaeik.growfarm.repository.token;

import jaeik.growfarm.entity.user.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>토큰 Jpa Repository</h2>
 * <p>JpaRepository를 상속받은 인터페이스입니다.</p>
 * <p>토큰 관련 데이터베이스 작업을 수행합니다.</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

}
