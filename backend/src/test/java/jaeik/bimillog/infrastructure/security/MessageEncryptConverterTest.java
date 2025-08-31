package jaeik.bimillog.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

/**
 * <h2>MessageEncryptConverter 예외 처리 테스트</h2>
 * <p>MessageEncryptConverter의 암호화/복호화 실패 시 RuntimeException 처리를 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class MessageEncryptConverterTest {

    @Mock
    private EncryptionUtil encryptionUtil;

    private MessageEncryptConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MessageEncryptConverter(encryptionUtil);
    }

    @Test
    @DisplayName("정상 케이스 - 암호화 성공")
    void shouldEncryptSuccessfully_WhenValidMessageProvided() throws Exception {
        // Given: 정상적인 메시지와 암호화 결과
        String originalMessage = "안녕하세요! 테스트 메시지입니다.";
        String encryptedMessage = "encrypted_message_base64";
        given(encryptionUtil.encrypt(originalMessage)).willReturn(encryptedMessage);

        // When: 암호화 실행
        String result = converter.convertToDatabaseColumn(originalMessage);

        // Then: 암호화된 결과 반환
        assertThat(result).isEqualTo(encryptedMessage);
    }

    @Test
    @DisplayName("정상 케이스 - 복호화 성공")
    void shouldDecryptSuccessfully_WhenValidEncryptedDataProvided() throws Exception {
        // Given: 암호화된 데이터와 복호화 결과
        String encryptedData = "encrypted_message_base64";
        String decryptedMessage = "안녕하세요! 테스트 메시지입니다.";
        given(encryptionUtil.decrypt(encryptedData)).willReturn(decryptedMessage);

        // When: 복호화 실행
        String result = converter.convertToEntityAttribute(encryptedData);

        // Then: 복호화된 결과 반환
        assertThat(result).isEqualTo(decryptedMessage);
    }

    @Test
    @DisplayName("암호화 실패 처리 - RuntimeException 발생")
    void shouldThrowRuntimeException_WhenEncryptionFails() throws Exception {
        // Given: 암호화 시 예외 발생
        String originalMessage = "테스트 메시지";
        Exception encryptionError = new Exception("Encryption key is invalid");
        willThrow(encryptionError).given(encryptionUtil).encrypt(originalMessage);

        // When & Then: RuntimeException으로 래핑되어 발생
        assertThatThrownBy(() -> converter.convertToDatabaseColumn(originalMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Encryption error")
                .hasCause(encryptionError);
    }

    @Test
    @DisplayName("복호화 실패 처리 - RuntimeException 발생")
    void shouldThrowRuntimeException_WhenDecryptionFails() throws Exception {
        // Given: 복호화 시 예외 발생
        String encryptedData = "invalid_encrypted_data";
        Exception decryptionError = new Exception("Invalid encrypted data format");
        willThrow(decryptionError).given(encryptionUtil).decrypt(encryptedData);

        // When & Then: RuntimeException으로 래핑되어 발생
        assertThatThrownBy(() -> converter.convertToEntityAttribute(encryptedData))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Decryption error")
                .hasCause(decryptionError);
    }

    @Test
    @DisplayName("Null 처리 - null 암호화")
    void shouldReturnNull_WhenEncryptingNullValue() {
        // Given: null 값

        // When: null 암호화
        String result = converter.convertToDatabaseColumn(null);

        // Then: null 반환 (암호화 시도 없음)
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Null 처리 - null 복호화")
    void shouldReturnNull_WhenDecryptingNullValue() {
        // Given: null 값

        // When: null 복호화
        String result = converter.convertToEntityAttribute(null);

        // Then: null 반환 (복호화 시도 없음)
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("빈 문자열 처리 - 빈 문자열 암호화")
    void shouldReturnEmptyString_WhenEncryptingEmptyString() {
        // Given: 빈 문자열
        String emptyString = "";

        // When: 빈 문자열 암호화
        String result = converter.convertToDatabaseColumn(emptyString);

        // Then: 빈 문자열 반환 (암호화 시도 없음)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열 처리 - 빈 문자열 복호화")
    void shouldReturnEmptyString_WhenDecryptingEmptyString() {
        // Given: 빈 문자열
        String emptyString = "";

        // When: 빈 문자열 복호화
        String result = converter.convertToEntityAttribute(emptyString);

        // Then: 빈 문자열 반환 (복호화 시도 없음)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("암호화 예외 타입 - IllegalArgumentException 처리")
    void shouldWrapIllegalArgumentException_WhenEncryptionFails() throws Exception {
        // Given: IllegalArgumentException 발생하는 암호화
        String originalMessage = "테스트 메시지";
        IllegalArgumentException encryptionError = new IllegalArgumentException("Invalid key length");
        willThrow(encryptionError).given(encryptionUtil).encrypt(originalMessage);

        // When & Then: RuntimeException으로 래핑
        assertThatThrownBy(() -> converter.convertToDatabaseColumn(originalMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Encryption error")
                .hasCause(encryptionError);
    }

    @Test
    @DisplayName("복호화 예외 타입 - IllegalStateException 처리")
    void shouldWrapIllegalStateException_WhenDecryptionFails() throws Exception {
        // Given: IllegalStateException 발생하는 복호화
        String encryptedData = "corrupted_data";
        IllegalStateException decryptionError = new IllegalStateException("Cipher not initialized");
        willThrow(decryptionError).given(encryptionUtil).decrypt(encryptedData);

        // When & Then: RuntimeException으로 래핑
        assertThatThrownBy(() -> converter.convertToEntityAttribute(encryptedData))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Decryption error")
                .hasCause(decryptionError);
    }

    @Test
    @DisplayName("특수 문자 암호화 실패 - RuntimeException 처리")
    void shouldHandleEncryptionFailure_WithSpecialCharacters() throws Exception {
        // Given: 특수 문자 포함 메시지의 암호화 실패
        String messageWithSpecialChars = "🌟 안녕하세요! @#$%^&*()_+ 테스트입니다.";
        Exception encryptionError = new Exception("Special character encoding error");
        willThrow(encryptionError).given(encryptionUtil).encrypt(messageWithSpecialChars);

        // When & Then: RuntimeException 발생
        assertThatThrownBy(() -> converter.convertToDatabaseColumn(messageWithSpecialChars))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Encryption error")
                .hasCause(encryptionError);
    }

    @Test
    @DisplayName("긴 텍스트 복호화 실패 - RuntimeException 처리")
    void shouldHandleDecryptionFailure_WithLongText() throws Exception {
        // Given: 긴 암호화된 텍스트의 복호화 실패
        String longEncryptedData = "very_long_encrypted_data_that_might_cause_buffer_overflow".repeat(10);
        Exception decryptionError = new Exception("Buffer overflow during decryption");
        willThrow(decryptionError).given(encryptionUtil).decrypt(longEncryptedData);

        // When & Then: RuntimeException 발생
        assertThatThrownBy(() -> converter.convertToEntityAttribute(longEncryptedData))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Decryption error")
                .hasCause(decryptionError);
    }

    @Test
    @DisplayName("정상 처리 - 암호화/복호화 순환 테스트 시뮬레이션")
    void shouldSimulateSuccessfulEncryptionDecryptionCycle() throws Exception {
        // Given: 원본 메시지와 암호화/복호화 시뮬레이션
        String originalMessage = "롤링페이퍼 메시지 테스트입니다.";
        String encryptedData = "simulated_encrypted_base64_data";
        given(encryptionUtil.encrypt(originalMessage)).willReturn(encryptedData);
        given(encryptionUtil.decrypt(encryptedData)).willReturn(originalMessage);

        // When: 암호화 후 복호화
        String encrypted = converter.convertToDatabaseColumn(originalMessage);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then: 원본 메시지 복원
        assertThat(encrypted).isEqualTo(encryptedData);
        assertThat(decrypted).isEqualTo(originalMessage);
    }
}