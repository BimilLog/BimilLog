package jaeik.bimillog.springboot.nodb;

import jaeik.bimillog.domain.auth.event.MemberLoggedOutEvent;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialLogoutService;
import jaeik.bimillog.domain.global.listener.MemberLogoutListener;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.notification.service.SseService;
import jaeik.bimillog.infrastructure.config.AsyncConfig;
import jaeik.bimillog.infrastructure.config.RetryConfig;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>MemberLogoutListener 재시도 테스트</h2>
 * <p>DB/Redis 관련 예외 발생 시 재시도 로직이 정상 동작하는지 검증</p>
 * <p>AsyncConfig를 포함하여 실제 비동기 환경에서 재시도를 검증</p>
 */
@DisplayName("MemberLogoutListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {MemberLogoutListener.class, RetryConfig.class, AsyncConfig.class})
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class MemberLogoutListenerRetryTest {

    @Autowired
    private MemberLogoutListener listener;

    @MockitoBean
    private SocialLogoutService socialLogoutService;

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private AuthTokenService authTokenService;

    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        Mockito.reset(socialLogoutService, sseService, authTokenService);
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("재시도 대상 예외 발생 시 재시도")
    void shouldRetryOnRetryableExceptions(String exceptionName, RuntimeException exception) {
        // Given
        MemberLoggedOutEvent event = new MemberLoggedOutEvent(1L, 100L, SocialProvider.KAKAO);
        willThrow(exception)
                .given(sseService).deleteEmitters(anyLong(), anyLong());

        // When: 비동기로 실행되며 @Recover 메서드가 있으므로 예외가 외부로 전파되지 않음
        listener.memberLogout(event);

        // Then: 비동기 완료 대기 후 재시도 횟수만큼 호출되었는지 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(sseService, times(MAX_ATTEMPTS))
                        .deleteEmitters(1L, 100L));
    }

    private static Stream<Arguments> provideRetryableExceptions() {
        return Stream.of(
                Arguments.of("TransientDataAccessException",
                        new TransientDataAccessException("일시적 DB 오류") {}),
                Arguments.of("DataAccessResourceFailureException",
                        new DataAccessResourceFailureException("DB 리소스 획득 실패")),
                Arguments.of("QueryTimeoutException",
                        new QueryTimeoutException("쿼리 타임아웃")),
                Arguments.of("RedisConnectionFailureException",
                        new RedisConnectionFailureException("Redis 연결 실패"))
        );
    }

    @Test
    @DisplayName("2회 실패 후 3회차에 성공")
    void shouldSucceedAfterTwoFailures() {
        // Given
        MemberLoggedOutEvent event = new MemberLoggedOutEvent(1L, 100L, SocialProvider.NAVER);
        willThrow(new RedisConnectionFailureException("실패"))
                .willThrow(new QueryTimeoutException("타임아웃"))
                .willDoNothing()
                .given(sseService).deleteEmitters(1L, 100L);

        // When
        listener.memberLogout(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(sseService, times(3))
                        .deleteEmitters(1L, 100L));
    }

    @ParameterizedTest(name = "{0} 제공자로 1회 성공 시 재시도 없음")
    @MethodSource("provideSocialProviders")
    @DisplayName("다양한 소셜 제공자에서 1회 성공 시 재시도 없음")
    void shouldNotRetryOnSuccess(String providerName, SocialProvider provider) throws Exception {
        // Given
        MemberLoggedOutEvent event = new MemberLoggedOutEvent(1L, 100L, provider);
        doNothing().when(sseService).deleteEmitters(anyLong(), anyLong());
        doNothing().when(socialLogoutService).socialLogout(anyLong(), any(SocialProvider.class));
        doNothing().when(authTokenService).deleteTokens(anyLong(), anyLong());

        // When
        listener.memberLogout(event);

        // Then: 비동기 완료 대기 - SSE 연결 정리, 소셜 로그아웃, 토큰 삭제 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(sseService, times(1)).deleteEmitters(1L, 100L);
                    verify(socialLogoutService, times(1)).socialLogout(1L, provider);
                    verify(authTokenService, times(1)).deleteTokens(1L, 100L);
                });
    }

    private static Stream<Arguments> provideSocialProviders() {
        return Stream.of(
                Arguments.of("KAKAO", SocialProvider.KAKAO),
                Arguments.of("NAVER", SocialProvider.NAVER),
                Arguments.of("GOOGLE", SocialProvider.GOOGLE)
        );
    }
}
