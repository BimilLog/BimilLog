package jaeik.growfarm.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.UserRole;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

/*
 * JWT 토큰 생성 및 검증을 위한 클래스
 * JWT 토큰을 생성하고 검증하는 기능을 제공
 * 수정일 : 2025-05-03
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    public static final String ACCESS_TOKEN_COOKIE = "jwt_access_token";
    public static final String REFRESH_TOKEN_COOKIE = "jwt_refresh_token";

    @Value("${jwt.secret}")
    private String secretKey;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserDTO userDTO) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + 21600000); // 6시간

        return Jwts.builder()
                .setSubject(String.valueOf(userDTO.getUserId()))
                .claim("farmName", userDTO.getFarmName())
                .claim("role", userDTO.getRole().name())
                .claim("kakaoNickname", userDTO.getKakaoNickname())
                .claim("thumbnailImage", userDTO.getThumbnailImage())
                .claim("tokenId", userDTO.getTokenId())
                .claim("kakaoId", userDTO.getKakaoId())
                .claim("settingId", userDTO.getSettingId())
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserDTO userDTO) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + (86400000L * 30)); // 30일 유효기간

        return Jwts.builder()
                .setSubject(String.valueOf(userDTO.getUserId()))
                .claim("tokenId", userDTO.getTokenId()) // 리프레시 토큰도 특정 유저와 연결
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public List<ResponseCookie> getResponseCookies(String jwtAccessToken, String jwtRefreshToken, int maxAge) {
        ResponseCookie accessTokenCookie = ResponseCookie.from(JwtTokenProvider.ACCESS_TOKEN_COOKIE, jwtAccessToken)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .sameSite("Strict")
                .secure(true)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(JwtTokenProvider.REFRESH_TOKEN_COOKIE, jwtRefreshToken)
                .path("/")
                .maxAge(maxAge * 30L)
                .httpOnly(true)
                .sameSite("Strict")
                .secure(true)
                .build();

        return List.of(accessTokenCookie, refreshTokenCookie);
    }

    public List<ResponseCookie> getLogoutCookies() {
        ResponseCookie accessTokenCookie = ResponseCookie.from(JwtTokenProvider.ACCESS_TOKEN_COOKIE, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Strict")
                .secure(true)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(JwtTokenProvider.REFRESH_TOKEN_COOKIE, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Strict")
                .secure(true)
                .build();

        return List.of(accessTokenCookie, refreshTokenCookie);
    }

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

    public UserDTO getUserDTOFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(Long.parseLong(claims.getSubject()));
        userDTO.setFarmName(claims.get("farmName", String.class));
        userDTO.setKakaoNickname(claims.get("kakaoNickname", String.class));
        userDTO.setThumbnailImage(claims.get("thumbnailImage", String.class));
        userDTO.setRole(claims.get("role", String.class).equals("USER") ? UserRole.USER : UserRole.ADMIN);
        userDTO.setTokenId(claims.get("tokenId", Long.class));
        userDTO.setKakaoId(claims.get("kakaoId", Long.class));
        userDTO.setSettingId(claims.get("settingId", Long.class));
        return userDTO;
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
