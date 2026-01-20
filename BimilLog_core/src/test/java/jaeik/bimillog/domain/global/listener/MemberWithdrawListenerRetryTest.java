package jaeik.bimillog.domain.global.listener;

import jaeik.bimillog.domain.admin.service.AdminCommandService;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialTokenService;
import jaeik.bimillog.domain.auth.service.SocialWithdrawService;
import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.event.MemberWithdrawnEvent;
import jaeik.bimillog.domain.member.service.MemberAccountService;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.domain.notification.service.SseService;
import jaeik.bimillog.domain.paper.service.PaperCommandService;
import jaeik.bimillog.domain.post.service.PostCommandService;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
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
 * <h2>MemberWithdrawListener 재시도 테스트</h2>
 * <p>DB/Redis 관련 예외 발생 시 재시도 로직이 정상 동작하는지 검증</p>
 */
@DisplayName("MemberWithdrawListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {MemberWithdrawListener.class, jaeik.bimillog.infrastructure.config.RetryConfig.class})
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class MemberWithdrawListenerRetryTest {

    @Autowired
    private MemberWithdrawListener listener;

    @MockitoBean
    private SocialWithdrawService socialWithdrawService;

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private NotificationCommandService notificationCommandUseCase;

    @MockitoBean
    private CommentCommandService commentCommandService;

    @MockitoBean
    private PostCommandService postCommandService;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private PaperCommandService paperCommandService;

    @MockitoBean
    private AdminCommandService adminCommandService;

    @MockitoBean
    private MemberAccountService memberAccountService;

    @MockitoBean
    private SocialTokenService socialTokenService;

    @MockitoBean
    private RedisInteractionScoreRepository redisInteractionScoreRepository;

    @MockitoBean
    private RedisFriendshipRepository redisFriendshipRepository;

    private static final int MAX_ATTEMPTS = 3;

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("재시도 대상 예외 발생 시 재시도")
    void shouldRetryOnRetryableExceptions(String exceptionName, RuntimeException exception) {
        // Given
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(1L, "social123", SocialProvider.KAKAO);
        willThrow(exception)
                .given(sseService).deleteEmitters(anyLong(), any());

        // When & Then: 예외가 발생하는 것을 확인하면서 실행
        assertThatThrownBy(() -> listener.memberWithdraw(event))
                .isInstanceOf(exception.getClass());

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
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(1L, "social123", SocialProvider.NAVER);
        willThrow(new RedisConnectionFailureException("실패"))
                .willThrow(new DataAccessResourceFailureException("실패"))
                .willDoNothing()
                .given(sseService).deleteEmitters(1L, null);

        // When
        listener.memberWithdraw(event);

        // Then
        verify(sseService, times(3))
                .deleteEmitters(1L, null);
    }

    @Test
    @DisplayName("1회 성공 시 모든 서비스가 정상 호출됨")
    void shouldCallAllServicesOnSuccess() {
        // Given
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(1L, "social123", SocialProvider.GOOGLE);

        // When
        listener.memberWithdraw(event);

        // Then
        verify(sseService, times(1)).deleteEmitters(1L, null);
        verify(postCommandService, times(1)).deleteAllPostsByMemberId(1L);
        verify(commentCommandService, times(1)).processUserCommentsOnWithdrawal(1L);
        verify(paperCommandService, times(1)).deleteMessageInMyPaper(1L, null);
        verify(notificationCommandUseCase, times(1)).deleteAllNotification(1L);
        verify(authTokenService, times(1)).deleteTokens(1L, null);
        verify(adminCommandService, times(1)).anonymizeReporterByUserId(1L);
        verify(socialTokenService, times(1)).deleteByMemberId(1L);
        verify(memberAccountService, times(1)).removeMemberAccount(1L);
    }
}
