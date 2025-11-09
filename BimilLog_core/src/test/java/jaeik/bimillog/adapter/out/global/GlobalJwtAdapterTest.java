package jaeik.bimillog.adapter.out.global;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jaeik.bimillog.domain.global.entity.MemberDetail;
import jaeik.bimillog.domain.member.entity.MemberRole;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.global.out.GlobalJwtAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>GlobalJwtAdapter 단위 테스트</h2>
 * <p>JWT 생성과 파싱, 해시 생성 로직을 검증한다.</p>
 */
@Tag("unit")
class GlobalJwtAdapterTest {

    private static final String RAW_SECRET = "0123456789abcdef0123456789abcdef";

    private GlobalJwtAdapter globalJwtAdapter;
    private MemberDetail memberDetail;

    @BeforeEach
    void setUp() {
        globalJwtAdapter = new GlobalJwtAdapter();
        String secret = Base64.getEncoder().encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(globalJwtAdapter, "secretKey", secret);
        globalJwtAdapter.init();

        memberDetail = MemberDetail.builder()
                .memberId(1L)
                .socialId("social-1")
                .provider(SocialProvider.KAKAO)
                .settingId(10L)
                .socialNickname("소셜닉")
                .thumbnailImage("thumb.jpg")
                .memberName("tester")
                .role(MemberRole.USER)
                .authTokenId(99L)
                .fcmTokenId(777L)
                .build();
    }

    @Test
    @DisplayName("액세스 토큰 생성 후 사용자 정보를 역직렬화한다")
    void shouldGenerateAndParseAccessToken() {
        String accessToken = globalJwtAdapter.generateAccessToken(memberDetail);

        assertThat(accessToken).isNotBlank();
        assertThat(globalJwtAdapter.validateToken(accessToken)).isTrue();

        MemberDetail parsed = globalJwtAdapter.getUserInfoFromToken(accessToken);

        assertThat(parsed.getMemberId()).isEqualTo(memberDetail.getMemberId());
        assertThat(parsed.getAuthTokenId()).isEqualTo(memberDetail.getAuthTokenId());
        assertThat(parsed.getMemberName()).isEqualTo(memberDetail.getMemberName());
        assertThat(parsed.getProvider()).isEqualTo(memberDetail.getProvider());
        assertThat(parsed.getFcmTokenId()).isEqualTo(memberDetail.getFcmTokenId());
    }

    @Test
    @DisplayName("리프레시 토큰에서 토큰 ID를 추출한다")
    void shouldGenerateRefreshTokenAndExtractTokenId() {
        String refreshToken = globalJwtAdapter.generateRefreshToken(memberDetail);

        assertThat(refreshToken).isNotBlank();
        assertThat(globalJwtAdapter.getTokenIdFromToken(refreshToken)).isEqualTo(memberDetail.getAuthTokenId());
    }

    @Test
    @DisplayName("만료 임계값 이하의 토큰은 갱신 대상으로 간주한다")
    void shouldRecommendRefreshWhenRemainingDaysBelowThreshold() {
        Key key = (Key) ReflectionTestUtils.getField(globalJwtAdapter, "key");
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

        assertThat(globalJwtAdapter.shouldRefreshToken(shortLivedToken, 15)).isTrue();
        assertThat(globalJwtAdapter.shouldRefreshToken(longLivedToken, 15)).isFalse();
    }

    @Test
    @DisplayName("토큰 해시값은 안정적으로 생성된다")
    void shouldGenerateStableTokenHash() {
        String token = "sample-token";

        String hash1 = globalJwtAdapter.generateTokenHash(token);
        String hash2 = globalJwtAdapter.generateTokenHash(token);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
    }
}
