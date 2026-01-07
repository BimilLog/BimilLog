package jaeik.bimillog.domain.auth.repository;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * <h2>소셜 토큰 JPA Repository</h2>
 * <p>JpaRepository를 상속받은 인터페이스입니다.</p>
 * <p>소셜 플랫폼 OAuth 토큰 관련 데이터베이스 작업을 수행합니다.</p>
 * <p>Member와 1:1 관계를 가지므로 Member를 통해 직접 접근합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface SocialTokenRepository extends JpaRepository<SocialToken, Long> {

    /**
     * <h3>사용자 ID로 소셜 토큰 삭제</h3>
     * <p>회원 탈퇴 시 소셜 토큰을 삭제합니다.</p>
     *
     * @param memberId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM SocialToken st WHERE st.id IN (SELECT m.socialToken.id FROM Member m WHERE m.id = :memberId)")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
