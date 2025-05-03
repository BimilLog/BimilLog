package jaeik.growfarm.repository.user;


import jaeik.growfarm.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*
 * 유저 Repository
 * 유저 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    Users findByKakaoId(Long kakaoId);

    Users findByTokenId(Long tokenId);

    boolean existsByFarmName(String farmName);

    Users findByFarmName(String farmName);
}