package jaeik.bimillog.infrastructure.adapter.in.paper;

import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.paper.MessageRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestUsers;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>페이퍼 Query 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 페이퍼 Query API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@IntegrationTest
@Import(TestSocialLoginPortConfig.class)
class PaperQueryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MessageRepository messageRepository;
    
    @Test
    @DisplayName("내 롤링페이퍼 조회 - 성공 (메시지 있음)")
    void myPaper_WithMessages_Success() throws Exception {
        // Given
        messageRepository.save(TestFixtures.createRollingPaper(
                testUser, "생일 축하해!", "red", "font1", 1, 1));
        messageRepository.save(TestFixtures.createRollingPaper(
                testUser, "항상 행복하세요", "blue", "font2", 2, 1));
        messageRepository.save(TestFixtures.createRollingPaper(
                testUser, "응원합니다", "green", "font3", 3, 1));

        // When & Then
        performGet("/api/paper", testUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].content").value("응원합니다"))
                .andExpect(jsonPath("$[0].x").value(3))
                .andExpect(jsonPath("$[0].y").value(1))
                .andExpect(jsonPath("$[0].decoType").value("POTATO"));
    }
    
    @Test
    @DisplayName("내 롤링페이퍼 조회 - 성공 (메시지 없음)")
    void myPaper_WithoutMessages_Success() throws Exception {
        // Given - 새로운 사용자 생성 (메시지 없음)
        User emptyUser = saveAndFlush(TestUsers.createUnique());

        // When & Then
        performGet("/api/paper", createCustomUserDetails(emptyUser))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자의 내 롤링페이퍼 조회 - 실패")
    void myPaper_Unauthenticated_Unauthorized() throws Exception {
        // When & Then
        performGet("/api/paper")
                .andDo(print())
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 성공 (메시지 있음)")
    void visitPaper_WithMessages_Success() throws Exception {
        // Given
        messageRepository.save(TestFixtures.createRollingPaper(
                otherUser, "좋은 메시지", "red", "font1", 1, 1));
        messageRepository.save(TestFixtures.createRollingPaper(
                otherUser, "또 다른 메시지", "blue", "font2", 2, 2));

        // When & Then
        performGet("/api/paper/" + otherUser.getUserName())
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].x").value(1))
                .andExpect(jsonPath("$[0].y").value(1))
                .andExpect(jsonPath("$[0].decoType").value("POTATO"))
                .andExpect(jsonPath("$[1].x").value(2))
                .andExpect(jsonPath("$[1].y").value(2));
    }
    
    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 성공 (메시지 없음)")
    void visitPaper_WithoutMessages_Success() throws Exception {
        // Given
        User emptyUser = saveAndFlush(TestUsers.createUnique());

        // When & Then
        performGet("/api/paper/" + emptyUser.getUserName())
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 롤링페이퍼 방문 - 실패")
    void visitPaper_NonExistentUser_NotFound() throws Exception {
        // When & Then
        performGet("/api/paper/nonexistentuser")
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("인증된 사용자도 다른 사용자 롤링페이퍼 방문 - 성공")
    void visitPaper_AuthenticatedUser_Success() throws Exception {
        // Given
        messageRepository.save(TestFixtures.createRollingPaper(
                otherUser, "방문용 메시지", "red", "font1", 1, 2));

        // When & Then
        performGet("/api/paper/" + otherUser.getUserName(), testUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].x").value(1))
                .andExpect(jsonPath("$[0].y").value(2))
                .andExpect(jsonPath("$[0].decoType").value("POTATO"));
    }
    
    @Test
    @DisplayName("자신의 롤링페이퍼를 userName으로 방문 - 성공")
    void visitPaper_OwnPaper_Success() throws Exception {
        // Given
        Message message = TestFixtures.createRollingPaper(
                testUser, "자기 메시지", "red", "font1", 3, 1);
        messageRepository.save(message);

        // When & Then
        performGet("/api/paper/" + testUser.getUserName(), testUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].x").value(3))
                .andExpect(jsonPath("$[0].y").value(1));
    }
}