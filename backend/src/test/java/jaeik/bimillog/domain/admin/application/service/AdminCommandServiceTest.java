package jaeik.bimillog.domain.admin.application.service;

import jaeik.bimillog.domain.admin.application.port.out.AdminCommandPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.admin.event.AdminWithdrawRequestedEvent;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.domain.admin.exception.AdminCustomException;
import jaeik.bimillog.domain.admin.exception.AdminErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    private AdminCommandPort adminCommandPort;

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private PostQueryUseCase postQueryUseCase;

    @Mock
    private CommentQueryUseCase commentQueryUseCase;

    @InjectMocks
    private AdminCommandService adminCommandService;

    private User testUser;
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
        Post testPost = Post.builder()
                .id(200L)
                .user(testUser)
                .build();
        given(postQueryUseCase.findById(200L)).willReturn(Optional.of(testPost));

        // When
        adminCommandService.banUser(testReportType, testTargetId);

        // Then
        ArgumentCaptor<UserBannedEvent> eventCaptor = ArgumentCaptor.forClass(UserBannedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserBannedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(200L);
        assertThat(capturedEvent.socialId()).isEqualTo("kakao123");
        assertThat(capturedEvent.provider()).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("targetId가 null인 경우 INVALID_REPORT_TARGET 예외 발생")
    void shouldThrowException_WhenTargetIdIsNull() {
        // Given
        ReportType invalidReportType = ReportType.POST;
        Long invalidTargetId = null;

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(invalidReportType, invalidTargetId))
                .isInstanceOf(AdminCustomException.class)
                .hasFieldOrPropertyWithValue("adminErrorCode", AdminErrorCode.INVALID_REPORT_TARGET);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("게시글이 존재하지 않는 경우 POST_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenPostNotFound() {
        // Given
        given(postQueryUseCase.findById(200L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(testReportType, testTargetId))
                .isInstanceOf(AdminCustomException.class)
                .hasFieldOrPropertyWithValue("adminErrorCode", AdminErrorCode.INVALID_REPORT_TARGET);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("댓글 신고의 경우 정상 처리")
    void shouldBanUser_WhenValidCommentReport() {
        // Given
        ReportType commentReportType = ReportType.COMMENT;
        Long commentTargetId = 300L;
        
        Comment testComment = Comment.builder()
                .id(300L)
                .user(testUser)
                .build();
        given(commentQueryUseCase.findById(300L)).willReturn(Optional.of(testComment));

        // When
        adminCommandService.banUser(commentReportType, commentTargetId);

        // Then
        ArgumentCaptor<UserBannedEvent> eventCaptor = ArgumentCaptor.forClass(UserBannedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserBannedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(200L);
        assertThat(capturedEvent.socialId()).isEqualTo("kakao123");
        assertThat(capturedEvent.provider()).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("유효한 댓글 신고로 강제 탈퇴 시 이벤트 발행")
    void shouldPublishEvent_WhenValidCommentReport() {
        // Given
        Long commentId = 1L;
        Long userId = 100L;
        ReportType reportType = ReportType.COMMENT;
        
        User mockUser = User.builder()
                .id(userId)
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .build();
        Comment mockComment = Comment.builder()
                .id(commentId)
                .user(mockUser)
                .build();
        
        given(commentQueryUseCase.findById(commentId)).willReturn(Optional.of(mockComment));

        // When
        adminCommandService.forceWithdrawUser(reportType, commentId);

        // Then
        ArgumentCaptor<AdminWithdrawRequestedEvent> eventCaptor = 
                ArgumentCaptor.forClass(AdminWithdrawRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        AdminWithdrawRequestedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(userId);
        assertThat(capturedEvent.reason()).isEqualTo("관리자 강제 탈퇴");
    }

    @Test
    @DisplayName("ERROR 타입 신고로 강제 탈퇴 시도 시 예외 발생")
    void shouldThrowException_WhenErrorTypeReport() {
        // Given
        ReportType errorReportType = ReportType.ERROR;
        Long targetId = null;

        // When & Then
        assertThatThrownBy(() -> adminCommandService.forceWithdrawUser(errorReportType, targetId))
                .isInstanceOf(AdminCustomException.class)
                .hasFieldOrPropertyWithValue("adminErrorCode", AdminErrorCode.INVALID_REPORT_TARGET);
        
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("ERROR 타입 신고시 null 반환 및 정상 처리")
    void shouldReturnNull_WhenErrorReportType() {
        // Given
        ReportType errorReportType = ReportType.ERROR;
        Long targetId = null;

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(errorReportType, targetId))
                .isInstanceOf(AdminCustomException.class)
                .hasFieldOrPropertyWithValue("adminErrorCode", AdminErrorCode.INVALID_REPORT_TARGET);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("IMPROVEMENT 타입 신고시 null 반환 및 정상 처리")
    void shouldReturnNull_WhenImprovementReportType() {
        // Given
        ReportType improvementReportType = ReportType.IMPROVEMENT;
        Long targetId = null;

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(improvementReportType, targetId))
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
        
        User reporter = User.builder()
                .id(userId)
                .userName("testuser")
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .build();

        Comment testComment = Comment.builder()
                .id(123L)
                .user(reporter)
                .build();

        Report expectedReport = Report.createReport(reportType, targetId, content, reporter);

        given(userQueryPort.findById(userId)).willReturn(Optional.of(reporter));
        given(commentQueryUseCase.findById(123L)).willReturn(Optional.of(testComment));
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
        
        User postUser = User.builder()
                .id(456L)
                .userName("postUser")
                .build();
        
        Post testPost = Post.builder()
                .id(456L)
                .user(postUser)
                .build();
        
        Report expectedReport = Report.createReport(reportType, targetId, content, null);
        given(postQueryUseCase.findById(456L)).willReturn(Optional.of(testPost));
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
        
        User reporter = User.builder()
                .id(userId)
                .userName("suggester")
                .socialId("social456")
                .provider(SocialProvider.KAKAO)
                .build();

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
    @DisplayName("신고 생성 - 실패 (존재하지 않는 사용자)")
    void createReport_Fail_UserNotFound() {
        // Given
        Long userId = 999L;
        ReportType reportType = ReportType.COMMENT;
        Long targetId = 123L;
        String content = "부적절한 댓글입니다";
        
        User testUser = User.builder()
                .id(123L)
                .userName("commentUser")
                .build();
        
        Comment testComment = Comment.builder()
                .id(123L)
                .user(testUser)
                .build();
        
        given(userQueryPort.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminCommandService.createReport(userId, reportType, targetId, content))
                .isInstanceOf(AdminCustomException.class)
                .hasFieldOrPropertyWithValue("adminErrorCode", AdminErrorCode.USER_NOT_FOUND);

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

        ReportType reportType1 = ReportType.COMMENT;
        ReportType reportType2 = ReportType.POST;
        ReportType reportType3 = ReportType.IMPROVEMENT;
        Long targetId1 = 100L;
        Long targetId2 = 200L;
        Long targetId3 = null;
        String content1 = "신고 내용 1";
        String content2 = "신고 내용 2";
        String content3 = "건의 내용";

        User user1 = User.builder().id(userId1).userName("user1").build();
        User user3 = User.builder().id(userId3).userName("user3").build();

        Comment testComment = Comment.builder().id(100L).user(user1).build();
        Post testPost = Post.builder().id(200L).user(user1).build();

        given(userQueryPort.findById(userId1)).willReturn(Optional.of(user1));
        given(userQueryPort.findById(userId3)).willReturn(Optional.of(user3));
        given(commentQueryUseCase.findById(100L)).willReturn(Optional.of(testComment));
        given(postQueryUseCase.findById(200L)).willReturn(Optional.of(testPost));
        given(adminCommandPort.save(any(Report.class))).willReturn(mock(Report.class));

        // When
        adminCommandService.createReport(userId1, reportType1, targetId1, content1);
        adminCommandService.createReport(userId2, reportType2, targetId2, content2); // 익명
        adminCommandService.createReport(userId3, reportType3, targetId3, content3);

        // Then
        verify(userQueryPort, times(1)).findById(userId1);
        verify(userQueryPort, never()).findById(userId2); // 익명은 조회하지 않음
        verify(userQueryPort, times(1)).findById(userId3);
        verify(adminCommandPort, times(3)).save(any(Report.class));
    }

}