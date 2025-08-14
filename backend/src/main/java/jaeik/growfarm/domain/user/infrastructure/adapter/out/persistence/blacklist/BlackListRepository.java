package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.blacklist;

import jaeik.growfarm.domain.user.entity.BlackList;
import jaeik.growfarm.global.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>BlackList Repository Interface</h2>
 * <p>
 * 블랙리스트 관련 데이터베이스 작업을 처리하는 레포지토리 인터페이스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface BlackListRepository extends JpaRepository<BlackList, Long> {

    /**
     * <h3>소셜 제공자와 소셜 ID로 블랙리스트 존재 여부 확인</h3>
     *
     * <p>
     * 주어진 소셜 제공자와 소셜 ID가 블랙리스트에 존재하는지 확인한다.
     * </p>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     * @return 블랙리스트에 존재하면 true, 아니면 false
     */
    boolean existsByProviderAndSocialId(SocialProvider provider, String socialId);
}
