package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.admin.application.port.in.ReportedUserResolver;
import jaeik.growfarm.domain.admin.application.port.out.AdminCommandPort;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.admin.event.UserBannedEvent;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
    private AdminCommandPort adminCommandPort;

    @Mock
    private ReportedUserResolver reportedUserResolver;

    @InjectMocks
    private AdminCommandService adminCommandService;

    private ReportDTO validReportDTO;
    private User testUser;

    @BeforeEach
    void setUp() {
        adminCommandService = new AdminCommandService(
                eventPublisher,
                List.of(reportedUserResolver),
                adminCommandPort
        );

        validReportDTO = ReportDTO.builder()
                .id(1L)
                .reporterId(100L)
                .reporterName("reporter")
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
        adminCommandService.banUser(validReportDTO);

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
        ReportDTO invalidReport = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(null)
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
        assertThatThrownBy(() -> adminCommandService.banUser(validReportDTO))
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
        assertThatThrownBy(() -> adminCommandService.banUser(validReportDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("유효한 사용자 ID로 강제 탈퇴 시 성공")
    void shouldForceWithdrawUser_WhenValidUserId() {
        // Given
        Long userId = 100L;

        // When
        adminCommandService.forceWithdrawUser(userId);

        // Then
        verify(adminCommandPort).forceWithdraw(userId);
    }

    @Test
    @DisplayName("null 사용자 ID로 강제 탈퇴 시도 시 처리")
    void shouldHandleNullUserId_WhenForceWithdraw() {
        // Given
        Long userId = null;

        // When
        adminCommandService.forceWithdrawUser(userId);

        // Then
        verify(adminCommandPort).forceWithdraw(userId);
    }

    @Test
    @DisplayName("여러 신고 유형에 대한 ReportedUserResolver 테스트")
    void shouldHandleMultipleReportTypesCorrectly() {
        // Given
        ReportedUserResolver postResolver = createMockResolver(ReportType.POST);
        ReportedUserResolver commentResolver = createMockResolver(ReportType.COMMENT);
        ReportedUserResolver paperResolver = createMockResolver(ReportType.PAPER);

        given(postResolver.resolve(200L)).willReturn(testUser);
        given(commentResolver.resolve(300L)).willReturn(testUser);
        given(paperResolver.resolve(400L)).willReturn(testUser);

        adminCommandService = new AdminCommandService(
                eventPublisher,
                List.of(postResolver, commentResolver, paperResolver),
                adminCommandPort
        );

        ReportDTO postReport = ReportDTO.builder()
                .id(1L)
                .reporterId(100L)
                .reporterName("reporter")
                .reportType(ReportType.POST)
                .targetId(200L)
                .content("부적절한 게시글")
                .build();
        
        ReportDTO commentReport = ReportDTO.builder()
                .id(2L)
                .reporterId(100L)
                .reporterName("reporter")
                .reportType(ReportType.COMMENT)
                .targetId(300L)
                .content("부적절한 댓글")
                .build();
        
        ReportDTO paperReport = ReportDTO.builder()
                .id(3L)
                .reporterId(100L)
                .reporterName("reporter")
                .reportType(ReportType.PAPER)
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
                adminCommandPort
        );

        // When
        adminCommandService.banUser(validReportDTO);

        // Then
        verify(postResolver).resolve(200L);
        verify(commentResolver, never()).resolve(any());
    }

    private ReportedUserResolver createMockResolver(ReportType supportedType) {
        ReportedUserResolver resolver = org.mockito.Mockito.mock(ReportedUserResolver.class);
        given(resolver.supports()).willReturn(supportedType);
        return resolver;
    }
}