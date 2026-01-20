package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.domain.post.service.PostInteractionService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;


/**
 * <h2>PostViewIncreaseListener 재시도 테스트</h2>
 * <p>DB 관련 예외 발생 시 재시도 로직이 정상 동작하는지 검증</p>
 * <p>AsyncConfig를 포함하여 실제 비동기 환경에서 재시도를 검증</p>
 */
@DisplayName("PostViewIncreaseListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {PostViewIncreaseListener.class, RetryConfig.class, AsyncConfig.class})
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class PostViewIncreaseListenerRetryTest {

    @Autowired
    private PostViewIncreaseListener listener;

    @MockitoBean
    private PostInteractionService postInteractionService;

    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        Mockito.reset(postInteractionService);
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("DB 관련 예외 발생 시 재시도")
    void shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        // Given
        PostViewedEvent event = new PostViewedEvent(1L);
        willThrow(exception)
                .given(postInteractionService).incrementViewCount(anyLong());

        // When: 비동기로 실행되며 @Recover 메서드가 있으므로 예외가 외부로 전파되지 않음
        listener.handlePostViewedEvent(event);

        // Then: 비동기 완료 대기 후 재시도 횟수만큼 호출되었는지 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(postInteractionService, times(MAX_ATTEMPTS))
                        .incrementViewCount(1L));
    }

    private static Stream<Arguments> provideRetryableExceptions() {
        return Stream.of(
                Arguments.of("TransientDataAccessException",
                        new TransientDataAccessException("일시적 DB 오류") {}),
                Arguments.of("DataAccessResourceFailureException",
                        new DataAccessResourceFailureException("DB 리소스 획득 실패")),
                Arguments.of("QueryTimeoutException",
                        new QueryTimeoutException("쿼리 타임아웃"))
        );
    }

    @Test
    @DisplayName("2회 실패 후 3회차에 성공")
    void shouldSucceedAfterTwoFailures() {
        // Given
        PostViewedEvent event = new PostViewedEvent(100L);
        willThrow(new DataAccessResourceFailureException("실패"))
                .willThrow(new QueryTimeoutException("타임아웃"))
                .willDoNothing()
                .given(postInteractionService).incrementViewCount(100L);

        // When
        listener.handlePostViewedEvent(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(postInteractionService, times(3))
                        .incrementViewCount(100L));
    }

    @Test
    @DisplayName("1회 성공 시 재시도 없음")
    void shouldNotRetryOnSuccess() {
        // Given
        PostViewedEvent event = new PostViewedEvent(1L);
        doNothing().when(postInteractionService).incrementViewCount(anyLong());

        // When
        listener.handlePostViewedEvent(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(postInteractionService, times(1))
                        .incrementViewCount(1L));
    }
}
