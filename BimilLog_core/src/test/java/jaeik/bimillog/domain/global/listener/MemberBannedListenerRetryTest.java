package jaeik.bimillog.domain.global.listener;

import jaeik.bimillog.domain.admin.event.MemberBannedEvent;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialLogoutService;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.notification.service.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>MemberBannedListener 재시도 테스트</h2>
 * <p>DB/Redis 관련 예외 발생 시 재시도 로직이 정상 동작하는지 검증</p>
 */
@DisplayName("MemberBannedListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {MemberBannedListener.class, jaeik.bimillog.infrastructure.config.RetryConfig.class})
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class MemberBannedListenerRetryTest {

    @Autowired
    private MemberBannedListener listener;

    @MockitoBean
    private SocialLogoutService socialLogoutService;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private SseService sseService;

    private static final int MAX_ATTEMPTS = 3;

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("재시도 대상 예외 발생 시 재시도")
    void shouldRetryOnRetryableExceptions(String exceptionName, RuntimeException exception) {
        // Given
        MemberBannedEvent event = new MemberBannedEvent(1L, "social123", SocialProvider.KAKAO);
        willThrow(exception)
                .given(sseService).deleteEmitters(anyLong(), any());

        // When & Then: 예외가 발생하는 것을 확인하면서 실행
        assertThatThrownBy(() -> listener.memberBanned(event)).isSameAs(exception);

        // Then: 예외 발생 후 재시도 횟수만큼 호출되었는지 검증
        verify(sseService, times(MAX_ATTEMPTS))
                .deleteEmitters(1L, null);
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
        MemberBannedEvent event = new MemberBannedEvent(1L, "social123", SocialProvider.NAVER);
        willThrow(new RedisConnectionFailureException("실패"))
                .willThrow(new DataAccessResourceFailureException("실패"))
                .willDoNothing()
                .given(sseService).deleteEmitters(1L, null);

        // When
        listener.memberBanned(event);

        // Then
        verify(sseService, times(3))
                .deleteEmitters(1L, null);
    }

    @ParameterizedTest(name = "{0} 제공자로 1회 성공 시 재시도 없음")
    @MethodSource("provideSocialProviders")
    @DisplayName("다양한 소셜 제공자에서 1회 성공 시 재시도 없음")
    void shouldNotRetryOnSuccess(String providerName, SocialProvider provider) {
        // Given
        MemberBannedEvent event = new MemberBannedEvent(1L, "social123", provider);
        doNothing().when(sseService).deleteEmitters(anyLong(), any());
        doNothing().when(socialLogoutService).forceLogout(anyString(), any(SocialProvider.class));
        doNothing().when(authTokenService).deleteTokens(anyLong(), any());

        // When
        listener.memberBanned(event);

        // Then: SSE 연결 정리, 소셜 로그아웃, 토큰 무효화 검증
        verify(sseService, times(1)).deleteEmitters(1L, null);
        verify(socialLogoutService, times(1)).forceLogout("social123", provider);
        verify(authTokenService, times(1)).deleteTokens(1L, null);
    }

    private static Stream<Arguments> provideSocialProviders() {
        return Stream.of(
                Arguments.of("KAKAO", SocialProvider.KAKAO),
                Arguments.of("NAVER", SocialProvider.NAVER),
                Arguments.of("GOOGLE", SocialProvider.GOOGLE)
        );
    }
}
