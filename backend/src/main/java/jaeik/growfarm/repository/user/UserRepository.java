package jaeik.growfarm.repository.user;

import jaeik.growfarm.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
public interface UserRepository extends JpaRepository<Users, Long> {

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
     * <h3>농장 이름 존재 여부 확인</h3>
     *
     * <p>
     * 해당 농장 이름이 이미 존재하는지 확인한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param farmName 농장 이름
     * @return 존재 여부
     */
    boolean existsByFarmName(String farmName);

    /**
     * <h3>농장 이름으로 사용자 조회</h3>
     *
     * <p>
     * 농장 이름을 통해 사용자를 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param farmName 농장 이름
     * @return 사용자 정보
     */
    Users findByFarmName(String farmName);

    /**
     * <h3>카카오 ID 목록으로 농장 이름 조회</h3>
     *
     * <p>
     * 카카오 ID 목록의 순서대로 농장 이름을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param ids 카카오 ID 목록
     * @return 농장 이름 목록
     */
    @Query(value = "SELECT u.farm_name FROM users u WHERE u.kakao_id IN (:ids) ORDER BY FIELD(u.kakao_id, :#{#ids})", nativeQuery = true)
    List<String> findFarmNamesInOrder(@Param("ids") List<Long> ids);
}