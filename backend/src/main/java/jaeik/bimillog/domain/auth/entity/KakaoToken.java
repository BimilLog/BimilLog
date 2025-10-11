package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>카카오 토큰 엔티티</h2>
 * <p>사용자의 카카오 OAuth 토큰 정보를 저장하는 엔티티</p>
 * <p>User와 1:1 관계를 가지며, 카카오 API 호출에 사용됩니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "kakao_token")
public class KakaoToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kakao_token_id")
    private Long id;

    @Column(name = "kakao_access_token", length = 500)
    private String kakaoAccessToken;

    @Column(name = "kakao_refresh_token", length = 500)
    private String kakaoRefreshToken;

    /**
     * <h3>카카오 토큰 생성</h3>
     * <p>카카오 OAuth 액세스 토큰과 리프레시 토큰으로 KakaoToken 엔티티를 생성합니다.</p>
     *
     * @param kakaoAccessToken 카카오 액세스 토큰
     * @param kakaoRefreshToken 카카오 리프레시 토큰
     * @return 생성된 KakaoToken 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static KakaoToken createKakaoToken(String kakaoAccessToken, String kakaoRefreshToken) {
        return KakaoToken.builder()
                .kakaoAccessToken(kakaoAccessToken)
                .kakaoRefreshToken(kakaoRefreshToken)
                .build();
    }

    /**
     * <h3>카카오 토큰 업데이트</h3>
     * <p>로그인 시 갱신된 카카오 토큰 정보를 업데이트합니다.</p>
     *
     * @param kakaoAccessToken 새로운 카카오 액세스 토큰
     * @param kakaoRefreshToken 새로운 카카오 리프레시 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateTokens(String kakaoAccessToken, String kakaoRefreshToken) {
        this.kakaoAccessToken = kakaoAccessToken;
        this.kakaoRefreshToken = kakaoRefreshToken;
    }
}