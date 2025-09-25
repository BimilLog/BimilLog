package jaeik.bimillog.adapter.in.notification;

import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.H2TestConfiguration;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>알림 SSE 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest + H2 인메모리 데이터베이스 환경에서 SSE 구독 API를 검증합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginPortConfig.class})
@DisplayName("알림 SSE 컨트롤러 통합 테스트")
class NotificationSseControllerIntegrationTest extends BaseIntegrationTest {

    // BaseIntegrationTest에서 mockMvc, testUser, userRepository 등을 자동으로 제공
    
    @Test
    @DisplayName("로그인된 사용자의 SSE 구독 - 성공")
    void subscribe_AuthenticatedUser_Success() throws Exception {
        // When & Then - testUserDetails는 BaseIntegrationTest에서 자동 생성
        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
    
    @Test
    @DisplayName("비로그인 사용자의 SSE 구독 - 실패")
    void subscribe_Unauthenticated_Unauthorized() throws Exception {
        // When & Then - 인증 없이 요청
        mockMvc.perform(get("/api/notification/subscribe"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("잘못된 HTTP 메서드로 SSE 구독 - 실패 (CSRF 보안으로 403)")
    void subscribe_WrongHttpMethod_Forbidden() throws Exception {
        // When & Then - POST 메서드로 요청 (Spring Security CSRF가 먼저 차단)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/notification/subscribe")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isForbidden()); // CSRF 보안으로 403 반환
    }
    
    @Test
    @DisplayName("다른 사용자들의 동시 SSE 구독 - 성공")
    void subscribe_MultipleUsers_Success() throws Exception {
        // Given - 추가 사용자 생성 (BaseIntegrationTest에서 otherUser, adminUser 이미 제공)
        User additionalUser = TestUsers.createUniqueWithPrefix("additional");
        userRepository.save(additionalUser);
        CustomUserDetails additionalUserDetails = createCustomUserDetails(additionalUser);

        // When & Then - 각각 구독 가능해야 함
        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));

        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(otherUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));

        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(additionalUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
    
    @Test
    @DisplayName("Accept 헤더 확인 - text/listener-stream")
    void subscribe_CheckAcceptHeader_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notification/subscribe")
                .header("Accept", "text/listener-stream")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }

}