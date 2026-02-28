package jaeik.bimillog.springboot.h2;

import jaeik.bimillog.domain.auth.dto.SocialLoginRequestDTO;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.repository.AuthTokenRepository;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.notification.dto.FcmTokenRegisterRequestDTO;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import jaeik.bimillog.testutil.config.TestSocialLoginAdapterConfig;
import jaeik.bimillog.testutil.fixtures.AuthTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>인증 명령 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Auth Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL + Redis 환경에서 테스트</p>
 * <p>Mock Social Login 설정을 통해 실제 외부 API 호출 없이 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("인증 명령 컨트롤러 통합 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("springboot-h2")
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginAdapterConfig.class})
class AuthCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Test
    @DisplayName("소셜 로그인 통합 테스트 - 신규 사용자")
    void socialLogin_NewUser_IntegrationTest() throws Exception {
        SocialLoginRequestDTO request = new SocialLoginRequestDTO(
                SocialProvider.KAKAO,
                "new_user_code",
                "test-state"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("소셜 로그인 통합 테스트 - 기존 사용자")
    void socialLogin_ExistingUser_IntegrationTest() throws Exception {
        Member existingMember = TestMembers.createMember(
                "test-social-id-12345",
                "existing-member",
                "existing-member"
        );
        saveMember(existingMember);

        SocialLoginRequestDTO request = new SocialLoginRequestDTO(SocialProvider.KAKAO, "existing-member-code", "test-state");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("로그아웃 통합 테스트 - 성공")
    void logout_IntegrationTest_Success() throws Exception {
        Member testMember = TestMembers.createUnique();
        testMember = saveMember(testMember);

        AuthToken authToken = AuthToken.createToken("refresh-authToken", testMember);
        authToken = authTokenRepository.save(authToken);

        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(testMember, authToken.getId(), null);

        mockMvc.perform(post("/api/auth/logout")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("소셜 로그인 - 잘못된 provider 통합 테스트")
    void socialLogin_InvalidProvider_IntegrationTest() throws Exception {
        String invalidPayload = "{\"provider\":\"INVALID_PROVIDER\",\"code\":\"test-code\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("인증이 필요한 API에 비인증 요청 - 403 Forbidden")
    void authenticatedApiWithoutAuth_Forbidden() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ==================== FCM 토큰 등록 테스트 ====================

    @Test
    @DisplayName("FCM 토큰 등록 통합 테스트 - 성공")
    void registerFcmToken_IntegrationTest_Success() throws Exception {
        // Given: AuthToken 생성
        AuthToken authToken = AuthToken.createToken("refresh-token-123", testMember);
        authToken = authTokenRepository.save(authToken);

        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(
                testMember,
                authToken.getId()
        );

        FcmTokenRegisterRequestDTO request = new FcmTokenRegisterRequestDTO(
                "fcm-test-token-abcdefg1234567890"
        );

        // When: POST /api/auth/fcm
        mockMvc.perform(post("/api/auth/fcm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                // Then: 200 OK
                .andExpect(status().isOk());

        // Then: DB 확인
        AuthToken updatedToken = authTokenRepository.findById(authToken.getId())
                .orElseThrow();
        assertThat(updatedToken.getFcmRegistrationToken())
                .isEqualTo("fcm-test-token-abcdefg1234567890");
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 빈 토큰 - 400 Bad Request")
    void registerFcmToken_EmptyToken_BadRequest() throws Exception {
        // Given: AuthToken 생성
        AuthToken authToken = AuthToken.createToken("refresh-token-456", testMember);
        authToken = authTokenRepository.save(authToken);

        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(
                testMember,
                authToken.getId()
        );

        FcmTokenRegisterRequestDTO request = new FcmTokenRegisterRequestDTO("");

        // When & Then: 400 Bad Request
        mockMvc.perform(post("/api/auth/fcm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FCM 토큰 등록 - null 토큰 - 400 Bad Request")
    void registerFcmToken_NullToken_BadRequest() throws Exception {
        // Given: AuthToken 생성
        AuthToken authToken = AuthToken.createToken("refresh-token-789", testMember);
        authToken = authTokenRepository.save(authToken);

        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(
                testMember,
                authToken.getId()
        );

        String requestJson = "{\"fcmToken\": null}";

        // When & Then: 400 Bad Request
        mockMvc.perform(post("/api/auth/fcm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 존재하지 않는 AuthToken ID - 404 Not Found")
    void registerFcmToken_InvalidAuthTokenId_NotFound() throws Exception {
        // Given: 존재하지 않는 authTokenId 사용
        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(
                testMember,
                999999L  // 존재하지 않는 ID
        );

        FcmTokenRegisterRequestDTO request = new FcmTokenRegisterRequestDTO(
                "fcm-test-token-xyz"
        );

        // When & Then: 404 Not Found
        mockMvc.perform(post("/api/auth/fcm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("FCM 토큰을 등록할 AuthToken을 찾을 수 없습니다."));
    }


    @Test
    @DisplayName("FCM 토큰 등록 - 인증 없이 요청 - 403 Forbidden")
    void registerFcmToken_Unauthenticated_Forbidden() throws Exception {
        // Given: Request without authentication
        FcmTokenRegisterRequestDTO request = new FcmTokenRegisterRequestDTO(
                "fcm-unauthorized-token"
        );

        // When & Then: 403 Forbidden (Spring Security blocks)
        mockMvc.perform(post("/api/auth/fcm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 기존 토큰 업데이트 - 성공")
    void registerFcmToken_UpdateExistingToken_Success() throws Exception {
        // Given: 이미 FCM 토큰이 등록된 AuthToken
        AuthToken authToken = AuthToken.createToken("refresh-token-update", testMember);
        authToken = authTokenRepository.save(authToken);

        // 기존 FCM 토큰 등록
        authToken.updateFcmToken("old-fcm-token");
        authToken = authTokenRepository.save(authToken);

        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(
                testMember,
                authToken.getId()
        );

        // 새로운 FCM 토큰
        FcmTokenRegisterRequestDTO request = new FcmTokenRegisterRequestDTO(
                "new-fcm-token-updated"
        );

        // When: POST /api/auth/fcm (업데이트)
        mockMvc.perform(post("/api/auth/fcm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                // Then: 200 OK
                .andExpect(status().isOk());

        // Then: DB 확인 - 새 토큰으로 업데이트되었는지
        AuthToken updatedToken = authTokenRepository.findById(authToken.getId())
                .orElseThrow();
        assertThat(updatedToken.getFcmRegistrationToken())
                .isEqualTo("new-fcm-token-updated")
                .isNotEqualTo("old-fcm-token");
    }
}
