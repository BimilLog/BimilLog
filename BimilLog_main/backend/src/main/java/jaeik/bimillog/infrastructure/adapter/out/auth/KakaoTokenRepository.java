package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>카카오 토큰 Jpa Repository</h2>
 * <p>JpaRepository를 상속받은 인터페이스입니다.</p>
 * <p>카카오 OAuth 토큰 관련 데이터베이스 작업을 수행합니다.</p>
 * <p>Member와 1:1 관계를 가지므로 memberId로 조회 시 Optional 반환</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface KakaoTokenRepository extends JpaRepository<KakaoToken, Long> {

    /**
     * <h3>사용자 ID로 카카오 토큰 조회</h3>
     * <p>Member와 1:1 관계를 가지므로 하나의 KakaoToken만 존재합니다.</p>
     * <p>카카오 API 호출 시 액세스 토큰이 필요할 때 사용됩니다.</p>
     *
     * @param memberId 사용자 ID
     * @return 사용자의 카카오 토큰 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT kt FROM KakaoToken kt WHERE kt.id IN (SELECT u.kakaoToken.id FROM Member u WHERE u.id = :memberId)")
    Optional<KakaoToken> findByMemberId(@Param("memberId") Long memberId);

    /**
     * <h3>사용자 ID로 카카오 토큰 삭제</h3>
     * <p>회원 탈퇴 시 카카오 토큰을 삭제합니다.</p>
     *
     * @param memberId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM KakaoToken kt WHERE kt.id IN (SELECT u.kakaoToken.id FROM Member u WHERE u.id = :memberId)")
    void deleteByMemberId(@Param("memberId") Long memberId);
}