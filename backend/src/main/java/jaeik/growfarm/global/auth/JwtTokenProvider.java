package jaeik.growfarm.global.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.repository.token.TokenJdbcRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * <h2>Jwt 토큰 생성 클래스</h2>
 *
 * <p>JWT 토큰을 생성하고 검증합니다.</p>
 *
 * @author Jaeik
 * @version  1.0.0
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final TokenJdbcRepository tokenJdbcRepository;
    public static final String ACCESS_TOKEN_COOKIE = "jwt_access_token";
    public static final String REFRESH_TOKEN_COOKIE = "jwt_refresh_token";
    private static final int MAX_AGE = 21600;

    @Value("${jwt.secret}")
    private String secretKey;
    private Key key;

    /**
     * <h3>JWT 키 초기화</h3>
     *
     * <p>
     * Base64로 인코딩된 시크릿 키를 디코딩하여 HMAC SHA 키를 생성한다.
     * </p>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * <h3>JWT 토큰 쿠키 생성</h3>
     *
     * <p>Access 토큰과 Refresh 토큰이 담긴 쿠키 리스트를 생성한다</p>
     *
     * @param clientDTO 클라이언트용 DTO
     * @return 응답 쿠키 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    public List<ResponseCookie> generateJwtCookie(ClientDTO clientDTO) {
        return List.of(generateJwtAccessCookie(clientDTO), generateJwtRefreshCookie(clientDTO));
    }

    /**
     * <h3>JWT 액세스 토큰이 담긴 쿠키 생성</h3>
     *
     * <p>사용자 정보를 포함한 JWT 액세스 토큰 쿠키를 생성합니다.</p>
     *
     * @param clientDTO 클라이언트용 DTO
     * @return JWT 액세스 토큰이 담긴 쿠키
     * @author Jaeik
     * @since 1.0.0
     */
    public ResponseCookie generateJwtAccessCookie(ClientDTO clientDTO) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, generateAccessToken(clientDTO))
                .path("/")
                .maxAge(MAX_AGE)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(false)
                .build();
    }

    /**
     * <h3>JWT 리프레시 토큰이 담긴 쿠키 생성</h3>
     *
     * <p>사용자 정보를 포함한 JWT 리프레시 토큰 쿠키를 생성합니다.</p>
     *
     * @param clientDTO 클라이언트용 DTO
     * @return JWT 리프레시 토큰이 담긴 쿠키
     * @author Jaeik
     * @since 1.0.0
     */
    public ResponseCookie generateJwtRefreshCookie(ClientDTO clientDTO) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, generateRefreshToken(clientDTO))
                .path("/")
                .maxAge(MAX_AGE * 120L)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(false)
                .build();
    }

    /**
     * <h3>JWT 액세스 토큰 생성</h3>
     *
     * <p>사용자 정보를 포함한 JWT 액세스 토큰을 생성한다. 유효기간은 6시간이다</p>
     *
     * @param clientDTO 클라이언트용 DTO
     * @return JWT 액세스 토큰
     * @author Jaeik
     * @since 1.0.0
     */
    private String generateAccessToken(ClientDTO clientDTO) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + 21600000);

        return Jwts.builder()
                .setSubject(String.valueOf(clientDTO.getUserId()))
                .claim("tokenId", clientDTO.getTokenId())
                .claim("kakaoId", clientDTO.getKakaoId())
                .claim("fcmTokenId", clientDTO.getFcmTokenId())
                .claim("settingId", clientDTO.getSettingDTO().getSettingId())
                .claim("userName", clientDTO.getUserName())
                .claim("role", clientDTO.getRole().name())
                .claim("kakaoNickname", clientDTO.getKakaoNickname())
                .claim("thumbnailImage", clientDTO.getThumbnailImage())
                .claim("postFeaturedNotification", clientDTO.getSettingDTO().isPostFeaturedNotification())
                .claim("messageNotification", clientDTO.getSettingDTO().isMessageNotification())
                .claim("commentNotification", clientDTO.getSettingDTO().isCommentNotification())
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * <h3>JWT 리프레시 토큰 생성</h3>
     *
     * <p>사용자 ID와 토큰 ID를 포함한 JWT 리프레시 토큰을 생성한다. 유효기간은 30일이다.</p>
     * <p>DB의 JWT토큰을 업데이트한다.</p>
     *
     * @param clientDTO 클라이언트용 DTO
     * @return JWT 리프레시 토큰
     * @author Jaeik
     * @since 1.0.0
     */
    private String generateRefreshToken(ClientDTO clientDTO) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + (21600000L * 120));

        String jwtRefreshToken = Jwts.builder()
                .setSubject(String.valueOf(clientDTO.getTokenId()))
                .claim("fcmTokenId", clientDTO.getFcmTokenId())
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        tokenJdbcRepository.UpdateJwtRefreshToken(clientDTO.getTokenId(), jwtRefreshToken);
        return jwtRefreshToken;
    }

    /**
     * <h3>로그아웃 쿠키 생성</h3>
     *
     * <p>로그아웃 시 사용되는 쿠키를 생성한다. 액세스 토큰과 리프레시 토큰을 모두 삭제합니다.</p>
     *
     * @return 로그아웃 쿠키 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    public List<ResponseCookie> getLogoutCookies() {
        ResponseCookie accessTokenCookie = ResponseCookie.from(JwtTokenProvider.ACCESS_TOKEN_COOKIE, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(false)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(JwtTokenProvider.REFRESH_TOKEN_COOKIE, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(false)
                .build();

        return List.of(accessTokenCookie, refreshTokenCookie);
    }

    /**
     * <h3>JWT 토큰 유효성 검사</h3>
     *
     * <p>주어진 JWT 토큰이 유효한지 검사합니다.</p>
     *
     * @param token JWT 토큰
     * @return boolean 토큰 유효성 여부
     * @author Jaeik
     * @since 1.0.0
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException
                 | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * <h3>JWT 엑세스 토큰에서 사용자 정보 추출</h3>
     *
     * <p>JWT 엑세스 토큰에서 사용자 정보를 추출하여 ClientDTO로 변환합니다.</p>
     *
     * @param jwtAccessToken JWT 엑세스 토큰
     * @return 클라이언트 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    public ClientDTO getUserInfoFromToken(String jwtAccessToken) {
        Claims claims = getClaims(jwtAccessToken);

        SettingDTO settingDTO = new SettingDTO(
                claims.get("settingId", Long.class),
                claims.get("postFeaturedNotification", Boolean.class),
                claims.get("messageNotification", Boolean.class),
                claims.get("commentNotification", Boolean.class));

        return new ClientDTO(
                Long.parseLong(claims.getSubject()),
                claims.get("kakaoId", Long.class),
                claims.get("kakaoNickname", String.class),
                claims.get("thumbnailImage", String.class),
                claims.get("userName", String.class),
                claims.get("role", String.class).equals("USER") ? UserRole.USER : UserRole.ADMIN,
                claims.get("tokenId", Long.class),
                claims.get("fcmTokenId", Long.class),
                settingDTO);
    }

    /**
     * <h3>JWT 리프레시 토큰에서 토큰 ID 추출</h3>
     *
     * <p>JWT 리프레시 토큰에서 토큰 ID를 추출합니다.</p>
     *
     * @param jwtRefreshToken JWT 리프레시 토큰
     * @return 토큰 ID
     * @author Jaeik
     * @since 1.0.0
     */
    public Long getTokenIdFromToken(String jwtRefreshToken) {
        Claims claims = getClaims(jwtRefreshToken);

        return Long.parseLong(claims.getSubject());
    }

    /**
     * <h3>JWT 리프레시 토큰에서 FCM 토큰 ID 추출</h3>
     * <p>JWT 리프레시 토큰에서 FCM 토큰 ID를 추출합니다.</p>
     * @since 1.0.0
     * @author Jaeik
     * @param jwtRefreshToken
     * @return FCM 토큰 ID
     */
    public Long getFcmTokenIdFromToken(String jwtRefreshToken) {
        Claims claims = getClaims(jwtRefreshToken);

        return claims.get("fcmTokenId", Long.class);
    }

    /**
     * <h3>JWT 리프레시 토큰에서 Claims 추출</h3>
     *
     * <p>JWT 리프레시 토큰에서 Claims를 추출합니다.</p>
     *
     * @param jwtRefreshToken JWT 리프레시 토큰
     * @return Claims 객체
     * @author Jaeik
     * @since 1.0.0
     */
    private Claims getClaims(String jwtRefreshToken) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwtRefreshToken)
                .getBody();
    }
}
