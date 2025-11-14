package jaeik.bimillog.domain.auth.dto;

import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.auth.dto.SocialLoginRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>SocialLoginRequestDTO 검증 테스트</h2>
 * <p>소셜 로그인 요청 DTO의 유효성 검증 로직을 테스트합니다.</p>
 * <p>Bean Validation과 @AssertTrue 커스텀 검증 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
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
        @DisplayName("모든 필드가 유효한 경우 - 검증 통과")
        void shouldPass_WhenAllFieldsValid() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "valid-code", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Provider 검증 테스트")
    class ProviderValidationTests {

        @Test
        @DisplayName("provider가 null인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenProviderIsNull() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(null, "valid-code", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(2); // @NotBlank + @AssertTrue
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("소셜 제공자는 필수입니다.", "유효하지 않은 소셜 제공자입니다.");
        }

        @Test
        @DisplayName("provider가 빈 문자열인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenProviderIsEmpty() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("", "valid-code", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(2); // @NotBlank + @AssertTrue
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("소셜 제공자는 필수입니다.", "유효하지 않은 소셜 제공자입니다.");
        }

        @Test
        @DisplayName("유효하지 않은 provider인 경우 - @AssertTrue 검증 실패")
        void shouldFail_WhenProviderIsInvalid() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("INVALID_PROVIDER", "valid-code", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("유효하지 않은 소셜 제공자입니다.");
        }

        @Test
        @DisplayName("provider 대소문자 무관하게 검증 - KAKAO")
        void shouldPass_WhenProviderIsCaseInsensitive_KAKAO() {
            // Given
            SocialLoginRequestDTO request1 = new SocialLoginRequestDTO("kakao", "valid-code", null);
            SocialLoginRequestDTO request2 = new SocialLoginRequestDTO("Kakao", "valid-code", null);
            SocialLoginRequestDTO request3 = new SocialLoginRequestDTO("KAKAO", "valid-code", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations1 = validator.validate(request1);
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations2 = validator.validate(request2);
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations3 = validator.validate(request3);

            // Then
            assertThat(violations1).isEmpty();
            assertThat(violations2).isEmpty();
            assertThat(violations3).isEmpty();
        }
    }

    @Nested
    @DisplayName("Code 검증 테스트")
    class CodeValidationTests {

        @Test
        @DisplayName("code가 null인 경우 - @NotBlank 검증 실패")
        void shouldFail_WhenCodeIsNull() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", null, null);

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
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "", null);

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
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "   ", null);

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
    @DisplayName("getSocialProvider() 메서드 테스트")
    class GetSocialProviderTests {

        @Test
        @DisplayName("유효한 provider로 SocialProvider 열거형 반환 - KAKAO")
        void shouldReturnSocialProvider_WhenProviderIsKAKAO() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "valid-code", null);

            // When
            SocialProvider result = request.getSocialProvider();

            // Then
            assertThat(result).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("대소문자 구분 없이 SocialProvider 반환 - kakao")
        void shouldReturnSocialProvider_WhenProviderIsLowerCase() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("kakao", "valid-code", null);

            // When
            SocialProvider result = request.getSocialProvider();

            // Then
            assertThat(result).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("유효하지 않은 provider로 호출 시 IllegalArgumentException 발생")
        void shouldThrowException_WhenProviderIsInvalid() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("INVALID_PROVIDER", "valid-code", null);

            // When & Then
            assertThatThrownBy(request::getSocialProvider)
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null provider로 호출 시 NullPointerException 발생")
        void shouldThrowException_WhenProviderIsNull() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(null, "valid-code", null);

            // When & Then
            assertThatThrownBy(request::getSocialProvider)
                    .isInstanceOf(NullPointerException.class);
        }
    }
}