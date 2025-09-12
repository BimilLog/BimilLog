package jaeik.bimillog.infrastructure.adapter.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>SignUpRequestDTO 검증 테스트</h2>
 * <p>회원가입 요청 DTO의 유효성 검증 로직을 테스트합니다.</p>
 * <p>Bean Validation과 @AssertTrue 커스텀 검증 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("SignUpRequestDTO 검증 테스트")
class SignUpRequestDTOTest {

    private Validator validator;
    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("유효한 요청 테스트")
    class ValidRequestTests {

        @Test
        @DisplayName("모든 필드가 유효한 경우 - 검증 통과")
        void shouldPass_WhenAllFieldsValid() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("테스트사용자", VALID_UUID);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("영문 사용자 이름 - 검증 통과")
        void shouldPass_WhenUserNameIsEnglish() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("TestUser", VALID_UUID);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("숫자가 포함된 사용자 이름 - 검증 통과")
        void shouldPass_WhenUserNameContainsNumbers() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("사용자123", VALID_UUID);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("허용된 특수문자가 포함된 사용자 이름 - 검증 통과")
        void shouldPass_WhenUserNameContainsAllowedSpecialChars() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("사용자_이름.test-123", VALID_UUID);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("userName 검증 테스트")
    class UserNameValidationTests {

        @Test
        @DisplayName("userName이 null인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenUserNameIsNull() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO(null, VALID_UUID);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(3); // @NotBlank + @Size + @AssertTrue
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .containsAnyOf("사용자 이름은 필수입니다.", "사용자 이름에 유효한 문자가 포함되어야 합니다.");
        }

        @Test
        @DisplayName("userName이 빈 문자열인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenUserNameIsEmpty() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("", VALID_UUID);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(3); // @NotBlank + @Size + @AssertTrue
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .containsAnyOf("사용자 이름은 필수입니다.", "사용자 이름에 유효한 문자가 포함되어야 합니다.");
        }

        @Test
        @DisplayName("userName이 공백으로만 구성된 경우 - @AssertTrue 검증 실패")
        void shouldFail_WhenUserNameIsBlank() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("   ", VALID_UUID);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1); // @AssertTrue만 실패 (@NotBlank, @Size는 통과)
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("사용자 이름에 유효한 문자가 포함되어야 합니다.");
        }

        @Test
        @DisplayName("userName이 20자를 초과하는 경우 - @Size 검증 실패")
        void shouldFail_WhenUserNameIsTooLong() {
            // Given
            String longUserName = "이것은아주긴사용자이름입니다정말로길어요"; // 21자
            SignUpRequestDTO request = new SignUpRequestDTO(longUserName, VALID_UUID);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(2); // @Size + @AssertTrue
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .containsAnyOf("사용자 이름은 1자 이상 20자 이하여야 합니다.", "사용자 이름에 유효한 문자가 포함되어야 합니다.");
        }

        @Test
        @DisplayName("userName에 허용되지 않은 특수문자가 포함된 경우 - @AssertTrue 검증 실패")
        void shouldFail_WhenUserNameContainsInvalidSpecialChars() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("사용자@#$%", VALID_UUID);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("사용자 이름에 허용되지 않은 문자가 포함되어 있습니다.");
        }

        @Test
        @DisplayName("userName 앞뒤 공백 처리 후 20자 초과 - @AssertTrue 검증 실패")
        void shouldFail_WhenTrimmedUserNameIsTooLong() {
            // Given
            String userNameWithSpaces = "  이것은아주긴사용자이름입니다정말로길어요  "; // trim 후 21자
            SignUpRequestDTO request = new SignUpRequestDTO(userNameWithSpaces, VALID_UUID);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("사용자 이름에 유효한 문자가 포함되어야 합니다.");
        }
    }

    @Nested
    @DisplayName("UUID 검증 테스트")
    class UuidValidationTests {

        @Test
        @DisplayName("UUID가 null인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenUuidIsNull() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("테스트사용자", null);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(2); // @NotBlank + @Pattern
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("임시 UUID는 필수입니다.", "올바른 UUID 형식이 아닙니다.");
        }

        @Test
        @DisplayName("UUID가 빈 문자열인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenUuidIsEmpty() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("테스트사용자", "");

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(2); // @NotBlank + @Pattern
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("임시 UUID는 필수입니다.", "올바른 UUID 형식이 아닙니다.");
        }

        @Test
        @DisplayName("유효하지 않은 UUID 형식 - @Pattern 검증 실패")
        void shouldFail_WhenUuidFormatIsInvalid() {
            // Given
            String invalidUuid = "not-a-valid-uuid-format";
            SignUpRequestDTO request = new SignUpRequestDTO("테스트사용자", invalidUuid);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("올바른 UUID 형식이 아닙니다.");
        }

        @Test
        @DisplayName("UUID에 대문자가 포함된 경우 - @Pattern 검증 실패")
        void shouldFail_WhenUuidContainsUpperCase() {
            // Given
            String uppercaseUuid = "123E4567-E89B-12D3-A456-426614174000";
            SignUpRequestDTO request = new SignUpRequestDTO("테스트사용자", uppercaseUuid);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("올바른 UUID 형식이 아닙니다.");
        }

        @Test
        @DisplayName("UUID 구분자가 잘못된 경우 - @Pattern 검증 실패")
        void shouldFail_WhenUuidDelimiterIsWrong() {
            // Given
            String wrongDelimiterUuid = "123e4567_e89b_12d3_a456_426614174000"; // '-' 대신 '_' 사용
            SignUpRequestDTO request = new SignUpRequestDTO("테스트사용자", wrongDelimiterUuid);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("올바른 UUID 형식이 아닙니다.");
        }

        @Test
        @DisplayName("UUID 길이가 잘못된 경우 - @Pattern 검증 실패")
        void shouldFail_WhenUuidLengthIsWrong() {
            // Given
            String shortUuid = "123e4567-e89b-12d3-a456-42661417400"; // 한 글자 부족
            SignUpRequestDTO request = new SignUpRequestDTO("테스트사용자", shortUuid);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("올바른 UUID 형식이 아닙니다.");
        }
    }

    @Nested
    @DisplayName("복합 검증 테스트")
    class CombinedValidationTests {

        @Test
        @DisplayName("userName과 UUID 모두 유효하지 않은 경우 - 여러 검증 실패")
        void shouldFail_WhenBothFieldsAreInvalid() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("", "invalid-uuid");

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(4); // userName(3) + UUID(1)
        }

        @Test
        @DisplayName("극단적인 경우 - 모든 필드가 null")
        void shouldFail_WhenAllFieldsAreNull() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO(null, null);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(5); // userName(3) + UUID(2)
        }
    }
}