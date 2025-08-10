package jaeik.growfarm.repository.user;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * <h2>사용자 관련 데이터베이스 작업을 수행하는 Repository</h2>
 * <p>사용자 연결 해제 처리 및 카카오 엑세스 토큰 조회 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Repository
@AllArgsConstructor
public class UserJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * <h3>사용자 연결 해제 처리</h3>
     *
     * <p>외래키인 userId를 이용하여 모든 토큰과 FCM토큰 데이터를 삭제합니다.</p>
     * <p>로그아웃, 회원탈퇴시 사용합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 1.0.0
     */
    public void deleteAllTokensByUserId(Long userId) {
        String sql = "DELETE FROM token WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
}
