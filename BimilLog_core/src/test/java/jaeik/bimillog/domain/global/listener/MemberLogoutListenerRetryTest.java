package jaeik.bimillog.domain.global.listener;

import jaeik.bimillog.domain.auth.event.MemberLoggedOutEvent;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>MemberLogoutListener 재시도 테스트</h2>
 * <p>DB/Redis 관련 예외 발생 시 재시도 로직이 정상 동작하는지 검증</p>
 */
@DisplayName("MemberLogoutListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {MemberLogoutListener.class, jaeik.bimillog.infrastructure.config.RetryConfig.class})
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

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("재시도 대상 예외 발생 시 재시도")
    void shouldRetryOnRetryableExceptions(String exceptionName, RuntimeException exception) {
        // Given
        MemberLoggedOutEvent event = new MemberLoggedOutEvent(1L, 100L, SocialProvider.KAKAO);
        willThrow(exception)
                .given(sseService).deleteEmitters(anyLong(), anyLong());

        // When & Then: 예외가 발생하는 것을 확인하면서 실행
        assertThatThrownBy(() -> listener.memberLogout(event))
                .isInstanceOf(exception.getClass());

        // Then: 예외 발생 후 재시도 횟수만큼 호출되었는지 검증
        verify(sseService, times(MAX_ATTEMPTS))
                .deleteEmitters(1L, 100L);
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

        // Then
        verify(sseService, times(3))
                .deleteEmitters(1L, 100L);
    }

    @Test
    @DisplayName("1회 성공 시 재시도 없음")
    void shouldNotRetryOnSuccess() throws Exception {
        // Given
        MemberLoggedOutEvent event = new MemberLoggedOutEvent(1L, 100L, SocialProvider.GOOGLE);
        doNothing().when(sseService).deleteEmitters(anyLong(), anyLong());
        doNothing().when(socialLogoutService).socialLogout(anyLong(), any(SocialProvider.class));
        doNothing().when(authTokenService).deleteTokens(anyLong(), anyLong());

        // When
        listener.memberLogout(event);

        // Then
        verify(sseService, times(1)).deleteEmitters(1L, 100L);
        verify(authTokenService, times(1)).deleteTokens(1L, 100L);
    }
}
