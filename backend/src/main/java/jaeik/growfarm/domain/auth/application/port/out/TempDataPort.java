package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.domain.user.entity.TokenVO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.TemporaryUserDataDTO;
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
     * @param tokenVO 토큰 정보
     */
    void saveTempData(String uuid, SocialLoginUserData userData, TokenVO tokenVO);

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     *
     * @param uuid UUID 키
     * @return 임시 사용자 데이터
     */
    Optional<TemporaryUserDataDTO> getTempData(String uuid);

    /**
     * <h3>임시 사용자 ID 쿠키 생성</h3>
     * <p>신규 회원가입 시 사용자의 임시 UUID를 담는 쿠키를 생성</p>
     *
     * @param uuid 임시 사용자 ID
     * @return 임시 사용자 ID 쿠키
     * @since 2.0.0
     * @author Jaeik
     */
    ResponseCookie createTempCookie(String uuid);
}