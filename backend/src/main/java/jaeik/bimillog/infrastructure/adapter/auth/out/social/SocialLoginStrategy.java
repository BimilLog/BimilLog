package jaeik.bimillog.infrastructure.adapter.auth.out.social;

import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.TokenVO;
import jaeik.bimillog.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import reactor.core.publisher.Mono;

/**
 * <h2>소셜 로그인 전략 인터페이스</h2>
 * <p>소셜 로그인 관련 작업을 정의하는 인터페이스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginStrategy {

    /**
     * <h3>전략별 로그인 결과</h3>
     * <p>Strategy 레벨에서의 로그인 결과를 담는 레코드 클래스</p>
     *
     * @param userData 소셜 사용자 데이터
     * @param token 토큰 정보
     * @since 2.0.0
     * @author Jaeik
     */
    record StrategyLoginResult(SocialLoginUserData userData, TokenVO token) {}

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>소셜 로그인 코드를 받아 사용자 정보를 조회하고 로그인 결과를 반환합니다.</p>
     *
     * @param code 소셜 로그인 코드
     * @return 로그인 결과 (비동기)
     * @since 2.0.0
     * @author Jaeik
     */
    Mono<StrategyLoginResult> login(String code);

    /**
     * <h3>소셜 계정 연결 해제</h3>
     * <p>주어진 소셜 ID에 해당하는 계정의 연결을 해제합니다.</p>
     *
     * @param socialId 소셜 ID
     * @return 연결 해제 결과 (비동기)
     * @since 2.0.0
     * @author Jaeik
     */
    Mono<Void> unlink(String socialId);

    /**
     * <h3>소셜 로그아웃</h3>
     * <p>사용자의 소셜 로그아웃을 처리합니다.</p>
     *
     * @param accessToken 액세스 토큰
     * @since 2.0.0
     * @author Jaeik
     */
    void logout(String accessToken);

    /**
     * <h3>소셜 제공자 정보 조회</h3>
     * <p>현재 소셜 로그인 전략에 해당하는 소셜 제공자 정보를 반환합니다.</p>
     *
     * @return 소셜 제공자 정보
     * @since 2.0.0
     * @author Jaeik
     */
    SocialProvider getProvider();
}
