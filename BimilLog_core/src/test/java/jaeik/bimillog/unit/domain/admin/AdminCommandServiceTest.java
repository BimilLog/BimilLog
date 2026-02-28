package jaeik.bimillog.unit.domain.admin;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.event.MemberBannedEvent;
import jaeik.bimillog.domain.admin.repository.AdminQueryRepository;
import jaeik.bimillog.domain.admin.adapter.AdminToCommentAdapter;
import jaeik.bimillog.domain.admin.adapter.AdminToPostAdapter;
import jaeik.bimillog.domain.admin.repository.ReportRepository;
import jaeik.bimillog.domain.admin.service.AdminCommandService;
import jaeik.bimillog.domain.auth.service.BlacklistService;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.event.MemberWithdrawnEvent;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
@Tag("unit")
class AdminCommandServiceTest extends BaseUnitTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private AdminQueryRepository adminQueryRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AdminToPostAdapter adminToPostAdapter;

    @Mock
    private AdminToCommentAdapter adminToCommentAdapter;

    @Mock
    private BlacklistService blacklistService;

    @InjectMocks
    private AdminCommandService adminCommandService;

    private ReportType testReportType = ReportType.POST;
    private Long testTargetId = 200L;

    @BeforeEach
    void setUp() {
        adminCommandService = new AdminCommandService(
                eventPublisher,
                reportRepository,
                adminQueryRepository,
                memberRepository,
                adminToPostAdapter,
                adminToCommentAdapter,
                blacklistService
        );
    }

    @Test
    @DisplayName("유효한 신고 정보로 사용자 제재 시 성공")
    void shouldBanUser_WhenValidReportDTO() {
        // Given
        Member memberWithId = createTestMemberWithId(200L);
        Post testPost = PostTestDataBuilder.withId(200L, PostTestDataBuilder.createPost(memberWithId, "테스트 제목", "테스트 내용"));
        given(adminToPostAdapter.findById(200L)).willReturn(testPost);

        // When
        adminCommandService.banUser(testReportType, testTargetId);

        // Then
        ArgumentCaptor<MemberBannedEvent> eventCaptor = ArgumentCaptor.forClass(MemberBannedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        MemberBannedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.memberId()).isEqualTo(200L);
        assertThat(capturedEvent.socialId()).isEqualTo(memberWithId.getSocialId());
        assertThat(capturedEvent.provider()).isEqualTo(memberWithId.getProvider());
    }

    @Test
    @DisplayName("targetId가 null인 경우 NullPointerException 발생")
    void shouldThrowException_WhenTargetIdIsNull() {
        // Given
        ReportType invalidReportType = ReportType.POST;
        Long invalidTargetId = null;

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(invalidReportType, invalidTargetId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADMIN_INVALID_REPORT_TARGET);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("게시글이 존재하지 않는 경우 POST_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenPostNotFound() {
        // Given
        given(adminToPostAdapter.findById(200L)).willThrow(new CustomException(ErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> adminCommandService.banUser(testReportType, testTargetId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADMIN_POST_ALREADY_DELETED);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("댓글 신고의 경우 정상 처리")
    void shouldBanUser_WhenValidCommentReport() {
        // Given
        ReportType commentReportType = ReportType.COMMENT;
        Long commentTargetId = 300L;
        
        Member memberWithId = createTestMemberWithId(200L);
        Comment testComment = Comment.builder()
                .id(300L)
                .member(memberWithId)
                .build();
        given(adminToCommentAdapter.findById(300L)).willReturn(testComment);

        // When
        adminCommandService.banUser(commentReportType, commentTargetId);

        // Then
        ArgumentCaptor<MemberBannedEvent> eventCaptor = ArgumentCaptor.forClass(MemberBannedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        MemberBannedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.memberId()).isEqualTo(200L);
        assertThat(capturedEvent.socialId()).isEqualTo(memberWithId.getSocialId());
        assertThat(capturedEvent.provider()).isEqualTo(memberWithId.getProvider());
    }

    @Test
    @DisplayName("유효한 댓글 신고로 강제 탈퇴 시 이벤트 발행")
    void shouldPublishEvent_WhenValidCommentReport() {
        // Given
        Long commentId = 1L;
        Long memberId = 100L;
        ReportType reportType = ReportType.COMMENT;
        
        Member mockMember = createTestMemberWithId(memberId);
        Comment mockComment = Comment.builder()
                .id(commentId)
                .member(mockMember)
                .build();
        
        given(adminToCommentAdapter.findById(commentId)).willReturn(mockComment);

        // When
        adminCommandService.forceWithdrawUser(reportType, commentId);

        // Then
        ArgumentCaptor<MemberWithdrawnEvent> eventCaptor =
                ArgumentCaptor.forClass(MemberWithdrawnEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        MemberWithdrawnEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.memberId()).isEqualTo(memberId);
        assertThat(capturedEvent.socialId()).isEqualTo(mockMember.getSocialId());
        assertThat(capturedEvent.provider()).isEqualTo(mockMember.getProvider());
    }

    @ParameterizedTest(name = "{0} - {1} 타입으로 제재 시도 시 예외 발생")
    @MethodSource("provideInvalidReportTypeScenarios")
    @DisplayName("유효하지 않은 신고 타입으로 제재 시도 시 예외 발생")
    void shouldThrowException_WhenInvalidReportType(String operation, ReportType reportType) {
        // When & Then
        if ("banUser".equals(operation)) {
            assertThatThrownBy(() -> adminCommandService.banUser(reportType, null))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADMIN_INVALID_REPORT_TARGET);
        } else {
            assertThatThrownBy(() -> adminCommandService.forceWithdrawUser(reportType, null))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADMIN_INVALID_REPORT_TARGET);
        }

        verify(eventPublisher, never()).publishEvent(any());
    }

    private static Stream<Arguments> provideInvalidReportTypeScenarios() {
        return Stream.of(
                Arguments.of("banUser", ReportType.ERROR),
                Arguments.of("banUser", ReportType.IMPROVEMENT),
                Arguments.of("forceWithdrawUser", ReportType.ERROR),
                Arguments.of("forceWithdrawUser", ReportType.IMPROVEMENT)
        );
    }

    // ========== createReport 메서드 테스트 ==========

    @Test
    @DisplayName("인증된 사용자 신고 생성 - 성공")
    void createReport_AuthenticatedUser_Success() {
        // Given
        Long memberId = 1L;
        ReportType reportType = ReportType.COMMENT;
        Long targetId = 123L;
        String content = "부적절한 댓글입니다";

        Member reporter = createTestMemberWithId(memberId);

        Report expectedReport = Report.createReport(reportType, targetId, content, reporter);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(reporter));
        given(reportRepository.save(any(Report.class))).willReturn(expectedReport);

        // When
        adminCommandService.createReport(memberId, reportType, targetId, content);

        // Then
        verify(memberRepository, times(1)).findById(memberId);
        verify(reportRepository, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("익명 사용자 신고 생성 - 성공")
    void createReport_AnonymousUser_Success() {
        // Given
        Long memberId = null; // 익명 사용자
        ReportType reportType = ReportType.POST;
        Long targetId = 456L;
        String content = "스팸 게시글입니다";

        Report expectedReport = Report.createReport(reportType, targetId, content, null);
        given(reportRepository.save(any(Report.class))).willReturn(expectedReport);

        // When
        adminCommandService.createReport(memberId, reportType, targetId, content);

        // Then
        verify(memberRepository, never()).findById(any()); // 익명 사용자는 조회하지 않음
        verify(reportRepository, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("건의사항 생성 - 성공 (targetId null)")
    void createReport_Suggestion_Success() {
        // Given
        Long memberId = 2L;
        ReportType reportType = ReportType.IMPROVEMENT;
        Long targetId = null;
        String content = "새로운 기능을 건의합니다";

        Member reporter = createTestMemberWithId(memberId);

        Report expectedReport = Report.createReport(reportType, targetId, content, reporter);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(reporter));
        given(reportRepository.save(any(Report.class))).willReturn(expectedReport);

        // When
        adminCommandService.createReport(memberId, reportType, targetId, content);

        // Then
        verify(memberRepository, times(1)).findById(memberId);
        verify(reportRepository, times(1)).save(any(Report.class));
    }


    @Test
    @DisplayName("신고 생성 - 성공 (존재하지 않는 사용자 ID는 익명 신고로 처리)")
    void createReport_Success_NonExistentUserTreatedAsAnonymous() {
        // Given
        Long memberId = 999L;
        ReportType reportType = ReportType.COMMENT;
        Long targetId = 123L;
        String content = "부적절한 댓글입니다";

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // When & Then - 예외가 발생하지 않고 정상적으로 처리되어야 함
        assertThatCode(() -> adminCommandService.createReport(memberId, reportType, targetId, content))
                .doesNotThrowAnyException();

        verify(memberRepository, times(1)).findById(memberId);
        verify(reportRepository, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("회원 탈퇴 시 신고자 익명화")
    void anonymizeReporterByUserId_ShouldClearReporter() {
        // Given
        Long memberId = 10L;
        Member reporter = createTestMemberWithId(memberId);
        Report report = Report.createReport(ReportType.POST, 1L, "신고 내용", reporter);
        given(adminQueryRepository.findAllReportsByUserId(memberId)).willReturn(List.of(report));

        // When
        adminCommandService.anonymizeReporterByUserId(memberId);

        // Then
        verify(adminQueryRepository, times(1)).findAllReportsByUserId(memberId);
        assertThat(report.getReporter()).isNull();
    }

}
