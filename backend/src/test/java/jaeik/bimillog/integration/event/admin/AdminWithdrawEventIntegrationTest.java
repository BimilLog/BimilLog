package jaeik.bimillog.integration.event.admin;

import jaeik.bimillog.domain.admin.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.auth.application.port.in.UserBanUseCase;
import jaeik.bimillog.domain.user.application.port.in.WithdrawUseCase;
import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;


@SpringBootTest
@Import(TestContainersConfiguration.class)
@Testcontainers
@Transactional
@DisplayName("관리자 강제 탈퇴 요청 이벤트 워크플로우 통합 테스트")
class AdminWithdrawEventIntegrationTest {


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private WithdrawUseCase withdrawUseCase;

    @MockitoBean
    private UserBanUseCase userBanUseCase;

    @MockitoBean
    private CommentCommandUseCase commentCommandUseCase;

    @Test
    @DisplayName("관리자 강제 탈퇴 요청 이벤트 워크플로우 - 모든 후속 처리 완료")
    void adminWithdrawRequestedEventWorkflow_ShouldCompleteAllProcessing() {
        // Given
        Long userId = 1L;
        String reason = "관리자 강제 탈퇴";
        AdminWithdrawEvent event = new AdminWithdrawEvent(userId, reason);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // 사용자 블랙리스트 등록
                    verify(withdrawUseCase).addToBlacklist(eq(userId));
                    // JWT 토큰 무효화
                    verify(userBanUseCase).blacklistAllUserTokens(eq(userId), eq("관리자 강제 탈퇴"));
                    // 댓글 처리
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId));
                    // 강제 탈퇴 처리
                    verify(withdrawUseCase).forceWithdraw(eq(userId));
                });
    }

    @Test
    @DisplayName("다중 관리자 강제 탈퇴 요청 이벤트 동시 처리")
    void multipleAdminWithdrawRequestedEvents_ShouldProcessConcurrently() {
        // Given
        AdminWithdrawEvent event1 = new AdminWithdrawEvent(1L, "스팸 행위");
        AdminWithdrawEvent event2 = new AdminWithdrawEvent(2L, "지속적 규칙 위반");
        AdminWithdrawEvent event3 = new AdminWithdrawEvent(3L, "부적절한 컸텐츠 게시");

        // When - 동시에 여러 강제 탈퇴 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 사용자의 후속 처리가 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(withdrawUseCase).addToBlacklist(eq(1L));
                    verify(withdrawUseCase).addToBlacklist(eq(2L));
                    verify(withdrawUseCase).addToBlacklist(eq(3L));
                    
                    verify(userBanUseCase).blacklistAllUserTokens(eq(1L), eq("관리자 강제 탈퇴"));
                    verify(userBanUseCase).blacklistAllUserTokens(eq(2L), eq("관리자 강제 탈퇴"));
                    verify(userBanUseCase).blacklistAllUserTokens(eq(3L), eq("관리자 강제 탈퇴"));
                    
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(1L));
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(2L));
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(3L));
                    
                    verify(withdrawUseCase).forceWithdraw(eq(1L));
                    verify(withdrawUseCase).forceWithdraw(eq(2L));
                    verify(withdrawUseCase).forceWithdraw(eq(3L));
                });
    }

    @Test
    @DisplayName("동일 사용자의 여러 강제 탈퇴 요청 이벤트 처리")
    void multipleAdminWithdrawRequestedEventsForSameUser_ShouldProcessAll() {
        // Given - 동일 사용자에 대한 여러 강제 탈퇴 요청 (동시 처리 등의 시나리오)
        Long userId = 1L;
        String reason1 = "첫 번째 사유";
        String reason2 = "두 번째 사유";
        String reason3 = "세 번째 사유";

        // When - 동일 사용자에 대한 강제 탈퇴 이벤트 여러 번 발행
        eventPublisher.publishEvent(new AdminWithdrawEvent(userId, reason1));
        eventPublisher.publishEvent(new AdminWithdrawEvent(userId, reason2));
        eventPublisher.publishEvent(new AdminWithdrawEvent(userId, reason3));

        // Then - 모든 이벤트가 처리되어야 함 (중복 호출 가능)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(withdrawUseCase, times(3)).addToBlacklist(eq(userId));
                    verify(userBanUseCase, times(3)).blacklistAllUserTokens(eq(userId), eq("관리자 강제 탈퇴"));
                    verify(commentCommandUseCase, times(3)).processUserCommentsOnWithdrawal(eq(userId));
                    verify(withdrawUseCase, times(3)).forceWithdraw(eq(userId));
                });
    }


    @Test
    @DisplayName("잘못된 이벤트 데이터 처리 - null userId")
    void adminWithdrawRequestedEventsWithInvalidData_ShouldThrowException() {
        // Given - 잘못된 데이터로 이벤트 생성 시 예외 발생 확인
        // AdminWithdrawRequestedEvent는 생성자에서 검증하므로 null userId 시 예외 발생
        
        // When & Then - 예외가 발생해야 함
        assertThatThrownBy(() -> new AdminWithdrawEvent(null, "테스트 사유"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자 ID는 null일 수 없습니다");
    }


    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - 댓글 처리 실패")
    void eventProcessingWithException_CommentProcessingFailure() {
        // Given
        Long userId = 1L;
        String reason = "예외 테스트";
        AdminWithdrawEvent event = new AdminWithdrawEvent(userId, reason);
        
        // 댓글 처리 실패 시뮬레이션
        doThrow(new RuntimeException("댓글 처리 실패"))
                .when(commentCommandUseCase).processUserCommentsOnWithdrawal(userId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 다른 리스너들은 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(withdrawUseCase).addToBlacklist(eq(userId));
                    verify(userBanUseCase).blacklistAllUserTokens(eq(userId), eq("관리자 강제 탈퇴"));
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId));
                    verify(withdrawUseCase).forceWithdraw(eq(userId));
                });
    }

    @Test
    @DisplayName("빈 사유 문자열 처리 - 디폴트 사유로 대체")
    void adminWithdrawRequestedEvent_EmptyReasonHandling() {
        // Given - 빈 사유 문자열로 이벤트 생성
        Long userId = 1L;
        AdminWithdrawEvent event1 = new AdminWithdrawEvent(userId, "");
        AdminWithdrawEvent event2 = new AdminWithdrawEvent(userId, null);
        AdminWithdrawEvent event3 = new AdminWithdrawEvent(userId, "   ");

        // When - 빈 사유로 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 디폴트 사유로 정상 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(withdrawUseCase, times(3)).addToBlacklist(eq(userId));
                    verify(userBanUseCase, times(3)).blacklistAllUserTokens(eq(userId), eq("관리자 강제 탈퇴"));
                    verify(commentCommandUseCase, times(3)).processUserCommentsOnWithdrawal(eq(userId));
                    verify(withdrawUseCase, times(3)).forceWithdraw(eq(userId));
                });
    }
}