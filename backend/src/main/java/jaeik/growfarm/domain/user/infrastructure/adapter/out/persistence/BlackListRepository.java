package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.user.domain.BlackList;
import jaeik.growfarm.global.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*
 * 블랙 리스트 Repository
 * 블랙 리스트 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
@Repository
public interface BlackListRepository extends JpaRepository<BlackList, Long> {

    boolean existsByProviderAndSocialId(SocialProvider provider, String socialId);
}
