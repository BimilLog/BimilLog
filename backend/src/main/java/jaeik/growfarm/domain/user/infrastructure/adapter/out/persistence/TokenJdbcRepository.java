package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


/**
 * <h2>토큰 Jdbc Repository</h2>
 * <p>JdbcTemplate을 사용하여 토큰 관련 데이터베이스 작업을 수행합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Repository
@AllArgsConstructor
public class TokenJdbcRepository {

    private JdbcTemplate jdbcTemplate;

    /**
     * <h3>JWT 리프레시 토큰 업데이트</h3>
     *
     * <p>주어진 토큰 ID에 해당하는 JWT 리프레시 토큰을 업데이트합니다.</p>
     * <p>로그인, 리프레시 토큰 재발급때 사용합니다.</p>
     *
     * @param tokenId            토큰 ID
     * @param newJwtRefreshToken 새로 설정할 JWT 리프레시 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    public void UpdateJwtRefreshToken(Long tokenId, String newJwtRefreshToken) {
        String sql = "UPDATE token SET jwt_refresh_token = ? WHERE token_id = ?";
        jdbcTemplate.update(sql, newJwtRefreshToken, tokenId);
    }
}