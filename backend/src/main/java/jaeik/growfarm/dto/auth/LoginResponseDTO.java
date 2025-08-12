package jaeik.growfarm.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>카카오 로그인 시 반환되는 응답 클래스</h2>
 * <p>기존 유저와 신규 유저를 구별하는 역할을 한다.</p>
 * <p>카카오 로그인 API의 타입 안전성을 강화 시키기 위해 도입했다.</p>
 * <p>정적 팩토리 메서드 방식을 활용했다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class LoginResponseDTO<T> {

    private LoginType type;
    private T data;

    /**
     * <h3>LoginResponse 생성자</h3>
     *
     * <p>
     * 로그인 타입과 데이터를 받아 LoginResponse 객체를 생성한다.
     * </p>
     *
     * @param loginType 로그인 타입 (기존 사용자/신규 사용자)
     * @param data      응답 데이터
     * @author Jaeik
     * @since 2.0.0
     */
    public LoginResponseDTO(LoginType loginType, T data) {
        this.type = loginType;
        this.data = data;
    }

    /**
     * <h3>로그인 타입 열거형</h3>
     * <p>
     * 기존 사용자와 신규 사용자를 구분하는 열거형
     * </p>
     */
    public enum LoginType {
        EXISTING_USER, NEW_USER, NEW_USER_REGISTERED
    }

    /**
     * <h3>기존 사용자 로그인 응답 생성</h3>
     *
     * <p>
     * 기존 사용자의 로그인 응답을 생성한다.
     * </p>
     *
     * @param cookies JWT 쿠키 리스트
     * @return 기존 사용자 로그인 응답
     * @author Jaeik
     * @since 2.0.0
     */
    public static LoginResponseDTO<List<ResponseCookie>> existingUser(List<ResponseCookie> cookies) {
        return new LoginResponseDTO<>(LoginType.EXISTING_USER, cookies);
    }

    /**
     * <h3>신규 사용자 로그인 응답 생성</h3>
     *
     * <p>
     * 신규 사용자의 로그인 응답을 생성한다.
     * </p>
     *
     * @param uuid 임시 쿠키에 사용할 UUID
     * @return 신규 사용자 로그인 응답
     * @author Jaeik
     * @since 2.0.0
     */
    public static LoginResponseDTO<ResponseCookie> newUser(String uuid) {
        ResponseCookie cookie = getTempCookie(uuid);
        return new LoginResponseDTO<>(LoginType.NEW_USER, cookie);
    }

    /**
     * <h3>임시 쿠키 생성</h3>
     *
     * <p>
     * 신규 사용자의 UUID를 담은 임시 쿠키를 생성한다.
     * </p>
     *
     * @param uuid 임시 쿠키에 사용할 UUID
     * @return ResponseCookie 임시 쿠키
     * @author Jaeik
     * @since 2.0.0
     */
    public static ResponseCookie getTempCookie(String uuid) {
        return ResponseCookie.from("temp", uuid)
                .path("/")
                .maxAge(300)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .build();
    }
}
