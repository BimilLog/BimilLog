package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.user.entity.SocialProvider;

/**
 * <h2>소셜 전략 레지스트리 포트</h2>
 * <p>소셜 제공자별 로그인 전략을 관리하고 조회하는 레지스트리 포트입니다.</p>
 * <p>전략 패턴 구현체들을 중앙에서 관리하여 동적 전략 선택 지원</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialStrategyRegistryPort {

    /**
     * <h3>제공자별 전략 조회</h3>
     * <p>소셜 제공자에 해당하는 로그인 전략 구현체를 반환합니다.</p>
     * <p>{@link SocialService}에서 소셜 로그인 처리 시 전략 선택을 위해 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO, GOOGLE, NAVER 등)
     * @return 해당 제공자의 로그인 전략 구현체
     * @throws IllegalArgumentException 지원하지 않는 제공자인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    SocialStrategyPort getStrategy(SocialProvider provider);
}