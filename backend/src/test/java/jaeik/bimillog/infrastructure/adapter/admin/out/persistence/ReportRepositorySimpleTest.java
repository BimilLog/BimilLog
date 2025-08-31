package jaeik.bimillog.infrastructure.adapter.admin.out.persistence;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>ReportRepository 단위 테스트</h2>
 * <p>ReportRepository의 기본 동작을 Mock을 사용하여 검증</p>
 * <p>TestContainers 환경 설정 문제를 피한 간단한 단위 테스트</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class ReportRepositorySimpleTest {

    @Mock
    private ReportRepository reportRepository;

    /**
     * <h3>신고 저장 테스트</h3>
     * <p>신고 엔티티가 Repository를 통해 올바르게 저장되는지 검증</p>
     */
    @Test
    void shouldSaveReport_WhenValidReportProvided() {
        // Given: 신고자와 신고 정보
        Setting setting = Setting.builder()
                .commentNotification(true)
                .messageNotification(true)
                .postFeaturedNotification(true)
                .build();

        User reporter = User.builder()
                .userName("reporter")
                .socialId("소셜ID123")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .socialNickname("reporter_nickname")
                .setting(setting)
                .build();

        Report report = Report.builder()
                .reporter(reporter)
                .reportType(ReportType.POST)
                .targetId(1L)
                .content("부적절한 게시글입니다.")
                .build();

        Report savedReport = Report.builder()
                .id(1L)
                .reporter(reporter)
                .reportType(ReportType.POST)
                .targetId(1L)
                .content("부적절한 게시글입니다.")
                .build();

        given(reportRepository.save(report)).willReturn(savedReport);

        // When: 신고 저장
        Report result = reportRepository.save(report);

        // Then: 올바르게 저장되고 ID가 할당되는지 검증
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getReporter().getUserName()).isEqualTo("reporter");
        assertThat(result.getReportType()).isEqualTo(ReportType.POST);
        assertThat(result.getTargetId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("부적절한 게시글입니다.");
        
        verify(reportRepository).save(report);
    }

    /**
     * <h3>ID로 신고 조회 테스트 - 성공</h3>
     * <p>저장된 신고를 ID로 올바르게 조회할 수 있는지 검증</p>
     */
    @Test
    void shouldFindReportById_WhenReportExists() {
        // Given: 저장된 신고
        Long reportId = 1L;
        Setting setting = Setting.createSetting();
        User reporter = User.builder()
                .id(1L)
                .userName("reporter")
                .socialId("소셜ID123")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();

        Report expectedReport = Report.builder()
                .id(reportId)
                .reporter(reporter)
                .reportType(ReportType.COMMENT)
                .targetId(2L)
                .content("스팸 댓글입니다.")
                .build();

        given(reportRepository.findById(reportId)).willReturn(Optional.of(expectedReport));

        // When: ID로 조회
        Optional<Report> foundReport = reportRepository.findById(reportId);

        // Then: 올바른 신고가 조회되는지 검증
        assertThat(foundReport).isPresent();
        assertThat(foundReport.get().getId()).isEqualTo(reportId);
        assertThat(foundReport.get().getReportType()).isEqualTo(ReportType.COMMENT);
        assertThat(foundReport.get().getTargetId()).isEqualTo(2L);
        assertThat(foundReport.get().getContent()).isEqualTo("스팸 댓글입니다.");
        assertThat(foundReport.get().getReporter().getUserName()).isEqualTo("reporter");
        
        verify(reportRepository).findById(reportId);
    }

    /**
     * <h3>ID로 신고 조회 테스트 - 존재하지 않는 경우</h3>
     * <p>존재하지 않는 ID로 조회 시 빈 Optional을 반환하는지 검증</p>
     */
    @Test
    void shouldReturnEmpty_WhenReportNotExists() {
        // Given: 존재하지 않는 ID
        Long nonExistentId = 999L;
        given(reportRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // When: 존재하지 않는 ID로 조회
        Optional<Report> foundReport = reportRepository.findById(nonExistentId);

        // Then: 빈 Optional 반환
        assertThat(foundReport).isEmpty();
        verify(reportRepository).findById(nonExistentId);
    }
}