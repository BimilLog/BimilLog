package jaeik.bimillog.domain.admin.service;

import jaeik.bimillog.domain.admin.application.port.out.AdminCommandPort;
import jaeik.bimillog.domain.admin.application.service.AdminCommandService;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.admin.exception.AdminCustomException;
import jaeik.bimillog.domain.admin.exception.AdminErrorCode;
import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.PostTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>AdminCommandService 단위 테스트</h2>
 * <p>관리자 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("AdminCommandService 단위 테스트")
class AdminCommandServiceTest extends BaseUnitTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AdminCommandPort adminCommandPort;

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private PostQueryUseCase postQueryUseCase;

    @Mock
    private CommentQueryUseCase commentQueryUseCase;

    @InjectMocks
    private AdminCommandService adminCommandService;

    private ReportType testReportType = ReportType.POST;
    private Long testTargetId = 200L;
    private String testContent = "불쾌한 내용";

    @BeforeEach
    void setUp() {
        adminCommandService = new AdminCommandService(
                eventPublisher,
                adminCommandPort,
                userQueryPort,
                postQueryUseCase,
                commentQueryUseCase
        );
    }

    @Test
    @DisplayName("유효한 신고 정보로 사용자 제재 시 성공")
    void shouldBanUser_WhenValidReportDTO() {
        // Given
        User userWithId = createTestUserWithId(200L);
        Post testPost = PostTestDataBuilder.withId(200L, PostTestDataBuilder.createPost(userWithId, "테스트 제목", "테스트 내용"));
        given(postQueryUseCase.findById(200L)).willReturn(testPost);

        // When
        adminCommandService.banUser(testReportType, testTargetId);

        // Then
        ArgumentCaptor<UserBannedEvent> eventCaptor = ArgumentCaptor.forClass(UserBannedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserBannedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(200L);
        assertThat(capturedEvent.socialId()).isEqualTo(userWithId.getSocialId());
        assertThat(capturedEvent.provider()).isEqualTo(userWithId.getProvider());
    }

    @Test
    @DisplayName("targetId가 null인 경우 NullPointerException 발생")
    void shouldThrowException_WhenTargetIdIsNull() {
        // Given
        ReportType invalidReportType = ReportType.POST;
        Long invalidTargetId = null;

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(invalidReportType, invalidTargetId))
                .isInstanceOf(NullPointerException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("게시글이 존재하지 않는 경우 POST_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenPostNotFound() {
        // Given
        given(postQueryUseCase.findById(200L)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(testReportType, testTargetId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("댓글 신고의 경우 정상 처리")
    void shouldBanUser_WhenValidCommentReport() {
        // Given
        ReportType commentReportType = ReportType.COMMENT;
        Long commentTargetId = 300L;
        
        User userWithId = createTestUserWithId(200L);
        Comment testComment = Comment.builder()
                .id(300L)
                .user(userWithId)
                .build();
        given(commentQueryUseCase.findById(300L)).willReturn(testComment);

        // When
        adminCommandService.banUser(commentReportType, commentTargetId);

        // Then
        ArgumentCaptor<UserBannedEvent> eventCaptor = ArgumentCaptor.forClass(UserBannedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserBannedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(200L);
        assertThat(capturedEvent.socialId()).isEqualTo(userWithId.getSocialId());
        assertThat(capturedEvent.provider()).isEqualTo(userWithId.getProvider());
    }

    @Test
    @DisplayName("유효한 댓글 신고로 강제 탈퇴 시 이벤트 발행")
    void shouldPublishEvent_WhenValidCommentReport() {
        // Given
        Long commentId = 1L;
        Long userId = 100L;
        ReportType reportType = ReportType.COMMENT;
        
        User mockUser = createTestUserWithId(userId);
        Comment mockComment = Comment.builder()
                .id(commentId)
                .user(mockUser)
                .build();
        
        given(commentQueryUseCase.findById(commentId)).willReturn(mockComment);

        // When
        adminCommandService.forceWithdrawUser(reportType, commentId);

        // Then
        ArgumentCaptor<AdminWithdrawEvent> eventCaptor =
                ArgumentCaptor.forClass(AdminWithdrawEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        AdminWithdrawEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(userId);
        assertThat(capturedEvent.reason()).isEqualTo("관리자 강제 탈퇴");
    }

    @Test
    @DisplayName("ERROR/IMPROVEMENT 타입 신고로 제재 시도 시 예외 발생")
    void shouldThrowException_WhenInvalidReportType() {
        // Given
        ReportType errorReportType = ReportType.ERROR;
        ReportType improvementReportType = ReportType.IMPROVEMENT;

        // When & Then - banUser 테스트
        assertThatThrownBy(() -> adminCommandService.banUser(errorReportType, null))
                .isInstanceOf(AdminCustomException.class)
                .hasFieldOrPropertyWithValue("adminErrorCode", AdminErrorCode.INVALID_REPORT_TARGET);

        assertThatThrownBy(() -> adminCommandService.banUser(improvementReportType, null))
                .isInstanceOf(AdminCustomException.class)
                .hasFieldOrPropertyWithValue("adminErrorCode", AdminErrorCode.INVALID_REPORT_TARGET);

        // When & Then - forceWithdrawUser 테스트
        assertThatThrownBy(() -> adminCommandService.forceWithdrawUser(errorReportType, null))
                .isInstanceOf(AdminCustomException.class)
                .hasFieldOrPropertyWithValue("adminErrorCode", AdminErrorCode.INVALID_REPORT_TARGET);

        assertThatThrownBy(() -> adminCommandService.forceWithdrawUser(improvementReportType, null))
                .isInstanceOf(AdminCustomException.class)
                .hasFieldOrPropertyWithValue("adminErrorCode", AdminErrorCode.INVALID_REPORT_TARGET);

        verify(eventPublisher, never()).publishEvent(any());
    }

    // ========== createReport 메서드 테스트 ==========

    @Test
    @DisplayName("인증된 사용자 신고 생성 - 성공")
    void createReport_AuthenticatedUser_Success() {
        // Given
        Long userId = 1L;
        ReportType reportType = ReportType.COMMENT;
        Long targetId = 123L;
        String content = "부적절한 댓글입니다";
        
        User reporter = createTestUserWithId(userId);

        Report expectedReport = Report.createReport(reportType, targetId, content, reporter);

        given(userQueryPort.findById(userId)).willReturn(Optional.of(reporter));
        given(adminCommandPort.save(any(Report.class))).willReturn(expectedReport);

        // When
        adminCommandService.createReport(userId, reportType, targetId, content);

        // Then
        verify(userQueryPort, times(1)).findById(userId);
        verify(adminCommandPort, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("익명 사용자 신고 생성 - 성공")
    void createReport_AnonymousUser_Success() {
        // Given
        Long userId = null; // 익명 사용자
        ReportType reportType = ReportType.POST;
        Long targetId = 456L;
        String content = "스팸 게시글입니다";
        
        Report expectedReport = Report.createReport(reportType, targetId, content, null);
        given(adminCommandPort.save(any(Report.class))).willReturn(expectedReport);

        // When
        adminCommandService.createReport(userId, reportType, targetId, content);

        // Then
        verify(userQueryPort, never()).findById(any()); // 익명 사용자는 조회하지 않음
        verify(adminCommandPort, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("건의사항 생성 - 성공 (targetId null)")
    void createReport_Suggestion_Success() {
        // Given
        Long userId = 2L;
        ReportType reportType = ReportType.IMPROVEMENT;
        Long targetId = null;
        String content = "새로운 기능을 건의합니다";
        
        User reporter = createTestUserWithId(userId);

        Report expectedReport = Report.createReport(reportType, targetId, content, reporter);

        given(userQueryPort.findById(userId)).willReturn(Optional.of(reporter));
        given(adminCommandPort.save(any(Report.class))).willReturn(expectedReport);

        // When
        adminCommandService.createReport(userId, reportType, targetId, content);

        // Then
        verify(userQueryPort, times(1)).findById(userId);
        verify(adminCommandPort, times(1)).save(any(Report.class));
    }


    @Test
    @DisplayName("신고 생성 - 성공 (존재하지 않는 사용자 ID는 익명 신고로 처리)")
    void createReport_Success_NonExistentUserTreatedAsAnonymous() {
        // Given
        Long userId = 999L;
        ReportType reportType = ReportType.COMMENT;
        Long targetId = 123L;
        String content = "부적절한 댓글입니다";
        
        given(userQueryPort.findById(userId)).willReturn(Optional.empty());

        // When & Then - 예외가 발생하지 않고 정상적으로 처리되어야 함
        assertThatCode(() -> adminCommandService.createReport(userId, reportType, targetId, content))
                .doesNotThrowAnyException();

        verify(userQueryPort, times(1)).findById(userId);
        verify(adminCommandPort, times(1)).save(any(Report.class));
    }


}