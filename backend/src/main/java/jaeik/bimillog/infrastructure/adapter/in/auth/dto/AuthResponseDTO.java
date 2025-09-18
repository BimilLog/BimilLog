package jaeik.bimillog.infrastructure.adapter.in.auth.dto;

import java.util.Map;

/**
 * <h2>인증 응답 DTO</h2>
 * <p>인증 관련 API의 통합 응답 모델</p>
 * <p>타입 안전성을 보장하면서 기존 클라이언트 호환성을 유지</p>
 *
 * @param status 응답 상태 (NEW_USER, EXISTING_USER, SUCCESS)
 * @param uuid 신규 사용자의 경우 임시 UUID
 * @param data 추가 응답 데이터
 * @author Jaeik
 * @version 2.0.0
 */
public record AuthResponseDTO(
    String status,
    String uuid,
    Map<String, Object> data
) {
    /**
     * <h3>신규 사용자 응답 생성</h3>
     * <p>회원가입이 필요한 신규 사용자를 위한 응답</p>
     *
     * @param uuid 임시 사용자 UUID
     * @return 신규 사용자 응답
     */
    public static AuthResponseDTO newUser(String uuid) {
        return new AuthResponseDTO("NEW_USER", uuid, Map.of("uuid", uuid));
    }
    
    /**
     * <h3>기존 사용자 로그인 성공 응답 생성</h3>
     * <p>이미 등록된 사용자를 위한 응답</p>
     *
     * @return 기존 사용자 응답
     */
    public static AuthResponseDTO existingUser() {
        return new AuthResponseDTO("EXISTING_USER", null, Map.of("message", "LOGIN_SUCCESS"));
    }
    
    /**
     * <h3>일반 성공 응답 생성</h3>
     * <p>회원가입, 로그아웃, 탈퇴 등의 성공 응답</p>
     *
     * @param message 성공 메시지
     * @return 성공 응답
     */
    public static AuthResponseDTO success(String message) {
        return new AuthResponseDTO("SUCCESS", null, Map.of("message", message));
    }
}