package jaeik.growfarm.global.security;

import jaeik.growfarm.global.filter.JwtFilter;
import jaeik.growfarm.global.filter.LogFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * <h2>보안 설정</h2>
 * <p>
 * Spring Security를 사용하여 애플리케이션의 보안을 구성하는 클래스입니다.
 * </p>
 * <p>
 * JWT 필터, 로그 필터, 헤더 체크 필터를 설정하고 CORS 정책을 정의합니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Getter
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

        private final JwtFilter jwtFilter;
        private final LogFilter LogFilter;

        private final String url = "http://localhost:3000";

        /**
         * <h3>보안 필터 체인 설정</h3>
         * <p>
         * HTTP 보안 설정을 정의합니다.
         * </p>
         * <ul>
         * <li>CSRF 보호를 위한 쿠키 기반 토큰 저장소 사용</li>
         * <li>CORS 정책 정의</li>
         * <li>폼 로그인 및 HTTP 기본 인증 비활성화</li>
         * <li>세션 관리 정책을 상태 비저장으로 설정</li>
         * <li>URL 패턴에 따른 권한 부여 규칙 설정</li>
         * </ul>
         *
         * @param http HttpSecurity 객체
         * @return SecurityFilterChain 객체
         * @throws Exception 보안 설정 중 발생할 수 있는 예외
         * @author Jaeik
         * @since 1.0.0
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.GET, "/").permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/api/auth/login", "/api/auth/health", "/api/auth/me",
                                                                "/api/auth/signUp")
                                                .permitAll()
                                                .requestMatchers("/api/comment/like").authenticated()
                                                .requestMatchers("/api/comment/**").permitAll()
                                                .requestMatchers("/api/post/like").authenticated()
                                                .requestMatchers("/api/post/**").permitAll()
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/paper/{userName}").permitAll()
                                                .requestMatchers("/api/user/suggestion", "/api/user/username/check")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(LogFilter, UsernamePasswordAuthenticationFilter.class)
                                .headers(headers -> headers
                                                .xssProtection(xss -> xss
                                                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                                .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                                                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives(
                                                                                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;")));

                return http.build();
        }

        /**
         * <h3>CORS 설정</h3>
         * <p>
         * Cross-Origin Resource Sharing(CORS) 정책을 정의합니다.
         * </p>
         * <ul>
         * <li>허용된 오리진: http://localhost:3000</li>
         * <li>허용된 HTTP 메서드: GET, POST, PUT, DELETE, OPTIONS</li>
         * <li>허용된 헤더: 모든 헤더</li>
         * <li>자격 증명 허용</li>
         * </ul>
         *
         * @return CorsConfigurationSource 객체
         * @author Jaeik
         * @since 1.0.0
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(url, url + "/"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}

/**
 * <h2>SPA CSRF 토큰 요청 핸들러</h2>
 * <p>
 * 단일 페이지 애플리케이션(SPA)에서 CSRF 토큰을 처리하는 핸들러입니다.
 * </p>
 * <p>
 * BREACH 공격 보호를 위해 XorCsrfTokenRequestAttributeHandler를 사용합니다.
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
                this.xor.handle(request, response, csrfToken);
                csrfToken.get();
        }

        /**
         * <h3>CSRF 토큰 값 해결</h3>
         * <p>
         * 요청에서 CSRF 토큰 값을 해결합니다.
         * </p>
         * <p>
         * 헤더에 CSRF 토큰이 포함된 경우 plain 핸들러를 사용하고, 그렇지 않은 경우 xor 핸들러를 사용합니다.
         * </p>
         *
         * @param request   HttpServletRequest 객체
         * @param csrfToken CsrfToken 객체
         * @return CSRF 토큰 값
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
                String headerValue = request.getHeader(csrfToken.getHeaderName());
                return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request,
                                csrfToken);
        }
}
