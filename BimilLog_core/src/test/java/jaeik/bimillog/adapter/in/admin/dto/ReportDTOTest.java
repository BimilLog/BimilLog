package jaeik.bimillog.adapter.in.admin.dto;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.in.admin.dto.ReportDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * <h2>ReportDTO 검증 테스트</h2>
 * <p>ReportDTO의 교차 필드 검증과 비즈니스 로직을 테스트합니다.</p>
 * <p>Bean Validation 어노테이션과 사용자 정의 검증 메서드를 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("ReportDTO 검증 테스트")
@Tag("unit")
class ReportDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("POST 타입 신고 시 targetId 필수 - 성공")
    void postReport_WithTargetId_Success() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(123L)
                .content("부적절한 게시글입니다.")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("POST 타입 신고 시 targetId null - 검증 실패")
    void postReport_WithoutTargetId_ValidationFailure() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(null)
                .content("부적절한 게시글입니다.")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<ReportDTO> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("글, 댓글 신고는 신고대상이 필수입니다");
    }

    @Test
    @DisplayName("COMMENT 타입 신고 시 targetId 필수 - 성공")
    void commentReport_WithTargetId_Success() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.COMMENT)
                .targetId(456L)
                .content("부적절한 댓글입니다.")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("COMMENT 타입 신고 시 targetId null - 검증 실패")
    void commentReport_WithoutTargetId_ValidationFailure() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.COMMENT)
                .targetId(null)
                .content("부적절한 댓글입니다.")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<ReportDTO> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("글, 댓글 신고는 신고대상이 필수입니다");
    }

    @Test
    @DisplayName("ERROR 타입 신고 시 targetId null - 성공")
    void errorReport_WithoutTargetId_Success() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.ERROR)
                .targetId(null)
                .content("시스템 오류가 발생했습니다.")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ERROR 타입 신고 시 targetId 존재 - 검증 실패")
    void errorReport_WithTargetId_ValidationFailure() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.ERROR)
                .targetId(123L)
                .content("시스템 오류가 발생했습니다.")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<ReportDTO> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("에러, 개선 신고는 신고대상이 없어야 합니다");
    }

    @Test
    @DisplayName("IMPROVEMENT 타입 신고 시 targetId null - 성공")
    void improvementReport_WithoutTargetId_Success() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.IMPROVEMENT)
                .targetId(null)
                .content("새로운 기능을 제안합니다.")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("IMPROVEMENT 타입 신고 시 targetId 존재 - 검증 실패")
    void improvementReport_WithTargetId_ValidationFailure() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.IMPROVEMENT)
                .targetId(123L)
                .content("새로운 기능을 제안합니다.")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<ReportDTO> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("에러, 개선 신고는 신고대상이 없어야 합니다");
    }


    @Test
    @DisplayName("신고 내용 검증 - 빈 값 실패")
    void reportContent_BlankContent_ValidationFailure() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(123L)
                .content("")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).hasSize(2); // @NotBlank + @Size 둘 다 실패
        assertThat(violations).anyMatch(v -> v.getMessage().equals("신고 내용은 필수입니다"));
        assertThat(violations).anyMatch(v -> v.getMessage().equals("신고 내용은 10-500자 사이여야 합니다"));
    }

    @Test
    @DisplayName("신고 내용 검증 - 길이 초과 실패")
    void reportContent_TooLong_ValidationFailure() {
        // Given
        String longContent = "a".repeat(501); // 501자
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(123L)
                .content(longContent)
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<ReportDTO> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("신고 내용은 10-500자 사이여야 합니다");
    }

    @Test
    @DisplayName("신고 내용 검증 - 길이 부족 실패")
    void reportContent_TooShort_ValidationFailure() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(123L)
                .content("짧음") // 2자
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<ReportDTO> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("신고 내용은 10-500자 사이여야 합니다");
    }

    @Test
    @DisplayName("신고 타입 검증 - null 실패")
    void reportType_Null_ValidationFailure() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(null)
                .targetId(123L)
                .content("신고 내용입니다.")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("신고 유형은 필수입니다"));
    }

    @Test
    @DisplayName("인증된 사용자 신고 엔티티 변환 - 성공")
    void from_AuthenticatedReport_Success() {
        // Given
        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(100L);
        given(mockMember.getMemberName()).willReturn("testuser");

        Report mockReport = mock(Report.class);
        given(mockReport.getId()).willReturn(1L);
        given(mockReport.getReporter()).willReturn(mockMember);
        given(mockReport.getReportType()).willReturn(ReportType.POST);
        given(mockReport.getTargetId()).willReturn(123L);
        given(mockReport.getContent()).willReturn("부적절한 게시글입니다");
        given(mockReport.getCreatedAt()).willReturn(Instant.now());

        // When
        ReportDTO result = ReportDTO.from(mockReport);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getReporterId()).isEqualTo(100L);
        assertThat(result.getReporterName()).isEqualTo("testuser");
        assertThat(result.getReportType()).isEqualTo(ReportType.POST);
        assertThat(result.getTargetId()).isEqualTo(123L);
        assertThat(result.getContent()).isEqualTo("부적절한 게시글입니다");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("익명 사용자 신고 엔티티 변환 - 성공 (null-safe 검증)")
    void from_AnonymousReport_Success() {
        // Given
        Report mockReport = mock(Report.class);
        given(mockReport.getId()).willReturn(2L);
        given(mockReport.getReporter()).willReturn(null); // 익명 사용자
        given(mockReport.getReportType()).willReturn(ReportType.COMMENT);
        given(mockReport.getTargetId()).willReturn(456L);
        given(mockReport.getContent()).willReturn("부적절한 댓글입니다");
        given(mockReport.getCreatedAt()).willReturn(Instant.now());

        // When
        ReportDTO result = ReportDTO.from(mockReport);

        // Then
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getReporterId()).isNull();
        assertThat(result.getReporterName()).isEqualTo("익명");
        assertThat(result.getReportType()).isEqualTo(ReportType.COMMENT);
        assertThat(result.getTargetId()).isEqualTo(456L);
        assertThat(result.getContent()).isEqualTo("부적절한 댓글입니다");
        assertThat(result.getCreatedAt()).isNotNull();
    }
}