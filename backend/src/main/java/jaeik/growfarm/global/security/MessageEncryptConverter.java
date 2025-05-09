package jaeik.growfarm.global.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

@Converter(autoApply = false)
@RequiredArgsConstructor
public class MessageEncryptConverter implements AttributeConverter<String, String> {

    private final EncryptionUtil encryptionUtil;

    @Override
    public String convertToDatabaseColumn(String attribute) {

        try {
            if (attribute == null || attribute.isEmpty()) return attribute;
            return encryptionUtil.encrypt(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

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
