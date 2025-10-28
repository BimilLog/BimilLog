package jaeik.bimillog.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * <h2>암호화 유틸리티 클래스</h2>
 * <p>메시지를 AES 알고리즘을 사용하여 암호화하고 복호화하는 기능을 제공하는 클래스</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Component
public class EncryptionUtil {

    @Value("${message.secret}")
    private String KEY;

    /**
     * <h3>메시지 암호화</h3>
     * <p>주어진 메시지를 AES 알고리즘을 사용하여 암호화하고 Base64로 인코딩하여 반환</p>
     *
     * @param message 암호화할 메시지
     * @return 암호화된 메시지 (Base64 인코딩)
     * @throws Exception 암호화 과정에서 발생할 수 있는 예외
     */
    public String encrypt(String message) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * <h3>메시지 복호화</h3>
     * <p>주어진 Base64로 인코딩된 암호화된 메시지를 AES 알고리즘을 사용하여 복호화하고 원래 메시지를 반환</p>
     *
     * @param encrypted 암호화된 메시지 (Base64 인코딩)
     * @return 복호화된 원래 메시지
     * @throws Exception 복호화 과정에서 발생할 수 있는 예외
     */
    public String decrypt(String encrypted) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
