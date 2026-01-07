package jaeik.bimillog.domain.auth.repository;

import jaeik.bimillog.domain.auth.entity.BlackList;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * <h2>블랙리스트 Repository 인터페이스</h2>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface BlackListRepository extends JpaRepository<BlackList, Long> {

    /**
     * <h3>소셜 제공자와 소셜 ID로 블랙리스트 존재 여부 확인</h3>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     * @return 블랙리스트에 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByProviderAndSocialId(SocialProvider provider, String socialId);


}
