package jaeik.growfarm.repository;

import jaeik.growfarm.entity.user.BlackList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlackListRepository extends JpaRepository<BlackList, Long> {
    boolean existsByKakaoId(Long kakaoId);
}
