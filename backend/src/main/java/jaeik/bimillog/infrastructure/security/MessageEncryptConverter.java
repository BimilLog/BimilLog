package jaeik.bimillog.infrastructure.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

/**
 * <h2>메시지 암호화 변환기</h2>
 * <p>데이터베이스에 저장되는 메시지를 암호화하고, 조회 시 복호화하는 변환기</p>
 * <p>암호화 및 복호화는 {@link EncryptionUtil}을 사용</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Converter(autoApply = false)
@RequiredArgsConstructor
public class MessageEncryptConverter implements AttributeConverter<String, String> {

    private final EncryptionUtil encryptionUtil;

    /**
     * <h3>데이터베이스 컬럼에 저장할 때 암호화</h3>
     * <p>문자열을 암호화하여 데이터베이스에 저장</p>
     *
     * @param attribute 암호화할 문자열
     * @return 암호화된 문자열
     * @since 1.0.0
     * @author Jaeik
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {

        try {
            if (attribute == null || attribute.isEmpty()) return attribute;
            return encryptionUtil.encrypt(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    /**
     * <h3>데이터베이스에서 조회할 때 복호화</h3>
     * <p>암호화된 문자열을 복호화하여 반환</p>
     *
     * @param dbData 데이터베이스에서 조회한 암호화된 문자열
     * @return 복호화된 문자열
     * @since 1.0.0
     * @author Jaeik
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isEmpty()) return dbData;
            return encryptionUtil.decrypt(dbData);
        } catch (Exception e) {
            throw new RuntimeException("Decryption error", e);
        }
    }
}
