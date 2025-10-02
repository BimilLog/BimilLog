package jaeik.bimillog.adapter.in.auth;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.infrastructure.adapter.in.auth.dto.SocialLoginRequestDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.auth.AuthTokenRepository;
import jaeik.bimillog.testutil.AuthTestFixtures;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
@IntegrationTest
@Import(TestSocialLoginPortConfig.class)
@DisplayName("인증 명령 컨트롤러 통합 테스트")
class AuthCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Test
    @DisplayName("소셜 로그인 통합 테스트 - 신규 사용자")
    void socialLogin_NewUser_IntegrationTest() throws Exception {
        SocialLoginRequestDTO request = new SocialLoginRequestDTO(
                "KAKAO",
                "new_user_code",
                "integration-test-fcm-TemporaryToken"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(content().string("NEW_USER"));
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

        SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "existing_user_code", null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(content().string("EXISTING_USER"));
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
    @DisplayName("회원탈퇴 통합 테스트 - 성공")
    void withdraw_IntegrationTest_Success() throws Exception {
        Member testMember = TestMembers.createUnique();
        saveMember(testMember);

        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(testMember);

        mockMvc.perform(delete("/api/member/withdraw")
                        .with(user(userDetails))
                        .with(csrf()))
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
                .andExpect(status().isBadRequest());
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
}
