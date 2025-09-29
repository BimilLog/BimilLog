package jaeik.bimillog.adapter.in.auth.dto;

import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.in.auth.dto.SocialLoginRequestDTO;
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
@Tag("test")
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
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "valid-code", "fcm-TemporaryToken-123");

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("FCM 토큰이 null인 경우 - 검증 통과")
        void shouldPass_WhenFcmTokenIsNull() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "valid-code", null);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("FCM 토큰이 빈 문자열인 경우 - 검증 통과")
        void shouldPass_WhenFcmTokenIsEmpty() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "valid-code", "");

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
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(null, "valid-code", "fcm-TemporaryToken");

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
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("", "valid-code", "fcm-TemporaryToken");

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
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("INVALID_PROVIDER", "valid-code", "fcm-TemporaryToken");

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
            SocialLoginRequestDTO request1 = new SocialLoginRequestDTO("kakao", "valid-code", "fcm-TemporaryToken");
            SocialLoginRequestDTO request2 = new SocialLoginRequestDTO("Kakao", "valid-code", "fcm-TemporaryToken");
            SocialLoginRequestDTO request3 = new SocialLoginRequestDTO("KAKAO", "valid-code", "fcm-TemporaryToken");

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
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", null, "fcm-TemporaryToken");

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
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "", "fcm-TemporaryToken");

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
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "   ", "fcm-TemporaryToken");

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
    @DisplayName("FCM 토큰 검증 테스트")
    class FcmTokenValidationTests {

        @Test
        @DisplayName("유효한 FCM 토큰 형식 - 검증 통과")
        void shouldPass_WhenFcmTokenIsValidFormat() {
            // Given
            String validFcmToken = "eKpF_Sz6RkuAia7gFMsq8Q:APA91bGHrB9UdPEqFkdBxJ8w7bGXxkgH_UzZ5pMCMvQt9YhJqVb2";
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "valid-code", validFcmToken);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("FCM 토큰에 허용되지 않은 문자가 포함된 경우 - 검증 통과")
        void shouldPass_WhenFcmTokenContainsInvalidCharacters() {
            // Given
            String invalidFcmToken = "invalid@TemporaryToken#with$special%chars";
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "valid-code", invalidFcmToken);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty(); // 현재 DTO에서는 형식 검증을 수행하지 않음
        }

        @Test
        @DisplayName("FCM 토큰이 너무 짧은 경우 - 검증 통과")
        void shouldPass_WhenFcmTokenIsTooShort() {
            // Given
            String shortFcmToken = "short";
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "valid-code", shortFcmToken);

            // When
            Set<ConstraintViolation<SocialLoginRequestDTO>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty(); // 현재 DTO에서는 길이 검증을 수행하지 않음
        }
    }

    @Nested
    @DisplayName("getSocialProvider() 메서드 테스트")
    class GetSocialProviderTests {

        @Test
        @DisplayName("유효한 provider로 SocialProvider 열거형 반환 - KAKAO")
        void shouldReturnSocialProvider_WhenProviderIsKAKAO() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "valid-code", "fcm-TemporaryToken");

            // When
            SocialProvider result = request.getSocialProvider();

            // Then
            assertThat(result).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("대소문자 구분 없이 SocialProvider 반환 - kakao")
        void shouldReturnSocialProvider_WhenProviderIsLowerCase() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("kakao", "valid-code", "fcm-TemporaryToken");

            // When
            SocialProvider result = request.getSocialProvider();

            // Then
            assertThat(result).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("유효하지 않은 provider로 호출 시 IllegalArgumentException 발생")
        void shouldThrowException_WhenProviderIsInvalid() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO("INVALID_PROVIDER", "valid-code", "fcm-TemporaryToken");

            // When & Then
            assertThatThrownBy(request::getSocialProvider)
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null provider로 호출 시 NullPointerException 발생")
        void shouldThrowException_WhenProviderIsNull() {
            // Given
            SocialLoginRequestDTO request = new SocialLoginRequestDTO(null, "valid-code", "fcm-TemporaryToken");

            // When & Then
            assertThatThrownBy(request::getSocialProvider)
                    .isInstanceOf(NullPointerException.class);
        }
    }
}