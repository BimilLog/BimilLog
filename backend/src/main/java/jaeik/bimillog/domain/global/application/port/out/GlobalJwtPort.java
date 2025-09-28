package jaeik.bimillog.domain.global.application.port.out;

import jaeik.bimillog.domain.auth.application.service.BlacklistService;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.infrastructure.adapter.out.global.GlobalCookieAdapter;
import jaeik.bimillog.infrastructure.filter.JwtFilter;

/**
 * <h2>JWT 토큰 관리 포트</h2>
 * <p>JWT 토큰의 생성, 검증, 정보 추출, 해시 생성을 담당하는 포트입니다.</p>
 * <p>토큰 생성/검증, 사용자 정보 추출, 토큰 해시 생성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface GlobalJwtPort {

    /**
     * <h3>JWT 액세스 토큰 생성</h3>
     * <p>사용자 정보를 포함한 JWT 액세스 토큰을 생성합니다. 유효기간은 1시간입니다.</p>
     * <p>{@link GlobalCookieAdapter}에서 로그인 성공 후 액세스 토큰 발급 시 호출됩니다.</p>
     *
     * @param userDetail 클라이언트용 사용자 정보 DTO
     * @return 생성된 JWT 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    String generateAccessToken(ExistingUserDetail userDetail);

    /**
     * <h3>JWT 리프레시 토큰 생성</h3>
     * <p>사용자 ID와 토큰 ID를 포함한 JWT 리프레시 토큰을 생성합니다. 유효기간은 30일입니다.</p>
     * <p>{@link GlobalCookieAdapter}에서 로그인 성공 후 리프레시 토큰 발급 시 호출됩니다.</p>
     *
     * @param userDetail 클라이언트용 사용자 정보 DTO
     * @return 생성된 JWT 리프레시 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    String generateRefreshToken(ExistingUserDetail userDetail);

    /**
     * <h3>JWT 토큰 유효성 검사</h3>
     * <p>주어진 JWT 토큰이 유효한지 검사합니다.</p>
     * <p>{@link JwtFilter}에서 모든 인증 요청 시 토큰 유효성 검증을 위해 호출됩니다.</p>
     *
     * @param token 검증할 JWT 토큰
     * @return 토큰이 유효하면 true, 유효하지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean validateToken(String token);

    /**
     * <h3>JWT 액세스 토큰에서 사용자 정보 추출</h3>
     * <p>JWT 액세스 토큰에서 사용자 정보를 추출하여 UserDetail DTO로 변환합니다.</p>
     * <p>{@link JwtFilter}에서 인증된 사용자의 정보를 SecurityContext에 설정하기 위해 호출됩니다.</p>
     *
     * @param jwtAccessToken JWT 액세스 토큰
     * @return 사용자 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    ExistingUserDetail getUserInfoFromToken(String jwtAccessToken);

    /**
     * <h3>JWT 리프레시 토큰에서 토큰 ID 추출</h3>
     * <p>JWT 리프레시 토큰에서 토큰 ID를 추출합니다.</p>
     * <p>{@link GlobalCookieAdapter}에서 토큰 갱신 시 기존 토큰 ID를 조회하기 위해 호출됩니다.</p>
     *
     * @param jwtRefreshToken JWT 리프레시 토큰
     * @return 토큰 ID
     * @author Jaeik
     * @since 2.0.0
     */
    Long getTokenIdFromToken(String jwtRefreshToken);

    /**
     * <h3>리프레시 토큰 자동 갱신 여부 확인</h3>
     * <p>리프레시 토큰의 남은 만료 시간이 지정된 임계값(일수) 이하인지 확인합니다.</p>
     * <p>15일 이하로 남았을 때 true를 반환하여 새로운 리프레시 토큰 발급을 유도합니다.</p>
     * <p>{@link GlobalCookieAdapter}에서 토큰 갱신 시 리프레시 토큰 재발급 여부 결정을 위해 호출됩니다.</p>
     *
     * @param token JWT 리프레시 토큰
     * @param thresholdDays 임계값(일수)
     * @return 갱신이 필요한 경우 true, 그렇지 않은 경우 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean shouldRefreshToken(String token, long thresholdDays);

    /**
     * <h3>JWT 토큰 해시값 생성</h3>
     * <p>주어진 JWT 토큰 문자열로부터 해시값을 생성합니다.</p>
     * <p>토큰 원본을 직접 저장하지 않고 해시값으로 변환하여 보안을 강화합니다.</p>
     * <p>{@link BlacklistService}에서 토큰 블랙리스트 등록이나 검증 시 호출됩니다.</p>
     *
     * @param token 해시값으로 변환할 JWT 토큰 문자열
     * @return 생성된 토큰 해시값 (SHA-256)
     * @author Jaeik
     * @since 2.0.0
     */
    String generateTokenHash(String token);
}