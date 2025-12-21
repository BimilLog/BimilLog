package jaeik.bimillog.infrastructure.security;

import jaeik.bimillog.infrastructure.filter.JwtFilter;
import jaeik.bimillog.infrastructure.filter.LogFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
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
 * @version 2.0.0
 */
@Getter
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final LogFilter LogFilter;
    private final boolean COOKIE_SECURE = true;

    @Value("${url}")
    private String url;

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
     * @since 2.0.0
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/**", "GET")) // GET요청 CSRF 제외
                        .csrfTokenRepository(createCsrfTokenRepository())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers("/.well-known/**").permitAll() // TWA Digital Asset Links
                        .requestMatchers("/api/member/search", "/api/member/all").permitAll()
                        .requestMatchers("/api/auth/login", "/api/global/health", "/api/member/signup", "/api/global/client-error").permitAll()
                        .requestMatchers("/api/mypage").authenticated()
                        .requestMatchers("/api/comment/like").authenticated()
                        .requestMatchers("/api/comment/**").permitAll()
                        .requestMatchers("/api/post/{postId}/notice").hasRole("ADMIN")
                        .requestMatchers("/api/post/{postId}/like").authenticated()
                        .requestMatchers("/api/post/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/paper/{userName}", "/api/paper/popular").permitAll()
                        .requestMatchers("/api/member/suggestion", "/api/member/username/check", "/api/member/report").permitAll()
                        .requestMatchers("/actuator/**").permitAll()  // ALB에서 외부 접근 차단됨
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(LogFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self'; " +
                                                "style-src 'self'; " +
                                                "img-src 'self'; " +
                                                "frame-ancestors 'self';"
                                )));
        return http.build();
    }

    // 로컬 테스트를 위해 HTTPS 해제
    /**
     * <h3>CSRF 토큰 쿠키 저장소 설정</h3>
     * <p>
     * CSRF 토큰을 쿠키에 저장하는 저장소를 설정합니다.
     * Secure 설정이 적용되어 HTTPS에서만 전송됩니다.
     * </p>
     *
     * @return CsrfTokenRepository 객체
     * @author Jaeik
     * @since 2.0.0
     */
    private CsrfTokenRepository createCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.secure(COOKIE_SECURE).sameSite("LAX"));
        return repository;
    }

    /**
     * <h3>CORS 설정</h3>
     * <p>
     * Cross-Origin Resource Sharing(CORS) 정책을 정의합니다.
     * </p>
     * <ul>
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
        configuration.setAllowedOrigins(List.of( // 로컬개발용 임시허용
                url
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Location"));
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

    /**
     * <h3>CSRF 토큰 처리</h3>
     * <p>HttpServletRequest와 HttpServletResponse에 CSRF 토큰을 설정합니다.</p>
     *
     * @param request HttpServletRequest 객체
     * @param response HttpServletResponse 객체
     * @param csrfToken CSRF 토큰 공급자
     * @author Jaeik
     * @since 2.0.0
     */
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