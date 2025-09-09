package jaeik.bimillog.infrastructure.adapter.auth.out.social;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;
import reactor.core.publisher.Mono;

/**
 * <h2>소셜 로그인 전략 인터페이스</h2>
 * <p>
 * Strategy 패턴을 적용한 소셜 로그인 전략의 공통 인터페이스입니다.
 * </p>
 * <p>
 * 각 소셜 로그인 제공자(카카오, 네이버 등)별로 구현되어야 하는 핵심 작업들을 정의합니다.
 * OAuth 2.0 인증 플로우, 사용자 정보 조회, 계정 연결 해제, 로그아웃 등의 기능을 포함합니다.
 * </p>
 * <p>
 * 이 인터페이스가 존재하는 이유: 각 소셜 로그인 제공자마다 API 스펙, 인증 방식, 응답 형식이 다르기 때문에
 * 각 제공자에 특화된 구현을 캁싐화하고, 동시에 새로운 제공자 추가 시 이 인터페이스만 구현하면 되도록 하는 확장 가능한 아키텍처를 제공합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginStrategy {

    /**
     * <h3>전략별 로그인 결과 레코드</h3>
     * <p>Strategy 레벨에서의 소셜 로그인 처리 결과를 이동가능한 데이터로 계층해주는 레코드 클래스입니다.</p>
     * <p>각 전략 구현체에서 소셜 API 호출 결과로 획득한 데이터를 전달하기 위해 내부적으로 사용됩니다.</p>
     * <p>소셜 로그인 인프라스트럭처 데이터를 도메인 모델로 변환하여 전달하므로 의존성 역전 원칙을 준수합니다.</p>
     *
     * @param userProfile 소셜 사용자 프로필 (도메인 모델)
     * @param token 소셜 로그인에서 획득한 토큰 정보 (도메인 모델)
     * @author Jaeik
     * @since 2.0.0
     */
    record StrategyLoginResult(LoginResult.SocialUserProfile userProfile, Token token) {}

    /**
     * <h3>소셜 로그인 인증 처리</h3>
     * <p>소셜 로그인 OAuth 2.0 인증 코드를 사용하여 전체 소셜 로그인 인증 플로우를 처리합니다.</p>
     * <p>소셜 로그인 요청 시 제공자별 인증 코드를 받아 처리하기 위해 SocialAdapter에서 호출합니다.</p>
     * <p>내부적으로 토큰 발급 → 사용자 정보 조회 순으로 처리하여 StrategyLoginResult로 결과를 반환합니다.</p>
     *
     * @param code 소셜 로그인 OAuth 2.0 인증 코드
     * @return Mono<StrategyLoginResult> 소셜 로그인 결과 (비동기 스트림)
     * @author Jaeik
     * @since 2.0.0
     */
    Mono<StrategyLoginResult> login(String code);

    /**
     * <h3>소셜 계정 연결 해제</h3>
     * <p>제공자별 API를 사용하여 특정 사용자의 소셜 계정 연결을 완전히 해제합니다.</p>
     * <p>회원 탈퇴 처리 시 소셜 계정과의 연결을 끊기 위해 SocialAdapter에서 호출합니다.</p>
     * <p>각 제공자별로 관리자 API 또는 사용자 API를 사용하여 연결 해제 요청을 전송합니다.</p>
     *
     * @param socialId 연결 해제할 소셜 사용자 식별자
     * @return Mono<Void> 연결 해제 결과 (비동기 스트림)
     * @author Jaeik
     * @since 2.0.0
     */
    Mono<Void> unlink(String socialId);

    /**
     * <h3>소셜 로그아웃 처리</h3>
     * <p>제공자별 API를 사용하여 사용자의 소셜 계정 세션을 종료시킵니다.</p>
     * <p>로그아웃 처리 시 소셜 계정에서도 완전하게 로그아웃되도록 하기 위해 SocialAdapter에서 호출합니다.</p>
     * <p>각 제공자에서 제공하는 로그아웃 API를 사용하여 소셜 계정 세션을 종료시킵니다.</p>
     *
     * @param accessToken 소셜 로그인 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    void logout(String accessToken);

    /**
     * <h3>소셜 로그인 제공자 식별자 반환</h3>
     * <p>현재 Strategy 구현체가 처리하는 소셜 로그인 제공자 타입을 반환합니다.</p>
     * <p>Strategy 패턴 구현에서 각 전략을 구별하고 적절한 전략을 선택하기 위해 SocialAdapter에서 호출합니다.</p>
     * <p>SocialAdapter에서 EnumMap을 구성할 때 이 메서드의 반환값을 키로 사용하여 전략 매핑을 수행합니다.</p>
     *
     * @return SocialProvider 소셜 로그인 제공자 식별자
     * @author Jaeik
     * @since 2.0.0
     */
    SocialProvider getProvider();
}
