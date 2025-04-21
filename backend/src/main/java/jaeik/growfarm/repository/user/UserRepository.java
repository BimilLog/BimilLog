package jaeik.growfarm.repository.user;


import jaeik.growfarm.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    Users findByKakaoId(Long kakaoId);

    Users findByTokenId(Long tokenId);

    boolean existsByFarmName(String farmName);

    Users findByFarmName(String farmName);
}