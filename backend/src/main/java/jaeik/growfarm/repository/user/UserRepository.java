package jaeik.growfarm.repository.user;

import jaeik.growfarm.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * <h3>사용자 이름으로 사용자 존재 여부 확인</h3>
     *
     * <p>
     * 사용자 이름이 이미 존재하는지 확인한다.
     * </p>
     *
     * @param userName 사용자 이름
     * @return 존재 여부 (true/false)
     */
    boolean existsByUserName(String userName);

    /**
     * <h3>ID로 사용자와 설정 조회</h3>
     *
     * <p>
     * 사용자 ID를 통해 사용자와 설정 정보를 함께 조회한다.
     * </p>
     * 
     * @param id 사용자 ID
     * @return 사용자 정보 (Setting 포함)
     * @author Jaeik
     * @since 1.0.0
     */
    @Query("SELECT u FROM Users u JOIN FETCH u.setting WHERE u.id = :id")
    Optional<Users> findByIdWithSetting(@Param("id") Long id);
}