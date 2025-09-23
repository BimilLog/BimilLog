package jaeik.bimillog.infrastructure.adapter.in.paper;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.paper.dto.MessageDTO;
import jaeik.bimillog.infrastructure.adapter.out.paper.MessageRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>페이퍼 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 페이퍼 Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@IntegrationTest
@Import(TestSocialLoginPortConfig.class)
class PaperCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MessageRepository messageRepository;
    
    @Test
    @DisplayName("익명 사용자 메시지 작성 - 성공")
    void writeMessage_AnonymousUser_Success() throws Exception {
        // Given
        MessageDTO messageDTO = TestFixtures.createPaperMessageRequest(
                "따뜻한 메시지입니다.", "red", "font1", 1, 1);
        messageDTO.setAnonymity("익명사용자");

        // When & Then
        performPost("/api/paper/" + testUser.getUserName(), messageDTO)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("메시지가 작성되었습니다."));
    }
    
    @Test
    @DisplayName("인증된 사용자 메시지 작성 - 성공")
    void writeMessage_AuthenticatedUser_Success() throws Exception {
        // Given
        MessageDTO messageDTO = TestFixtures.createPaperMessageRequest(
                "생일 축하해!", "blue", "font2", 2, 2);
        messageDTO.setDecoType(DecoType.STAR);
        messageDTO.setAnonymity("친구1");
        messageDTO.setUserId(otherUser.getId());

        // When & Then
        performPost("/api/paper/" + testUser.getUserName(), messageDTO, otherUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("메시지가 작성되었습니다."));
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자에게 메시지 작성 - 실패")
    void writeMessage_NonExistentUser_NotFound() throws Exception {
        // Given
        MessageDTO messageDTO = TestFixtures.createPaperMessageRequest(
                "메시지", "red", "font1", 1, 1);

        // When & Then
        performPost("/api/paper/nonexistentuser", messageDTO)
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("잘못된 MessageDTO로 메시지 작성 - 실패")
    void writeMessage_InvalidMessageDTO_BadRequest() throws Exception {
        // Given
        MessageDTO messageDTO = TestFixtures.createPaperMessageRequest(
                "메시지", "red", "font1", 1, 1);
        messageDTO.setAnonymity("매우긴익명사용자이름입니다"); // 8자 초과

        // When & Then
        performPost("/api/paper/" + testUser.getUserName(), messageDTO)
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("내 페이퍼에서 메시지 삭제 - 성공")
    void deleteMessage_MyPaper_Success() throws Exception {
        // Given
        Message message = TestFixtures.createRollingPaper(
                testUser, "삭제될 메시지", "red", "font1", 1, 1);
        Message savedMessage = messageRepository.save(message);

        MessageDTO messageDTO = TestFixtures.createPaperMessageRequest(
                "삭제될 메시지", "red", "font1", 1, 1);
        messageDTO.setId(savedMessage.getId());

        // When & Then
        performPost("/api/paper/delete", messageDTO, testUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("메시지가 삭제되었습니다."));
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자의 메시지 삭제 - 실패")
    void deleteMessage_Unauthenticated_Unauthorized() throws Exception {
        // Given
        MessageDTO messageDTO = TestFixtures.createPaperMessageRequest(
                "메시지", "red", "font1", 1, 1);
        messageDTO.setId(1L);

        // When & Then
        performDelete("/api/paper/delete")
                .andDo(print())
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("존재하지 않는 메시지 삭제 - 실패")
    void deleteMessage_NonExistentMessage_NotFound() throws Exception {
        // Given
        MessageDTO messageDTO = TestFixtures.createPaperMessageRequest(
                "메시지", "red", "font1", 1, 1);
        messageDTO.setId(99999L);

        // When & Then
        performPost("/api/paper/delete", messageDTO, testUserDetails)
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}