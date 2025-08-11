package jaeik.growfarm.global.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.UserRole;
import jaeik.growfarm.dto.user.ClientDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * <h2>JWT 처리 핸들러</h2>
 *
 * <p>JWT 토큰의 생성, 검증, 정보 추출 등 순수한 JWT 관련 로직을 담당합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class JwtHandler {

    @Value("${jwt.secret}")
    private String secretKey;
    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * <h3>JWT 액세스 토큰 생성</h3>
     *
     * <p>사용자 정보를 포함한 JWT 액세스 토큰을 생성한다. 유효기간은 1시간이다</p>
     *
     * @param clientDTO 클라이언트용 DTO
     * @return JWT 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    public String generateAccessToken(ClientDTO clientDTO) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + 3600000);

        return Jwts.builder()
                .setSubject(String.valueOf(clientDTO.getUserId()))
                .claim("tokenId", clientDTO.getTokenId())
                .claim("socialId", clientDTO.getSocialId())
                .claim("provider", clientDTO.getProvider().name())
                .claim("fcmTokenId", clientDTO.getFcmTokenId())
                .claim("settingId", clientDTO.getSettingId())
                .claim("userName", clientDTO.getUserName())
                .claim("role", clientDTO.getRole().name())
                .claim("socialNickname", clientDTO.getSocialNickname())
                .claim("thumbnailImage", clientDTO.getThumbnailImage())
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * <h3>JWT 리프레시 토큰 생성</h3>
     *
     * <p>사용자 ID와 토큰 ID를 포함한 JWT 리프레시 토큰을 생성한다. 유효기간은 30일이다.</p>
     *
     * @param clientDTO 클라이언트용 DTO
     * @return JWT 리프레시 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    public String generateRefreshToken(ClientDTO clientDTO) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + (3600000L * 720));

        return Jwts.builder()
                .setSubject(String.valueOf(clientDTO.getTokenId()))
                .claim("fcmTokenId", clientDTO.getFcmTokenId())
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * <h3>JWT 토큰 유효성 검사</h3>
     *
     * <p>주어진 JWT 토큰이 유효한지 검사합니다.</p>
     *
     * @param token JWT 토큰
     * @return boolean 토큰 유효성 여부
     * @author Jaeik
     * @since 2.0.0
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
     * @since 2.0.0
     */
    public ClientDTO getUserInfoFromToken(String jwtAccessToken) {
        Claims claims = getClaims(jwtAccessToken);

        return new ClientDTO(
                Long.parseLong(claims.getSubject()),
                claims.get("socialId", String.class),
                SocialProvider.valueOf(claims.get("provider", String.class)),
                claims.get("socialNickname", String.class),
                claims.get("thumbnailImage", String.class),
                claims.get("userName", String.class),
                UserRole.valueOf(claims.get("role", String.class)),
                claims.get("tokenId", Long.class),
                claims.get("fcmTokenId", Long.class),
                claims.get("settingId", Long.class));
    }

    /**
     * <h3>JWT 리프레시 토큰에서 토큰 ID 추출</h3>
     *
     * <p>JWT 리프레시 토큰에서 토큰 ID를 추출합니다.</p>
     *
     * @param jwtRefreshToken JWT 리프레시 토큰
     * @return 토큰 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public Long getTokenIdFromToken(String jwtRefreshToken) {
        Claims claims = getClaims(jwtRefreshToken);

        return Long.parseLong(claims.getSubject());
    }

    /**
     * <h3>JWT 리프레시 토큰에서 FCM 토큰 ID 추출</h3>
     * <p>JWT 리프레시 토큰에서 FCM 토큰 ID를 추출합니다.</p>
     * @since 2.0.0
     * @author Jaeik
     * @param jwtRefreshToken JWT 리프레시 토큰
     * @return FCM 토큰 ID
     */
    public Long getFcmTokenIdFromToken(String jwtRefreshToken) {
        Claims claims = getClaims(jwtRefreshToken);

        return claims.get("fcmTokenId", Long.class);
    }

    /**
     * <h3>JWT 토큰에서 Claims 추출</h3>
     *
     * <p>JWT 토큰에서 Claims를 추출합니다.</p>
     *
     * @param token JWT 토큰
     * @return Claims 객체
     * @author Jaeik
     * @since 2.0.0
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

