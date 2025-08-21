package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.TokenDTO;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>인증 데이터 관리 포트</h2>
 * <p>사용자 데이터 저장, 업데이트, 삭제를 위한 포트</p>
 * <p>CQRS 패턴에 따른 명령 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SaveUserPort {

    /**
     * <h3>기존 사용자 로그인 처리 및 쿠키 생성</h3>
     * <p>기존 사용자 정보를 업데이트하고 JWT가 삽입된 쿠키를 생성합니다.</p>
     *
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @param fcmToken FCM 토큰
     * @return JWT가 삽입된 쿠키 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    List<ResponseCookie> handleExistingUserLogin(SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken);

    /**
     * <h3>신규 사용자 정보 저장</h3>
     *
     * @param userName 사용자 닉네임
     * @param uuid     임시 데이터 UUID
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @param fcmToken FCM 토큰
     * @return JWT가 삽입된 쿠키 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    List<ResponseCookie> saveNewUser(String userName, String uuid, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken);



}