package jaeik.growfarm.global.security;

import jaeik.growfarm.global.filter.HeaderCheckFilter;
import jaeik.growfarm.global.filter.JwtFilter;
import jaeik.growfarm.global.filter.LogFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/*
 * 시큐리티 설정 클래스
 * 수정일 : 2025-05-03
 */
@Getter
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final LogFilter LogFilter;
    private final HeaderCheckFilter headerCheckFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
//                .csrf(csrf -> csrf
//                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/board/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/login", "/api/auth/signUp", "/api/auth/health")
                        .permitAll()
                        .requestMatchers("/api/farm/{farmName}").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(headerCheckFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(LogFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        // X-XSS-Protection 헤더 설정 (브라우저 내장 XSS 필터 활성화)
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        // X-Content-Type-Options 헤더 설정 (MIME 스니핑 방지)
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                        // X-Frame-Options 헤더 설정 (클릭재킹 방지)
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        // Content-Security-Policy 헤더 설정 (리소스 로드 제한)
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;")));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://grow-farm.com", "https://grow-farm.com/"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

/**
 * SPA(Single Page Application)에 대한 CSRF 보호를 처리하는 클래스입니다.
 * - BREACH 공격 방어를 위한 XOR 처리
 * - HTTP 요청 헤더와 요청 매개변수에 따른 토큰 확인
 * - 인증 및 로그아웃 후 새 토큰 발급을 위한 지연 로딩 처리
 */
//final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
//    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
//    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();
//
//    @Override
//    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
//        /*
//         * CsrfToken이 응답 본문에 렌더링될 때 BREACH 보호를 제공하기 위해
//         * 항상 XorCsrfTokenRequestAttributeHandler를 사용합니다.
//         */
//        this.xor.handle(request, response, csrfToken);
//        /*
//         * 지연된 토큰을 로드하여 쿠키에 토큰 값을 렌더링합니다.
//         */
//        csrfToken.get();
//    }
//
//    @Override
//    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
//        String headerValue = request.getHeader(csrfToken.getHeaderName());
//        /*
//         * 요청에 헤더 값이 포함된 경우 CsrfTokenRequestAttributeHandler를 사용하여
//         * CsrfToken을 해결합니다. 이것은 SPA가 쿠키를 통해 얻은 원시 CsrfToken 값을
//         * 자동으로 헤더에 포함시킬 때 적용됩니다.
//         *
//         * 다른 모든 경우(예: 요청에 요청 매개변수가 포함된 경우)에는
//         * XorCsrfTokenRequestAttributeHandler를 사용하여 CsrfToken을 해결합니다.
//         * 이것은 서버 측에서 렌더링된 폼에 _csrf 요청 매개변수가 숨겨진 입력으로
//         * 포함되어 있을 때 적용됩니다.
//         */
//        return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request,
//                csrfToken);
//    }
//}
