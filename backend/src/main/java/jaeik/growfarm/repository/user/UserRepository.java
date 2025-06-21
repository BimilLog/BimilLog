package jaeik.growfarm.repository.user;

import jaeik.growfarm.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>사용자 Repository</h2>
 * <p>
 * 사용자 관련 데이터베이스 작업을 수행하는 Repository
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long>, UserCustomRepository {

    /**
     * <h3>카카오 ID로 사용자 조회</h3>
     *
     * <p>
     * 카카오 ID를 통해 사용자를 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param kakaoId 카카오 ID
     * @return 사용자 정보 (Optional)
     */
    Optional<Users> findByKakaoId(Long kakaoId);

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     *
     * <p>
     * 닉네임을 통해 사용자를 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param UserName 유저 닉네임
     * @return 사용자 정보
     */
    Users findByUserName(String UserName);

    boolean existsByUserName(String userName);
}