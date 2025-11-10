package jaeik.bimillog.adapter.in.paper;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.out.MessageRepository;
import jaeik.bimillog.testutil.*;
import jaeik.bimillog.testutil.builder.PaperTestDataBuilder;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import jaeik.bimillog.testutil.config.TestSocialLoginAdapterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>페이퍼 Query 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest + H2 인메모리 데이터베이스 환경에서 페이퍼 조회 API를 검증합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginAdapterConfig.class})
@Tag("integration")
class PaperQueryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MessageRepository messageRepository;
    
    @Test
    @DisplayName("내 롤링페이퍼 조회 - 성공 (메시지 있음)")
    void myPaper_WithMessages_Success() throws Exception {
        // Given
        messageRepository.save(PaperTestDataBuilder.createRollingPaper(
                testMember, "생일 축하해!", 1, 1));
        messageRepository.save(PaperTestDataBuilder.createRollingPaper(
                testMember, "항상 행복하세요", 2, 1));
        messageRepository.save(PaperTestDataBuilder.createRollingPaper(
                testMember, "응원합니다", 3, 1));

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
        Member emptyMember = saveMember(TestMembers.createUnique());

        // When & Then
        performGet("/api/paper", createCustomUserDetails(emptyMember))
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
        messageRepository.save(PaperTestDataBuilder.createRollingPaper(
                otherMember, "좋은 메시지", 1, 1));
        messageRepository.save(PaperTestDataBuilder.createRollingPaper(
                otherMember, "또 다른 메시지", 2, 2));

        // When & Then
        performGet("/api/paper/" + otherMember.getMemberName())
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
        Member emptyMember = saveMember(TestMembers.createUnique());

        // When & Then
        performGet("/api/paper/" + emptyMember.getMemberName())
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
        messageRepository.save(PaperTestDataBuilder.createRollingPaper(
                otherMember, "방문용 메시지", 1, 2));

        // When & Then
        performGet("/api/paper/" + otherMember.getMemberName(), testUserDetails)
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
    @DisplayName("자신의 롤링페이퍼를 memberName으로 방문 - 성공")
    void visitPaper_OwnPaper_Success() throws Exception {
        // Given
        Message message = PaperTestDataBuilder.createRollingPaper(
                testMember, "자기 메시지", 3, 1);
        messageRepository.save(message);

        // When & Then
        performGet("/api/paper/" + testMember.getMemberName(), testUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].x").value(3))
                .andExpect(jsonPath("$[0].y").value(1));
    }
}