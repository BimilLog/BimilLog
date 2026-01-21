package jaeik.bimillog.domain.auth.dto;

import jaeik.bimillog.domain.member.dto.SignUpRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>SignUpRequestDTO 검증 테스트</h2>
 * <p>회원가입 요청 DTO의 유효성 검증 로직을 테스트합니다.</p>
 * <p>Bean Validation과 @AssertTrue 커스텀 검증 테스트</p>
 * <p>UUID는 HttpOnly 쿠키로 전달되어 요청 본문에 포함하지 않음</p>
 */
@DisplayName("SignUpRequestDTO 검증 테스트")
@Tag("unit")
class SignUpRequestDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("유효한 요청 테스트")
    class ValidRequestTests {

        @ParameterizedTest(name = "사용자명: {0}")
        @ValueSource(strings = {"테스트사용자", "TestUser", "사용자123", "사용자1234", "홍길"})
        @DisplayName("유효한 사용자 이름 - 검증 통과")
        void shouldPass_WhenValidUserName(String userName) {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO(userName);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("memberName 검증 테스트")
    class MemberNameValidationTests {

        @Test
        @DisplayName("memberName이 null인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenUserNameIsNull() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO(null);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(3); // @NotBlank + 두 개의 @AssertTrue
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactlyInAnyOrder(
                            "사용자 이름은 필수입니다.",
                            "사용자 이름에 유효한 문자가 포함되어야 합니다.",
                            "특수문자는 사용할 수 없습니다."
                    );
        }

        @Test
        @DisplayName("memberName이 빈 문자열인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenUserNameIsEmpty() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("");

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(4); // @NotBlank + @Size + 두 개의 @AssertTrue
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactlyInAnyOrder(
                            "사용자 이름은 필수입니다.",
                            "사용자 이름은 1자 이상 8자 이하여야 합니다.",
                            "사용자 이름에 유효한 문자가 포함되어야 합니다.",
                            "특수문자는 사용할 수 없습니다."
                    );
        }

        @Test
        @DisplayName("memberName이 공백으로만 구성된 경우 - @AssertTrue 검증 실패")
        void shouldFail_WhenUserNameIsBlank() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("   ");

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(3); // @NotBlank + 두 개의 @AssertTrue
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactlyInAnyOrder(
                            "사용자 이름은 필수입니다.",
                            "사용자 이름에 유효한 문자가 포함되어야 합니다.",
                            "특수문자는 사용할 수 없습니다."
                    );
        }

        @Test
        @DisplayName("memberName이 8자를 초과하는 경우 - @Size 검증 실패")
        void shouldFail_WhenUserNameIsTooLong() {
            // Given
            String longUserName = "사용자이름1234"; // 9자
            SignUpRequestDTO request = new SignUpRequestDTO(longUserName);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(2); // @Size + @AssertTrue (trim)
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactlyInAnyOrder(
                            "사용자 이름은 1자 이상 8자 이하여야 합니다.",
                            "사용자 이름에 유효한 문자가 포함되어야 합니다."
                    );
        }

        @Test
        @DisplayName("memberName이 1자인 경우 - 검증 통과")
        void shouldPass_WhenUserNameIsOneCharacter() {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO("홍");

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest(name = "사용자명: {0}")
        @ValueSource(strings = {"사용자@#$%", "사용자 이름", "사용자._-"})
        @DisplayName("memberName에 특수문자가 포함된 경우 - @AssertTrue 검증 실패")
        void shouldFail_WhenUserNameContainsSpecialChars(String userName) {
            // Given
            SignUpRequestDTO request = new SignUpRequestDTO(userName);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("특수문자는 사용할 수 없습니다.");
        }

        @Test
        @DisplayName("memberName 앞뒤 공백 처리 후 8자 초과 - @AssertTrue 검증 실패")
        void shouldFail_WhenTrimmedUserNameIsTooLong() {
            // Given
            String userNameWithSpaces = "  사용자이름1234  "; // trim 후 9자
            SignUpRequestDTO request = new SignUpRequestDTO(userNameWithSpaces);

            // When
            Set<ConstraintViolation<SignUpRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(3); // @Size + 두 개의 @AssertTrue
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactlyInAnyOrder(
                            "사용자 이름은 1자 이상 8자 이하여야 합니다.",
                            "사용자 이름에 유효한 문자가 포함되어야 합니다.",
                            "특수문자는 사용할 수 없습니다."
                    );
        }
    }
}
