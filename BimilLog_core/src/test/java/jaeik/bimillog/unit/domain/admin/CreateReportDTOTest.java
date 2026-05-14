package jaeik.bimillog.unit.domain.admin;

import jaeik.bimillog.domain.admin.dto.CreateReportDTO;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>CreateReportDTO 검증 테스트</h2>
 * <p>CreateReportDTO의 교차 필드 검증과 비즈니스 로직을 테스트합니다.</p>
 * <p>Bean Validation 어노테이션과 사용자 정의 검증 메서드를 검증</p>
 *
 * @author Jaeik
 */
@DisplayName("CreateReportDTO 검증 테스트")
@Tag("unit")
class CreateReportDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    static Stream<Arguments> provideValidReportScenarios() {
        return Stream.of(
                Arguments.of(ReportType.POST, 123L, "부적절한 게시글입니다."),
                Arguments.of(ReportType.COMMENT, 456L, "부적절한 댓글입니다."),
                Arguments.of(ReportType.ERROR, null, "시스템 오류가 발생했습니다."),
                Arguments.of(ReportType.IMPROVEMENT, null, "새로운 기능을 제안합니다.")
        );
    }

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
        CreateReportDTO createReportDTO = new CreateReportDTO(null, null, reportType, targetId, content);

        // When
        Set<ConstraintViolation<CreateReportDTO>> violations = validator.validate(createReportDTO);

        // Then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "{0} 타입: 잘못된 targetId={1}")
    @MethodSource("provideInvalidReportScenarios")
    @DisplayName("신고 타입별 targetId 검증 - 실패")
    void shouldValidateTargetIdByReportType_Failure(ReportType reportType, Long targetId, String expectedMessage) {
        // Given
        CreateReportDTO createReportDTO = new CreateReportDTO(null, null, reportType, targetId, "신고 내용입니다. 최소 10자 이상");

        // When
        Set<ConstraintViolation<CreateReportDTO>> violations = validator.validate(createReportDTO);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<CreateReportDTO> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo(expectedMessage);
    }


    @ParameterizedTest(name = "내용: {0}")
    @MethodSource("provideContentLengthScenarios")
    @DisplayName("신고 내용 검증 - 길이 경계값")
    void reportContent_LengthValidation(String content, int expectedViolationCount, String expectedMessage) {
        // Given
        CreateReportDTO createReportDTO = new CreateReportDTO(null, null, ReportType.POST, 123L, content);

        // When
        Set<ConstraintViolation<CreateReportDTO>> violations = validator.validate(createReportDTO);

        // Then
        assertThat(violations).hasSize(expectedViolationCount);
        if (expectedViolationCount > 0) {
            assertThat(violations).anyMatch(v -> v.getMessage().contains(expectedMessage));
        }
    }

    static Stream<Arguments> provideContentLengthScenarios() {
        return Stream.of(
            Arguments.of("", 2, "신고 내용은"),
            Arguments.of("짧음", 1, "신고 내용은 10-500자 사이여야 합니다"),
            Arguments.of("a".repeat(501), 1, "신고 내용은 10-500자 사이여야 합니다")
        );
    }

    /**
     * Kotlin data class의 non-null `reportType` 파라미터는 생성자 진입 시점에 NPE를 던집니다.
     * Bean Validation 단계 전에 차단되므로 타입 시스템 자체가 더 강한 보장을 제공합니다.
     */
    @Test
    @DisplayName("신고 타입 검증 - null 시 Kotlin 타입 시스템이 차단")
    void reportType_Null_RejectedByTypeSystem() {
        assertThatThrownBy(
                () -> new CreateReportDTO(null, null, null, 123L, "신고 내용입니다."))
                .isInstanceOf(NullPointerException.class);
    }
}
