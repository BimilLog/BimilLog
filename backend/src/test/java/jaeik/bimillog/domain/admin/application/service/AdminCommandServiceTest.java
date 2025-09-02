package jaeik.bimillog.domain.admin.application.service;

import jaeik.bimillog.domain.admin.application.port.in.ReportedUserResolver;
import jaeik.bimillog.domain.admin.application.port.out.AdminCommandPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.admin.event.AdminWithdrawRequestedEvent;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.admin.entity.ReportVO;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>AdminCommandService 단위 테스트</h2>
 * <p>관리자 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCommandService 단위 테스트")
class AdminCommandServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ReportedUserResolver reportedUserResolver;

    @Mock
    private AdminCommandPort adminCommandPort;

    @Mock
    private UserQueryPort userQueryPort;

    @InjectMocks
    private AdminCommandService adminCommandService;

    private ReportVO validReportVO;
    private User testUser;

    @BeforeEach
    void setUp() {
        adminCommandService = new AdminCommandService(
                eventPublisher,
                List.of(reportedUserResolver),
                adminCommandPort,
                userQueryPort
        );

        validReportVO = ReportVO.builder()
                .reportType(ReportType.POST)
                .targetId(200L)
                .content("불쾌한 내용")
                .build();

        testUser = User.builder()
                .id(200L)
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .build();
    }

    @Test
    @DisplayName("유효한 신고 정보로 사용자 제재 시 성공")
    void shouldBanUser_WhenValidReportDTO() {
        // Given
        given(reportedUserResolver.supports()).willReturn(ReportType.POST);
        given(reportedUserResolver.resolve(200L)).willReturn(testUser);

        // When
        adminCommandService.banUser(validReportVO);

        // Then
        ArgumentCaptor<UserBannedEvent> eventCaptor = ArgumentCaptor.forClass(UserBannedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserBannedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getUserId()).isEqualTo(200L);
        assertThat(capturedEvent.getSocialId()).isEqualTo("kakao123");
        assertThat(capturedEvent.getProvider()).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("targetId가 null인 경우 INVALID_REPORT_TARGET 예외 발생")
    void shouldThrowException_WhenTargetIdIsNull() {
        // Given
        ReportVO invalidReport = ReportVO.builder()
                .reportType(ReportType.POST)
                .targetId(null)
                .content("invalid report")
                .build();

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(invalidReport))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REPORT_TARGET);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("사용자가 존재하지 않는 경우 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        given(reportedUserResolver.supports()).willReturn(ReportType.POST);
        given(reportedUserResolver.resolve(200L)).willReturn(null);

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(validReportVO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("지원하지 않는 신고 유형인 경우 INVALID_INPUT_VALUE 예외 발생")
    void shouldThrowException_WhenUnsupportedReportType() {
        // Given
        given(reportedUserResolver.supports()).willReturn(ReportType.COMMENT);

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(validReportVO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("유효한 사용자 ID로 강제 탈퇴 시 이벤트 발행")
    void shouldPublishEvent_WhenValidUserId() {
        // Given
        Long userId = 100L;

        // When
        adminCommandService.forceWithdrawUser(userId);

        // Then
        ArgumentCaptor<AdminWithdrawRequestedEvent> eventCaptor = 
                ArgumentCaptor.forClass(AdminWithdrawRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        AdminWithdrawRequestedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(userId);
        assertThat(capturedEvent.reason()).isEqualTo("관리자 강제 탈퇴");
    }

    @Test
    @DisplayName("null 사용자 ID로 강제 탈퇴 시도 시 예외 발생")
    void shouldThrowException_WhenNullUserId() {
        // Given
        Long userId = null;

        // When & Then
        assertThatThrownBy(() -> adminCommandService.forceWithdrawUser(userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("여러 신고 유형에 대한 ReportedUserResolver 테스트")
    void shouldHandleMultipleReportTypesCorrectly() {
        // Given
        ReportedUserResolver postResolver = createMockResolver(ReportType.POST);
        ReportedUserResolver commentResolver = createMockResolver(ReportType.COMMENT);
        ReportedUserResolver paperResolver = createMockResolver(ReportType.SUGGESTION);

        given(postResolver.resolve(200L)).willReturn(testUser);
        given(commentResolver.resolve(300L)).willReturn(testUser);
        given(paperResolver.resolve(400L)).willReturn(testUser);

        adminCommandService = new AdminCommandService(
                eventPublisher,
                List.of(postResolver, commentResolver, paperResolver),
                adminCommandPort,
                userQueryPort
        );

        ReportVO postReport = ReportVO.builder()
                .reportType(ReportType.POST)
                .targetId(200L)
                .content("부적절한 게시글")
                .build();
        
        ReportVO commentReport = ReportVO.builder()
                .reportType(ReportType.COMMENT)
                .targetId(300L)
                .content("부적절한 댓글")
                .build();
        
        ReportVO paperReport = ReportVO.builder()
                .reportType(ReportType.SUGGESTION)
                .targetId(400L)
                .content("부적절한 메시지")
                .build();

        // When & Then
        adminCommandService.banUser(postReport);
        verify(postResolver).resolve(200L);

        adminCommandService.banUser(commentReport);
        verify(commentResolver).resolve(300L);

        adminCommandService.banUser(paperReport);
        verify(paperResolver).resolve(400L);

        // 이벤트 발행 검증
        verify(eventPublisher, times(3)).publishEvent(any(UserBannedEvent.class));
    }

    @Test
    @DisplayName("여러 ReportedUserResolver 중 올바른 것 선택")
    void shouldSelectCorrectResolver_WhenMultipleResolversExist() {
        // Given
        ReportedUserResolver postResolver = createMockResolver(ReportType.POST);
        ReportedUserResolver commentResolver = createMockResolver(ReportType.COMMENT);
        
        given(postResolver.resolve(200L)).willReturn(testUser);

        adminCommandService = new AdminCommandService(
                eventPublisher,
                List.of(commentResolver, postResolver),
                adminCommandPort,
                userQueryPort
        );

        // When
        adminCommandService.banUser(validReportVO);

        // Then
        verify(postResolver).resolve(200L);
        verify(commentResolver, never()).resolve(any());
    }

    // ========== createReport 메서드 테스트 ==========

    @Test
    @DisplayName("인증된 사용자 신고 생성 - 성공")
    void createReport_AuthenticatedUser_Success() {
        // Given
        Long userId = 1L;
        ReportVO reportVO = ReportVO.of(ReportType.COMMENT, 123L, "부적절한 댓글입니다");
        
        User reporter = User.builder()
                .id(userId)
                .userName("testuser")
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .build();

        Report expectedReport = Report.createReport(reportVO, reporter);

        given(userQueryPort.findById(userId)).willReturn(Optional.of(reporter));
        given(adminCommandPort.save(any(Report.class))).willReturn(expectedReport);

        // When
        adminCommandService.createReport(userId, reportVO);

        // Then
        verify(userQueryPort, times(1)).findById(userId);
        verify(adminCommandPort, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("익명 사용자 신고 생성 - 성공")
    void createReport_AnonymousUser_Success() {
        // Given
        Long userId = null; // 익명 사용자
        ReportVO reportVO = ReportVO.of(ReportType.POST, 456L, "스팸 게시글입니다");
        
        Report expectedReport = Report.createReport(reportVO, null);
        given(adminCommandPort.save(any(Report.class))).willReturn(expectedReport);

        // When
        adminCommandService.createReport(userId, reportVO);

        // Then
        verify(userQueryPort, never()).findById(any()); // 익명 사용자는 조회하지 않음
        verify(adminCommandPort, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("건의사항 생성 - 성공 (targetId null)")
    void createReport_Suggestion_Success() {
        // Given
        Long userId = 2L;
        ReportVO reportVO = ReportVO.of(ReportType.SUGGESTION, null, "새로운 기능을 건의합니다");
        
        User reporter = User.builder()
                .id(userId)
                .userName("suggester")
                .socialId("social456")
                .provider(SocialProvider.KAKAO)
                .build();

        Report expectedReport = Report.createReport(reportVO, reporter);

        given(userQueryPort.findById(userId)).willReturn(Optional.of(reporter));
        given(adminCommandPort.save(any(Report.class))).willReturn(expectedReport);

        // When
        adminCommandService.createReport(userId, reportVO);

        // Then
        verify(userQueryPort, times(1)).findById(userId);
        verify(adminCommandPort, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("신고 생성 - 실패 (reportVO가 null)")
    void createReport_Fail_NullReportVO() {
        // Given
        Long userId = 1L;
        ReportVO reportVO = null;

        // When & Then
        assertThatThrownBy(() -> adminCommandService.createReport(userId, reportVO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        verify(userQueryPort, never()).findById(any());
        verify(adminCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("신고 생성 - 실패 (존재하지 않는 사용자)")
    void createReport_Fail_UserNotFound() {
        // Given
        Long userId = 999L;
        ReportVO reportVO = ReportVO.of(ReportType.COMMENT, 123L, "부적절한 댓글입니다");
        
        given(userQueryPort.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminCommandService.createReport(userId, reportVO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userQueryPort, times(1)).findById(userId);
        verify(adminCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("여러 신고 연속 생성 - 성공")
    void createMultipleReports_Success() {
        // Given
        Long userId1 = 1L;
        Long userId2 = null; // 익명
        Long userId3 = 3L;

        ReportVO reportVO1 = ReportVO.of(ReportType.COMMENT, 100L, "신고 내용 1");
        ReportVO reportVO2 = ReportVO.of(ReportType.POST, 200L, "신고 내용 2");
        ReportVO reportVO3 = ReportVO.of(ReportType.SUGGESTION, null, "건의 내용");

        User user1 = User.builder().id(userId1).userName("user1").build();
        User user3 = User.builder().id(userId3).userName("user3").build();

        given(userQueryPort.findById(userId1)).willReturn(Optional.of(user1));
        given(userQueryPort.findById(userId3)).willReturn(Optional.of(user3));
        given(adminCommandPort.save(any(Report.class))).willReturn(mock(Report.class));

        // When
        adminCommandService.createReport(userId1, reportVO1);
        adminCommandService.createReport(userId2, reportVO2); // 익명
        adminCommandService.createReport(userId3, reportVO3);

        // Then
        verify(userQueryPort, times(1)).findById(userId1);
        verify(userQueryPort, never()).findById(userId2); // 익명은 조회하지 않음
        verify(userQueryPort, times(1)).findById(userId3);
        verify(adminCommandPort, times(3)).save(any(Report.class));
    }

    private ReportedUserResolver createMockResolver(ReportType supportedType) {
        ReportedUserResolver resolver = org.mockito.Mockito.mock(ReportedUserResolver.class);
        given(resolver.supports()).willReturn(supportedType);
        return resolver;
    }
}