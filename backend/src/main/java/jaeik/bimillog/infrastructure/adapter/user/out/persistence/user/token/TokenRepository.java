package jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.token;

import jaeik.bimillog.domain.user.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>토큰 Jpa Repository</h2>
 * <p>JpaRepository를 상속받은 인터페이스입니다.</p>
 * <p>토큰 관련 데이터베이스 작업을 수행합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    /**
     * <h3>사용자 ID로 모든 토큰 조회</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 토큰을 조회합니다.</p>
     * <p>회원 탈퇴 시 모든 토큰을 블랙리스트에 등록하기 위해 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return 사용자의 모든 토큰 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Token> findByUsersId(Long userId);

    /**
     * <h3>사용자 ID로 모든 토큰 삭제</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 토큰을 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM Token t WHERE t.users.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
