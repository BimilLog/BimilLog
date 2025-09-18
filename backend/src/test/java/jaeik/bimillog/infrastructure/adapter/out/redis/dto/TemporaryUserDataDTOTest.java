package jaeik.bimillog.infrastructure.adapter.out.redis.dto;

import jaeik.bimillog.domain.auth.entity.SocialAuthData;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>임시 사용자 데이터 DTO 테스트</h2>
 * <p>Bean Validation과 @AssertTrue 어노테이션 기반 검증 메서드들을 테스트</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
class TemporaryUserDataDTOTest {

    private Validator validator;
    private SocialAuthData.SocialUserProfile validProfile;
    private Token validToken;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        validProfile = new SocialAuthData.SocialUserProfile(
            "12345", "test@example.com", SocialProvider.KAKAO, "테스트사용자", "http://example.com/profile.jpg"
        );
        validToken = Token.createTemporaryToken("access-token", "refresh-token");
    }

    @Test
    @DisplayName("유효한 데이터로 DTO 생성 시 검증 통과")
    void validData_ShouldPassValidation() {
        // given
        TemporaryUserDataDTO dto = new TemporaryUserDataDTO(validProfile, validToken, "fcm-token");

        // when
        Set<ConstraintViolation<TemporaryUserDataDTO>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("socialUserProfile이 null인 경우 toDomainProfile에서 예외 발생")
    void nullSocialUserProfile_ShouldThrowException() {
        // given
        TemporaryUserDataDTO dto = new TemporaryUserDataDTO(null, validToken, "fcm-token");

        // when & then
        assertThatThrownBy(() -> dto.toDomainProfile())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("socialUserProfile은 null이 될 수 없습니다");
    }

    @Test
    @DisplayName("token이 null인 경우 toDomainProfile에서 예외 발생")
    void nullToken_ShouldThrowException() {
        // given
        TemporaryUserDataDTO dto = new TemporaryUserDataDTO(validProfile, null, "fcm-token");

        // when & then
        assertThatThrownBy(() -> dto.toDomainProfile())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("token은 null이 될 수 없습니다");
    }

    @Test
    @DisplayName("socialUserProfile과 token이 모두 null인 경우 toDomainProfile에서 예외 발생")
    void bothNull_ShouldThrowException() {
        // given
        TemporaryUserDataDTO dto = new TemporaryUserDataDTO(null, null, "fcm-token");

        // when & then
        assertThatThrownBy(() -> dto.toDomainProfile())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("socialUserProfile은 null이 될 수 없습니다");
    }

    @Test
    @DisplayName("fcmToken은 null이어도 검증 통과")
    void nullFcmToken_ShouldPassValidation() {
        // given
        TemporaryUserDataDTO dto = new TemporaryUserDataDTO(validProfile, validToken, null);

        // when
        Set<ConstraintViolation<TemporaryUserDataDTO>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("toDomainProfile 메서드 정상 동작 확인")
    void toDomainProfile_ShouldReturnSocialUserProfile() {
        // given
        TemporaryUserDataDTO dto = new TemporaryUserDataDTO(validProfile, validToken, "fcm-token");

        // when
        SocialAuthData.SocialUserProfile result = dto.toDomainProfile();

        // then
        assertThat(result).isEqualTo(validProfile);
        assertThat(result.socialId()).isEqualTo("12345");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.provider()).isEqualTo(SocialProvider.KAKAO);
    }
}