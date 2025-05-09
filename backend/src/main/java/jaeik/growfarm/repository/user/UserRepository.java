package jaeik.growfarm.repository.user;


import jaeik.growfarm.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/*
 * 유저 Repository
 * 유저 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByKakaoId(Long kakaoId);

    Users findByTokenId(Long tokenId);

    boolean existsByFarmName(String farmName);

    Users findByFarmName(String farmName);

    @Query(value = "SELECT u.farm_name FROM users u WHERE u.kakao_id IN (:ids) ORDER BY FIELD(u.kakao_id, :#{#ids})", nativeQuery = true)
    List<String> findFarmNamesInOrder(@Param("ids") List<Long> ids);}