package jaeik.bimillog.infrastructure.adapter.in.admin.dto;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.in.admin.dto.ReportDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        assertThat(violation.getMessage()).isEqualTo("POST/COMMENT 신고는 targetId가 필수입니다");
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
        assertThat(violation.getMessage()).isEqualTo("POST/COMMENT 신고는 targetId가 필수입니다");
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
        assertThat(violation.getMessage()).isEqualTo("ERROR/IMPROVEMENT 신고는 targetId가 없어야 합니다");
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
        assertThat(violation.getMessage()).isEqualTo("ERROR/IMPROVEMENT 신고는 targetId가 없어야 합니다");
    }

    @Test
    @DisplayName("사용자 제재 가능 타입 확인 - POST, COMMENT만 가능")
    void isBannableReportType_OnlyPostAndComment() {
        // Given
        ReportDTO postReport = ReportDTO.builder().reportType(ReportType.POST).build();
        ReportDTO commentReport = ReportDTO.builder().reportType(ReportType.COMMENT).build();
        ReportDTO errorReport = ReportDTO.builder().reportType(ReportType.ERROR).build();
        ReportDTO improvementReport = ReportDTO.builder().reportType(ReportType.IMPROVEMENT).build();

        // When & Then
        assertThat(postReport.isBannableReportType()).isTrue();
        assertThat(commentReport.isBannableReportType()).isTrue();
        assertThat(errorReport.isBannableReportType()).isFalse();
        assertThat(improvementReport.isBannableReportType()).isFalse();
    }

    @Test
    @DisplayName("인증된 사용자 정보 설정 - 성공")
    void enrichReporterInfo_AuthenticatedUser_Success() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder().build();
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getUsername()).willReturn("testuser");

        // When
        reportDTO.enrichReporterInfo(userDetails);

        // Then
        assertThat(reportDTO.getReporterId()).isEqualTo(100L);
        assertThat(reportDTO.getReporterName()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("익명 사용자 정보 설정 - 성공")
    void enrichReporterInfo_AnonymousUser_Success() {
        // Given
        ReportDTO reportDTO = ReportDTO.builder().build();

        // When
        reportDTO.enrichReporterInfo(null);

        // Then
        assertThat(reportDTO.getReporterId()).isNull();
        assertThat(reportDTO.getReporterName()).isEqualTo("익명");
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
}