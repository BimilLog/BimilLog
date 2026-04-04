package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>소셜 토큰 엔티티</h2>
 * <p>사용자의 소셜 플랫폼 OAuth 토큰 정보를 저장하는 통합 엔티티</p>
 * <p>SocialToken이 Member FK를 보유하는 단방향 관계입니다.</p>
 * <p>카카오, 네이버 등 모든 소셜 플랫폼의 토큰을 하나의 테이블로 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "social_token")
public class SocialToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_token_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "access_token", length = 500)
    private String accessToken;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    /**
     * <h3>소셜 토큰 생성</h3>
     * <p>소셜 플랫폼의 OAuth 액세스 토큰, 리프레시 토큰, 연관 Member로 SocialToken 엔티티를 생성합니다.</p>
     *
     * @param accessToken 소셜 플랫폼 액세스 토큰
     * @param refreshToken 소셜 플랫폼 리프레시 토큰
     * @param member 연관 회원 엔티티
     * @return 생성된 SocialToken 엔티티
     */
    public static SocialToken createSocialToken(String accessToken, String refreshToken, Member member) {
        return SocialToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .member(member)
                .build();
    }

    /**
     * <h3>소셜 토큰 업데이트</h3>
     * <p>로그인 시 갱신된 소셜 플랫폼 토큰 정보를 업데이트합니다.</p>
     *
     * @param accessToken 새로운 액세스 토큰
     * @param refreshToken 새로운 리프레시 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
