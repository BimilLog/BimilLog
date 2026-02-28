package jaeik.bimillog.unit.domain.admin;

import jaeik.bimillog.domain.admin.dto.ReportDTO;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>ReportDTO 검증 테스트</h2>
 * <p>ReportDTO의 교차 필드 검증과 비즈니스 로직을 테스트합니다.</p>
 * <p>Bean Validation 어노테이션과 사용자 정의 검증 메서드를 검증</p>
 *
 * @author Jaeik
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

    /**
     * <h3>신고 타입별 targetId 검증 성공 케이스 제공</h3>
     * <p>각 신고 타입에 따라 올바른 targetId 조합을 제공합니다.</p>
     *
     * @return 신고 타입, targetId, 내용의 조합
     */
    static Stream<Arguments> provideValidReportScenarios() {
        return Stream.of(
                Arguments.of(ReportType.POST, 123L, "부적절한 게시글입니다."),
                Arguments.of(ReportType.COMMENT, 456L, "부적절한 댓글입니다."),
                Arguments.of(ReportType.ERROR, null, "시스템 오류가 발생했습니다."),
                Arguments.of(ReportType.IMPROVEMENT, null, "새로운 기능을 제안합니다.")
        );
    }

    /**
     * <h3>신고 타입별 targetId 검증 실패 케이스 제공</h3>
     * <p>각 신고 타입에 따라 잘못된 targetId 조합을 제공합니다.</p>
     *
     * @return 신고 타입, targetId, 예상 에러 메시지의 조합
     */
    static Stream<Arguments> provideInvalidReportScenarios() {
        return Stream.of(
                Arguments.of(ReportType.POST, null, "글, 댓글 신고는 신고대상이 필수입니다"),
                Arguments.of(ReportType.COMMENT, null, "글, 댓글 신고는 신고대상이 필수입니다"),
                Arguments.of(ReportType.ERROR, 123L, "에러, 개선 신고는 신고대상이 없어야 합니다"),
                Arguments.of(ReportType.IMPROVEMENT, 123L, "에러, 개선 신고는 신고대상이 없어야 합니다")
        );
    }

    @ParameterizedTest(name = "{0} 타입: targetId={1}")
    @MethodSource("provideValidReportScenarios")
    @DisplayName("신고 타입별 targetId 검증 - 성공")
    void shouldValidateTargetIdByReportType_Success(ReportType reportType, Long targetId, String content) {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(reportType)
                .targetId(targetId)
                .content(content)
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "{0} 타입: 잘못된 targetId={1}")
    @MethodSource("provideInvalidReportScenarios")
    @DisplayName("신고 타입별 targetId 검증 - 실패")
    void shouldValidateTargetIdByReportType_Failure(ReportType reportType, Long targetId, String expectedMessage) {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(reportType)
                .targetId(targetId)
                .content("신고 내용입니다. 최소 10자 이상")
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<ReportDTO> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo(expectedMessage);
    }


    @ParameterizedTest(name = "내용: {0}")
    @MethodSource("provideContentLengthScenarios")
    @DisplayName("신고 내용 검증 - 길이 경계값")
    void reportContent_LengthValidation(String content, int expectedViolationCount, String expectedMessage) {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(123L)
                .content(content)
                .build();

        // When
        Set<ConstraintViolation<ReportDTO>> violations = validator.validate(reportDTO);

        // Then
        assertThat(violations).hasSize(expectedViolationCount);
        if (expectedViolationCount > 0) {
            assertThat(violations).anyMatch(v -> v.getMessage().contains(expectedMessage));
        }
    }

    static Stream<Arguments> provideContentLengthScenarios() {
        return Stream.of(
            // 빈 값 (@NotBlank + @Size 둘 다 실패)
            Arguments.of("", 2, "신고 내용은"),
            // 길이 부족 (2자)
            Arguments.of("짧음", 1, "신고 내용은 10-500자 사이여야 합니다"),
            // 길이 초과 (501자)
            Arguments.of("a".repeat(501), 1, "신고 내용은 10-500자 사이여야 합니다")
        );
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