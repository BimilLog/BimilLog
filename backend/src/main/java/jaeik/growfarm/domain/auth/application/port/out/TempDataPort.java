package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.TemporaryUserDataDTO;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.TokenDTO;
import org.springframework.http.ResponseCookie;

import java.util.Optional;

/**
 * <h2>임시 데이터 관리 포트</h2>
 * <p>신규 사용자의 임시 데이터 관리를 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface TempDataPort {

    /**
     * <h3>임시 사용자 데이터 저장</h3>
     *
     * @param uuid UUID 키
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     */
    void saveTempData(String uuid, SocialLoginUserData userData, TokenDTO tokenDTO);

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     *
     * @param uuid UUID 키
     * @return 임시 사용자 데이터
     */
    Optional<TemporaryUserDataDTO> getTempData(String uuid);

    /**
     * <h3>임시 사용자 데이터 저장 및 쿠키 생성</h3>
     * <p>임시 사용자 데이터를 저장하고 해당 UUID로 쿠키를 생성하여 함께 반환합니다.</p>
     * <p>신규 사용자 회원가입 플로우에서 두 단계 작업을 하나로 통합합니다.</p>
     *
     * @param uuid UUID 키
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @return 생성된 임시 사용자 ID 쿠키
     * @since 2.0.0
     * @author Jaeik
     */
    ResponseCookie saveTempDataAndCreateCookie(String uuid, SocialLoginUserData userData, TokenDTO tokenDTO);
}