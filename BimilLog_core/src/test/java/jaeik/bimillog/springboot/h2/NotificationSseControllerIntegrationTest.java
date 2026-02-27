package jaeik.bimillog.springboot.h2;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import jaeik.bimillog.testutil.config.TestSocialLoginAdapterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
@Import({H2TestConfiguration.class, TestSocialLoginAdapterConfig.class})
@DisplayName("알림 SSE 컨트롤러 통합 테스트")
@Tag("springboot-h2")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationSseControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("로그인된 사용자의 SSE 구독 - 성공")
    void subscribe_AuthenticatedUser_Success() throws Exception {
        // When & Then - testUserDetails는 BaseIntegrationTest에서 자동 생성
        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
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
        mockMvc.perform(MockMvcRequestBuilders.post("/api/notification/subscribe")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isForbidden()); // CSRF 보안으로 403 반환
    }
    
    @Test
    @DisplayName("다른 사용자들의 동시 SSE 구독 - 성공")
    void subscribe_MultipleUsers_Success() throws Exception {
        // Given - 추가 사용자 생성 (BaseIntegrationTest에서 otherMember, adminMember 이미 제공)
        Member additionalMember = TestMembers.createUniqueWithPrefix("additional");
        saveMember(additionalMember);
        CustomUserDetails additionalUserDetails = createCustomUserDetails(additionalMember);

        // When & Then - 각각 구독 가능해야 함
        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));

        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(otherUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));

        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(additionalUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
    
    @Test
    @DisplayName("Accept 헤더 확인 - text/event-stream")
    void subscribe_CheckAcceptHeader_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notification/subscribe")
                .header("Accept", "text/event-stream")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

}