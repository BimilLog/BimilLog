package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * <h3>사용자에 해당하는 토큰 조회</h3>
     *
     * <p>주어진 사용자에 해당하는 토큰을 조회합니다.</p>
     *
     * @param user 사용자 정보
     * @return 해당 사용자의 토큰 (없으면 Optional.empty())
     * @since 2.0.0
     * @author Jaeik
     */
    Optional<Token> findByUser(User user);


    @Modifying
    @Query("DELETE FROM Token t WHERE t.users.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
