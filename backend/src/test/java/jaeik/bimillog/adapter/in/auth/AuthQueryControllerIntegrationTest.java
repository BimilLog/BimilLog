package jaeik.bimillog.adapter.in.auth;

import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.testutil.AuthTestFixtures;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.H2TestConfiguration;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>인증 조회 컨트롤러 통합 테스트</h2>
 * <p>H2 인메모리 데이터베이스를 사용한 실제 Auth Query API 통합 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginPortConfig.class})
@DisplayName("인증 조회 컨트롤러 통합 테스트")
@Tag("fast-integration")
class AuthQueryControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("현재 사용자 정보 조회 통합 테스트 - 일반 사용자")
    void getCurrentUser_RegularUser_IntegrationTest() throws Exception {
        User testUser = TestUsers.createUniqueWithPrefix("통합테스트사용자");
        User savedUser = userRepository.save(testUser);

        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(savedUser);

        mockMvc.perform(get("/api/auth/me")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(savedUser.getId()))
                .andExpect(jsonPath("$.settingId").value(savedUser.getSetting().getId()))
                .andExpect(jsonPath("$.socialNickname").value(savedUser.getSocialNickname()))
                .andExpect(jsonPath("$.thumbnailImage").value(savedUser.getThumbnailImage()))
                .andExpect(jsonPath("$.userName").value(savedUser.getUserName()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("현재 사용자 정보 조회 통합 테스트 - 관리자 사용자")
    void getCurrentUser_AdminUser_IntegrationTest() throws Exception {
        User adminUser = TestUsers.createUniqueWithPrefix("관리자", builder -> {
            builder.userName("관리자");
            builder.socialNickname("관리자");
            builder.role(UserRole.ADMIN);
            builder.setting(TestUsers.createAllDisabledSetting());
        });
        User savedAdmin = userRepository.save(adminUser);

        CustomUserDetails adminUserDetails = AuthTestFixtures.createCustomUserDetails(savedAdmin);

        mockMvc.perform(get("/api/auth/me")
                        .with(user(adminUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(savedAdmin.getId()))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.userName").value("관리자"));
    }

    @Test
    @DisplayName("서버 헬스체크 통합 테스트")
    void healthCheck_IntegrationTest() throws Exception {
        mockMvc.perform(get("/api/global/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("OK"))
                .andExpect(content().contentType("text/plain;charset=UTF-8"));
    }

    @Test
    @DisplayName("인증이 필요한 API에 비인증 접근 - 401 Unauthorized")
    void getCurrentUser_Unauthenticated_IntegrationTest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
