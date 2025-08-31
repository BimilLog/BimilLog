package jaeik.bimillog.infrastructure.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jaeik.bimillog.domain.auth.application.port.out.AuthPort;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.user.out.social.dto.UserDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
public class JwtHandler implements AuthPort {

    @Value("${jwt.secret}")
    private String secretKey;
    private Key key;

    /**
     * <h3>초기화 메소드</h3>
     * <p>JWT 비밀 키를 디코딩하여 서명 키로 사용될 Key 객체를 초기화합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
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
     * @param userDTO 클라이언트용 DTO
     * @return JWT 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    public String generateAccessToken(UserDTO userDTO) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + 3600000);

        return Jwts.builder()
                .setSubject(String.valueOf(userDTO.getUserId()))
                .claim("tokenId", userDTO.getTokenId())
                .claim("socialId", userDTO.getSocialId())
                .claim("provider", userDTO.getProvider().name())
                .claim("settingId", userDTO.getSettingId())
                .claim("userName", userDTO.getUserName())
                .claim("role", userDTO.getRole().name())
                .claim("socialNickname", userDTO.getSocialNickname())
                .claim("thumbnailImage", userDTO.getThumbnailImage())
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
     * @param userDTO 클라이언트용 DTO
     * @return JWT 리프레시 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    public String generateRefreshToken(UserDTO userDTO) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + (3600000L * 720));

        return Jwts.builder()
                .setSubject(String.valueOf(userDTO.getTokenId()))
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
    public UserDTO getUserInfoFromToken(String jwtAccessToken) {
        Claims claims = getClaims(jwtAccessToken);

        return UserDTO.builder()
                .userId(Long.parseLong(claims.getSubject()))
                .socialId(claims.get("socialId", String.class))
                .provider(SocialProvider.valueOf(claims.get("provider", String.class)))
                .socialNickname(claims.get("socialNickname", String.class))
                .thumbnailImage(claims.get("thumbnailImage", String.class))
                .userName(claims.get("userName", String.class))
                .role(UserRole.valueOf(claims.get("role", String.class)))
                .tokenId(claims.get("tokenId", Long.class))
                .fcmTokenId(claims.get("fcmTokenId", Long.class))
                .settingId(claims.get("settingId", Long.class))
                .build();
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
     * <h3>리프레시 토큰 자동 갱신 여부 확인</h3>
     *
     * <p>리프레시 토큰의 남은 만료 시간이 지정된 임계값(일수) 이하인지 확인합니다.</p>
     * <p>15일 이하로 남았을 때 true를 반환하여 새로운 리프레시 토큰 발급을 유도합니다.</p>
     *
     * @param token JWT 리프레시 토큰
     * @param thresholdDays 임계값(일수)
     * @return 갱신이 필요한 경우 true, 그렇지 않은 경우 false
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean shouldRefreshToken(String token, long thresholdDays) {
        try {
            Claims claims = getClaims(token);
            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();
            long remainingTime = expiration.getTime() - now;
            long thresholdMillis = thresholdDays * 24 * 3600000L; // 일수를 밀리초로 변환
            
            return remainingTime <= thresholdMillis;
        } catch (Exception e) {
            return false;
        }
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

    /**
     * <h3>JWT 토큰 해시값 생성</h3>
     *
     * <p>JWT 토큰을 SHA-256으로 해시하여 블랙리스트 키로 사용할 해시값을 생성합니다.</p>
     * <p>전체 토큰을 저장하지 않고 해시값만 저장하여 보안성을 향상시킵니다.</p>
     *
     * @param token JWT 토큰
     * @return SHA-256 해시값 (Hex 문자열)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String generateTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * <h3>JWT 토큰 만료 시간 추출</h3>
     *
     * <p>JWT 토큰에서 만료 시간을 추출합니다. 블랙리스트 TTL 설정에 사용됩니다.</p>
     *
     * @param token JWT 토큰
     * @return 토큰 만료 시간 (Date 객체)
     * @author Jaeik
     * @since 2.0.0
     */
    public Date getTokenExpiration(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }
}

