package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.SignUpService;
import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.auth.entity.SocialAuthData;
import jaeik.bimillog.domain.user.entity.Token;
import org.springframework.http.ResponseCookie;

import java.util.Optional;

/**
 * <h2>Redis 사용자 데이터 관리 포트</h2>
 * <p>소셜 로그인 후 신규 사용자의 임시 데이터를 Redis에 저장하는 포트입니다.</p>
 * <p>임시 데이터 저장/조회/삭제, 임시 쿠키 생성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface RedisUserDataPort {

    /**
     * <h3>임시 사용자 데이터 저장</h3>
     * <p>소셜 로그인 성공 후 신규 사용자의 정보를 임시로 Redis에 저장합니다.</p>
     * <p>회원가입 완료 시까지 소셜 프로필과 토큰 정보를 보관하는데 사용됩니다.</p>
     * <p>{@link SocialService}에서 신규 사용자 소셜 로그인 처리 시 호출됩니다.</p>
     *
     * @param uuid 임시 데이터 식별을 위한 고유 UUID 키
     * @param userProfile 소셜 플랫폼에서 가져온 사용자 프로필 정보
     * @param token JWT 토큰 정보
     * @param fcmToken Firebase Cloud Messaging 토큰 (푸시 알림용, 선택적)
     * @author Jaeik
     * @since 2.0.0
     */
    void saveTempData(String uuid, SocialAuthData.SocialUserProfile userProfile, Token token, String fcmToken);

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     * <p>UUID를 사용하여 Redis에 저장된 임시 사용자 데이터를 조회합니다.</p>
     * <p>회원가입 완료 시 임시로 저장된 소셜 프로필 정보를 복원하는데 사용됩니다.</p>
     * <p>{@link SignUpService}에서 회원가입 요청 처리 시 임시 데이터 복원을 위해 호출됩니다.</p>
     *
     * @param uuid 조회할 임시 데이터의 UUID 키
     * @return 조회된 임시 사용자 데이터 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<SocialAuthData.TempUserData> getTempData(String uuid);

    /**
     * <h3>임시 사용자 데이터 삭제</h3>
     * <p>UUID를 사용하여 Redis에 저장된 임시 사용자 데이터를 삭제합니다.</p>
     * <p>회원가입 완료 후 불필요해진 임시 데이터를 정리하는데 사용됩니다.</p>
     * <p>회원가입 완료 처리 후 임시 데이터 정리를 위해 호출됩니다.</p>
     *
     * @param uuid 삭제할 임시 데이터의 UUID 키
     * @author Jaeik
     * @since 2.0.0
     */
    void removeTempData(String uuid);

    /**
     * <h3>임시 사용자 ID 쿠키 생성</h3>
     * <p>신규 사용자의 임시 UUID를 브라우저 쿠키로 생성하여 회원가입 플로우를 연결합니다.</p>
     * <p>소셜 로그인 후 회원가입 페이지에서 임시 데이터에 접근하는데 사용됩니다.</p>
     * <p>{@link SocialService}에서 신규 사용자 소셜 로그인 완료 시 호출됩니다.</p>
     *
     * @param uuid 임시 데이터와 연결된 고유 UUID
     * @return 임시 UUID가 포함된 HttpOnly 쿠키
     * @author Jaeik
     * @since 2.0.0
     */
    ResponseCookie createTempCookie(String uuid);
}