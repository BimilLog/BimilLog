package jaeik.bimillog.domain.auth.dto;

import jaeik.bimillog.domain.member.entity.SocialProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>SocialLoginRequestDTO 검증 테스트</h2>
 * <p>소셜 로그인 요청 DTO의 유효성 검증 로직을 테스트합니다.</p>
 * <p>SocialProvider enum 사용으로 타입 안전성 확보</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@DisplayName("SocialLoginRequestDTO 검증 테스트")
@Tag("unit")
class SocialLoginRequestDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("유효한 요청 테스트")
    class ValidRequestTests {

        @Test
        @DisplayName("모든 필드가 유효한 경우 - KAKAO")
        void shouldPass_WhenAllFieldsValid_KAKAO() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(SocialProvider.KAKAO, "valid-code", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
            assertThat(request.getProvider()).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("모든 필드가 유효한 경우 - NAVER")
        void shouldPass_WhenAllFieldsValid_NAVER() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(SocialProvider.NAVER, "valid-code", "state");

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
            assertThat(request.getProvider()).isEqualTo(SocialProvider.NAVER);
        }

        @Test
        @DisplayName("모든 필드가 유효한 경우 - GOOGLE")
        void shouldPass_WhenAllFieldsValid_GOOGLE() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(SocialProvider.GOOGLE, "valid-code", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
            assertThat(request.getProvider()).isEqualTo(SocialProvider.GOOGLE);
        }
    }

    @Nested
    @DisplayName("Provider 검증 테스트")
    class ProviderValidationTests {

        @Test
        @DisplayName("provider가 null인 경우 - @NotNull 검증 실패")
        void shouldFail_WhenProviderIsNull() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(null, "valid-code", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("소셜 제공자는 필수입니다.");
        }
    }

    @Nested
    @DisplayName("Code 검증 테스트")
    class CodeValidationTests {

        @Test
        @DisplayName("code가 null인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenCodeIsNull() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(SocialProvider.KAKAO, null, null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("인증 코드는 필수입니다.");
        }

        @Test
        @DisplayName("code가 빈 문자열인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenCodeIsEmpty() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(SocialProvider.KAKAO, "", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("인증 코드는 필수입니다.");
        }

        @Test
        @DisplayName("code가 공백으로만 구성된 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenCodeIsBlank() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(SocialProvider.KAKAO, "   ", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("인증 코드는 필수입니다.");
        }
    }

    @Nested
    @DisplayName("state 파라미터 테스트")
    class StateParameterTests {

        @Test
        @DisplayName("state가 null이어도 검증 통과 (optional)")
        void shouldPass_WhenStateIsNull() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(SocialProvider.KAKAO, "valid-code", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("state가 있어도 검증 통과")
        void shouldPass_WhenStateIsPresent() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(SocialProvider.NAVER, "valid-code", "test-state");

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
            assertThat(request.getState()).isEqualTo("test-state");
        }
    }
}
