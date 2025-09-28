package jaeik.bimillog.infrastructure.adapter.out.user.jpa;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>사용자 Repository</h2>
 * <p>
 * 사용자 관련 데이터베이스 작업을 위한 Repository
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * <h3>소셜 제공자와 소셜 ID로 사용자 조회</h3>
     * <p>주어진 소셜 제공자와 소셜 ID로 사용자 정보를 조회합니다.</p>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId);

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>주어진 닉네임으로 사용자 정보를 조회합니다.</p>
     *
     * @param userName 조회할 닉네임
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findByUserName(String userName);

    /**
     * <h3>닉네임 존재 여부 확인</h3>
     * <p>주어진 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     *
     * @param userName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByUserName(String userName);

    /**
     * <h3>사용자와 설정 동시 삭제</h3>
     * <p>사용자 ID를 기준으로 User와 Setting을 동시에 삭제합니다.</p>
     * <p>Native Query를 사용하여 JOIN으로 연관된 데이터를 한번에 삭제합니다.</p>
     * <p>회원 탈퇴 처리 시 UserCommandAdapter에서 호출됩니다.</p>
     *
     * @param userId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = "DELETE s, u FROM setting s " +
                   "INNER JOIN user u ON s.setting_id = u.setting_id " +
                   "WHERE u.user_id = :userId", nativeQuery = true)
    void deleteUserAndSettingByUserId(@Param("userId") Long userId);
}