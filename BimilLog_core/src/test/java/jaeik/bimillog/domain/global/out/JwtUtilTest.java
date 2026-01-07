package jaeik.bimillog.domain.global.out;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.entity.MemberRole;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.web.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>JwtUtil 단위 테스트</h2>
 * <p>JWT 생성과 파싱, 해시 생성 로직을 검증한다.</p>
 */
@Tag("unit")
class JwtUtilTest {

    private static final String RAW_SECRET = "0123456789abcdef0123456789abcdef";

    private JwtUtil jwtUtil;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        String secret = Base64.getEncoder().encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwtUtil, "secretKey", secret);
        jwtUtil.init();

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        userDetails = CustomUserDetails.builder()
                .memberId(1L)
                .socialId("social-1")
                .provider(SocialProvider.KAKAO)
                .settingId(10L)
                .socialNickname("소셜닉")
                .thumbnailImage("thumb.jpg")
                .memberName("tester")
                .role(MemberRole.USER)
                .authTokenId(99L)
                .authorities(authorities)
                .build();
    }

    @Test
    @DisplayName("액세스 토큰 생성 후 사용자 정보를 역직렬화한다")
    void shouldGenerateAndParseAccessToken() {
        String accessToken = jwtUtil.generateAccessToken(userDetails);

        assertThat(accessToken).isNotBlank();
        assertThat(jwtUtil.validateToken(accessToken)).isTrue();

        CustomUserDetails parsed = jwtUtil.getUserInfoFromToken(accessToken);

        assertThat(parsed.getMemberId()).isEqualTo(userDetails.getMemberId());
        assertThat(parsed.getAuthTokenId()).isEqualTo(userDetails.getAuthTokenId());
        assertThat(parsed.getMemberName()).isEqualTo(userDetails.getMemberName());
        assertThat(parsed.getProvider()).isEqualTo(userDetails.getProvider());
    }

    @Test
    @DisplayName("리프레시 토큰에서 토큰 ID를 추출한다")
    void shouldGenerateRefreshTokenAndExtractTokenId() {
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtUtil.getTokenIdFromToken(refreshToken)).isEqualTo(userDetails.getAuthTokenId());
    }

    @Test
    @DisplayName("만료 임계값 이하의 토큰은 갱신 대상으로 간주한다")
    void shouldRecommendRefreshWhenRemainingDaysBelowThreshold() {
        Key key = (Key) ReflectionTestUtils.getField(jwtUtil, "key");
        long now = System.currentTimeMillis();

        String shortLivedToken = Jwts.builder()
                .setSubject("99")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + TimeUnit.DAYS.toMillis(1)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String longLivedToken = Jwts.builder()
                .setSubject("99")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + TimeUnit.DAYS.toMillis(60)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertThat(jwtUtil.shouldRefreshToken(shortLivedToken, 15)).isTrue();
        assertThat(jwtUtil.shouldRefreshToken(longLivedToken, 15)).isFalse();
    }

    @Test
    @DisplayName("토큰 해시값은 안정적으로 생성된다")
    void shouldGenerateStableTokenHash() {
        String token = "sample-token";

        String hash1 = jwtUtil.generateTokenHash(token);
        String hash2 = jwtUtil.generateTokenHash(token);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
    }
}
