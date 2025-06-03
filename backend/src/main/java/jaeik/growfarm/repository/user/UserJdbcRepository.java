package jaeik.growfarm.repository.user;

import jakarta.transaction.Transactional;
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
    @Transactional
    public void deleteAllTokensByUserId(Long userId) {
        String deleteTokenSql = "DELETE FROM token WHERE user_id = ?";
        String deleteFcmTokenSql = "DELETE FROM fcm_token WHERE user_id = ?";

        jdbcTemplate.update(deleteTokenSql, userId);
        jdbcTemplate.update(deleteFcmTokenSql, userId);
    }

    /**
     * <h3>카카오 엑세스 토큰 가져오기</h3>
     *
     * <p>사용자의 카카오 엑세스 토큰을 반환합니다.</p>
     *
     * @param tokenId 토큰 ID
     * @return 카카오 엑세스 토큰
     * @author Jaeik
     * @since 1.0.0
     */
    public String getKakaoAccessToken(Long tokenId) {
        String getKakaoAccessTokenSql = "SELECT kakao_access_token FROM token WHERE token_id = ?";
        return jdbcTemplate.queryForObject(getKakaoAccessTokenSql, String.class, tokenId);
    }
}
