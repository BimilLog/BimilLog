package jaeik.growfarm.domain.auth.application.port.in;

import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.dto.auth.LoginResponseDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>인증 로그인 유스케이스</h2>
 * <p>소셜 로그인, 회원가입, 로그아웃, 회원탈퇴 관련 기능</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AuthLoginUseCase {

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>기존 회원은 JWT 쿠키를 반환하고, 신규 회원은 UUID 임시 쿠키를 반환</p>
     *
     * @param provider 소셜 로그인 제공자
     * @param code     프론트에서 반환된 인가 코드
     * @param fcmToken Firebase Cloud Messaging 토큰
     * @return JWT가 삽입된 쿠키 또는 토큰 ID
     */
    LoginResponseDTO<?> processSocialLogin(SocialProvider provider, String code, String fcmToken);

    /**
     * <h3>회원 가입</h3>
     * <p>UUID로 임시 저장된 사용자 정보를 기반으로 회원가입 처리</p>
     *
     * @param userName 사용자가 설정한 닉네임
     * @param uuid     임시 저장된 사용자 정보의 UUID
     * @return JWT가 삽입된 쿠키 리스트
     */
    List<ResponseCookie> signUp(String userName, String uuid);

    /**
     * <h3>로그아웃</h3>
     * <p>소셜 로그아웃, DB 토큰 삭제, SSE 연결 삭제, SecurityContext 클리어 등 모든 로그아웃 처리</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 로그아웃 쿠키 리스트
     */
    List<ResponseCookie> logout(CustomUserDetails userDetails);

    /**
     * <h3>회원탈퇴</h3>
     * <p>사용자의 모든 데이터를 삭제하고 카카오 연결을 해제</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 탈퇴 처리 쿠키 리스트
     */
    List<ResponseCookie> withdraw(CustomUserDetails userDetails);
}