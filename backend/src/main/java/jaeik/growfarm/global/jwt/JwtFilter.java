package jaeik.growfarm.global.jwt;

import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.service.KakaoService;
import jaeik.growfarm.util.UserUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
 * JWT 필터
 * JWT 토큰을 검증하고 인증 정보를 설정하는 필터
 * 수정일 : 2025-05-03
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRepository tokenRepository;
    private final UserUtil userUtil;
    private final KakaoService kakaoService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // accessToken 추출 및 검증
        String accessToken = extractTokenFromCookie(request, JwtTokenProvider.ACCESS_TOKEN_COOKIE);
        // accessToken이 유효하면 인증 정보 설정
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            try {
                setAuthentication(accessToken);
            } catch (Exception e) {
                logger.error("Access Token으로 인증 설정 중 오류 발생", e);
            }
        }
        // accessToken이 없거나 유효하지 않으면 refreshToken 확인
        else {
            String refreshToken = extractTokenFromCookie(request, JwtTokenProvider.REFRESH_TOKEN_COOKIE);

            // refreshToken이 유효하면 accessToken 재발급
            if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
                Token token = tokenRepository.findByJwtRefreshToken(refreshToken);
                // DB에서 refreshToken 검증
                if (token != null) {
                    try {
                        // 카카오 토큰 갱신 로직
                        tokenRepository.save(userUtil.DTOToToken(kakaoService.refreshToken(token.getKakaoAccessToken())));

                        Long tokenId = jwtTokenProvider.getClaimsFromToken(refreshToken).get("tokenId", Long.class);

                        // tokenId로 완전한 사용자 정보 조회
                        UserDTO userDTO = userUtil.getUserDTOByTokenId(tokenId);

                        // 새로운 accessToken 발급
                        String newAccessToken = jwtTokenProvider.generateAccessToken(userDTO);

                        // 새로운 accessToken을 쿠키에 설정
                        ResponseCookie cookie = ResponseCookie
                                .from(JwtTokenProvider.ACCESS_TOKEN_COOKIE, newAccessToken)
                                .path("/")
                                .maxAge(86400)
                                .httpOnly(true)
                                .sameSite("Lax")
                                .build();
                        response.addHeader("Set-Cookie", cookie.toString());

                        // 사용자 인증 정보 설정
                        setAuthentication(newAccessToken);
                    } catch (Exception e) {
                        logger.error("Refresh Token 처리 중 오류 발생", e);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    // 인증 정보 설정 메서드
    private void setAuthentication(String token) {
        UserDTO userDTO = jwtTokenProvider.getUserDTOFromToken(token);
        CustomUserDetails customUserDetails = new CustomUserDetails(userDTO);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // 특정 이름의 쿠키 값을 추출하는 메서드
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}