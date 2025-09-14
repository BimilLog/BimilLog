package jaeik.bimillog.e2e.scenarios;

import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.base.E2ETestConfig;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * 인증 시나리오 E2E 테스트
 * - 카카오 OAuth 로그인/로그아웃
 * - 회원가입 프로세스
 * - JWT 토큰 관리
 * - 인증 보호 페이지 접근
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("인증 시나리오 E2E 테스트")
public class AuthScenarioTest extends BaseE2ETest {

    @BeforeEach
    void setup() {
        E2ETestConfig.ensureFrontendReady();
        navigateToFrontend("/");
    }

    @Test
    @Order(1)
    @DisplayName("01. 카카오 OAuth 로그인 프로세스")
    void testKakaoOAuthLogin() {
        // TODO: 카카오 OAuth 로그인 모킹 또는 테스트 계정 사용
        // 1. 로그인 페이지 이동
        // 2. 카카오 로그인 버튼 클릭
        // 3. OAuth 콜백 처리
        // 4. 로그인 성공 확인
    }

    @Test
    @Order(2)
    @DisplayName("02. 신규 사용자 회원가입 - 닉네임 설정")
    void testSignupWithNickname() {
        // TODO: 회원가입 플로우
        // 1. 카카오 로그인 후 최초 접속
        // 2. 닉네임 설정 페이지 리다이렉트 확인
        // 3. 닉네임 중복 체크
        // 4. 닉네임 설정 완료
    }

    @Test
    @Order(3)
    @DisplayName("03. 로그아웃 및 세션 종료")
    void testLogout() {
        // TODO: 로그아웃 테스트
        // 1. 로그인 상태에서 시작
        // 2. 로그아웃 버튼 클릭
        // 3. 세션 쿠키 제거 확인
        // 4. 로그인 페이지로 리다이렉트 확인
    }

    @Test
    @Order(4)
    @DisplayName("04. 인증 보호 페이지 접근 제한")
    void testProtectedPageAccess() {
        // TODO: 보호된 페이지 접근 테스트
        // 1. 비로그인 상태에서 /mypage 접근
        // 2. 로그인 페이지로 리다이렉트 확인
        // 3. 로그인 후 원래 페이지로 리다이렉트
    }

    @Test
    @Order(5)
    @DisplayName("05. JWT 토큰 갱신")
    void testTokenRefresh() {
        // TODO: 토큰 갱신 테스트
        // 1. 로그인 후 액세스 토큰 확인
        // 2. 토큰 만료 시뮬레이션
        // 3. API 호출 시 자동 갱신 확인
    }

    @Test
    @Order(6)
    @DisplayName("06. 동시 로그인 세션 관리")
    void testConcurrentSessions() {
        // TODO: 다중 세션 테스트
        // 1. 첫 번째 브라우저에서 로그인
        // 2. 두 번째 브라우저에서 동일 계정 로그인
        // 3. 두 세션 모두 유효한지 확인
    }

    @Test
    @Order(7)
    @DisplayName("07. 소셜 로그인 연동 해제")
    void testSocialLoginDisconnect() {
        // TODO: 카카오 연동 해제
        // 1. 설정 페이지 이동
        // 2. 카카오 연동 해제 버튼 클릭
        // 3. 확인 다이얼로그 처리
        // 4. 연동 해제 후 로그아웃 확인
    }
}