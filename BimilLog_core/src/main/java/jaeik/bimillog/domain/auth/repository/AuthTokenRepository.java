package jaeik.bimillog.domain.auth.repository;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>인증 토큰 Jpa Repository</h2>
 * <p>JpaRepository를 상속받은 인터페이스입니다.</p>
 * <p>인증 토큰 관련 데이터베이스 작업을 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    /**
     * <h3>회원 ID로 모든 토큰 조회</h3>
     * <p>주어진 회원 ID에 해당하는 모든 토큰을 조회합니다.</p>
     * <p>회원 탈퇴 시 모든 토큰을 블랙리스트에 등록하기 위해 사용됩니다.</p>
     *
     * @param memberId 회원 ID
     * @return 회원의 모든 토큰 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<AuthToken> findByMemberId(Long memberId);

    /**
     * <h3>회원 ID로 모든 토큰 삭제</h3>
     * <p>주어진 회원 ID에 해당하는 모든 토큰을 삭제합니다.</p>
     *
     * @param memberId 회원 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM AuthToken t WHERE t.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
