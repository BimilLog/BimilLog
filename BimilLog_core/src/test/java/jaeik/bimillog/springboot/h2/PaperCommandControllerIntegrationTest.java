package jaeik.bimillog.springboot.h2;

import jaeik.bimillog.domain.paper.dto.MessageWriteDTO;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.dto.MessageDeleteDTO;
import jaeik.bimillog.domain.paper.repository.PaperRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.builder.PaperTestDataBuilder;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import jaeik.bimillog.testutil.config.TestSocialLoginAdapterConfig;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>페이퍼 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest + H2 인메모리 데이터베이스 환경에서 페이퍼 명령 API를 검증합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("springboot-h2")
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginAdapterConfig.class})
class PaperCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PaperRepository paperRepository;
    
    @Test
    @DisplayName("익명 사용자 메시지 작성 - 성공")
    void writeMessage_AnonymousUser_Success() throws Exception {
        // Given
        MessageWriteDTO dto = TestFixtures.createMessageWriteDTO(
                testMember.getId(), "따뜻한 메시지입니다.", 1, 1);
        ReflectionTestUtils.setField(dto, "anonymity", "익명사용자");

        // When & Then
        performPost("/api/paper/write", dto)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("메시지가 작성되었습니다."));
    }

    @Test
    @DisplayName("인증된 사용자 메시지 작성 - 성공")
    void writeMessage_AuthenticatedUser_Success() throws Exception {
        // Given
        MessageWriteDTO dto = TestFixtures.createMessageWriteDTO(
                testMember.getId(), "생일 축하해!", 2, 2);
        ReflectionTestUtils.setField(dto, "decoType", DecoType.STAR);
        ReflectionTestUtils.setField(dto, "anonymity", "친구1");

        // When & Then
        performPost("/api/paper/write", dto, otherUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("메시지가 작성되었습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 소유자에게 메시지 작성 - 실패")
    void writeMessage_NonExistentOwner_NotFound() throws Exception {
        // Given
        MessageWriteDTO dto = TestFixtures.createMessageWriteDTO(
                999999L, "메시지", 1, 1);

        // When & Then
        performPost("/api/paper/write", dto)
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("잘못된 MessageDTO로 메시지 작성 - 실패")
    void writeMessage_InvalidMessageDTO_BadRequest() throws Exception {
        // Given
        MessageWriteDTO dto = TestFixtures.createMessageWriteDTO(
                testMember.getId(), "메시지", 1, 1);
        ReflectionTestUtils.setField(dto, "anonymity", "매우긴익명사용자이름입니다"); // 8자 초과

        // When & Then
        performPost("/api/paper/write", dto)
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("내 페이퍼에서 메시지 삭제 - 성공")
    void deleteMessage_MyPaper_Success() throws Exception {
        // Given
        Message message = PaperTestDataBuilder.createRollingPaper(
                testMember, "삭제될 메시지", 1, 1);
        Message savedMessage = paperRepository.save(message);

        MessageDeleteDTO messageDeleteDTO = TestFixtures.createMessageDeleteDTO(savedMessage.getId());

        // When & Then
        performPost("/api/paper/delete", messageDeleteDTO, testUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("메시지가 삭제되었습니다."));
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자의 메시지 삭제 - 실패 (500 Internal Server Error)")
    void deleteMessage_Unauthenticated_InternalServerError() throws Exception {
        // Given
        MessageDeleteDTO messageDeleteDTO = TestFixtures.createMessageDeleteDTO(1L);

        // When & Then
        performPost("/api/paper/delete", messageDeleteDTO)
                .andDo(print())
                .andExpect(status().isInternalServerError());  // userDetails가 null일 때 NullPointerException 발생 → 500 반환
    }
    
    @Test
    @DisplayName("존재하지 않는 메시지 삭제 - 실패")
    void deleteMessage_NonExistentMessage_NotFound() throws Exception {
        // Given
        MessageDeleteDTO messageDeleteDTO = TestFixtures.createMessageDeleteDTO(99999L);

        // When & Then
        performPost("/api/paper/delete", messageDeleteDTO, testUserDetails)
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}