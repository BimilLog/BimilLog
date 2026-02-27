package jaeik.bimillog.springboot.h2;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.repository.PaperRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestMembers;
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
    private PaperRepository paperRepository;
    
    @Test
    @DisplayName("내 롤링페이퍼 조회 - 메시지 있음/없음 모두 성공")
    void myPaper_ReturnsMessagesOrEmptyArray_Success() throws Exception {
        // Given - 케이스 1: 메시지가 있는 사용자
        paperRepository.save(PaperTestDataBuilder.createRollingPaper(
                testMember, "생일 축하해!", 1, 1));
        paperRepository.save(PaperTestDataBuilder.createRollingPaper(
                testMember, "항상 행복하세요", 2, 1));
        paperRepository.save(PaperTestDataBuilder.createRollingPaper(
                testMember, "응원합니다", 3, 1));

        // When & Then - 케이스 1: 메시지 있음
        performGet("/api/paper", testUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].content").exists())
                .andExpect(jsonPath("$[*].x").exists())
                .andExpect(jsonPath("$[*].y").exists())
                .andExpect(jsonPath("$[*].decoType").exists());

        // Given - 케이스 2: 메시지가 없는 사용자
        Member emptyMember = saveMember(TestMembers.createUnique());

        // When & Then - 케이스 2: 메시지 없음
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
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 메시지 있음/없음 모두 성공")
    void visitPaper_ReturnsMessagesOrEmptyArray_Success() throws Exception {
        // Given - 케이스 1: 메시지가 있는 사용자
        paperRepository.save(PaperTestDataBuilder.createRollingPaper(
                otherMember, "좋은 메시지", 1, 1));
        paperRepository.save(PaperTestDataBuilder.createRollingPaper(
                otherMember, "또 다른 메시지", 2, 2));

        // When & Then - 케이스 1: 메시지 있음
        performGet("/api/paper/" + otherMember.getMemberName())
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.ownerId").value(otherMember.getId()))
                .andExpect(jsonPath("$.visitMessageDTOList").isArray())
                .andExpect(jsonPath("$.visitMessageDTOList.length()").value(2))
                .andExpect(jsonPath("$.visitMessageDTOList[*].decoType").exists())
                .andExpect(jsonPath("$.visitMessageDTOList[*].x").exists())
                .andExpect(jsonPath("$.visitMessageDTOList[*].y").exists());

        // Given - 케이스 2: 메시지가 없는 사용자
        Member emptyMember = saveMember(TestMembers.createUnique());

        // When & Then - 케이스 2: 메시지 없음
        performGet("/api/paper/" + emptyMember.getMemberName())
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.ownerId").value(emptyMember.getId()))
                .andExpect(jsonPath("$.visitMessageDTOList").isArray())
                .andExpect(jsonPath("$.visitMessageDTOList.length()").value(0));
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
        paperRepository.save(PaperTestDataBuilder.createRollingPaper(
                otherMember, "방문용 메시지", 1, 2));

        // When & Then
        performGet("/api/paper/" + otherMember.getMemberName(), testUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.ownerId").value(otherMember.getId()))
                .andExpect(jsonPath("$.visitMessageDTOList").isArray())
                .andExpect(jsonPath("$.visitMessageDTOList.length()").value(1))
                .andExpect(jsonPath("$.visitMessageDTOList[0].x").value(1))
                .andExpect(jsonPath("$.visitMessageDTOList[0].y").value(2))
                .andExpect(jsonPath("$.visitMessageDTOList[0].decoType").value("POTATO"));
    }
    
    @Test
    @DisplayName("자신의 롤링페이퍼를 memberName으로 방문 - 성공")
    void visitPaper_OwnPaper_Success() throws Exception {
        // Given
        Message message = PaperTestDataBuilder.createRollingPaper(
                testMember, "자기 메시지", 3, 1);
        paperRepository.save(message);

        // When & Then
        performGet("/api/paper/" + testMember.getMemberName(), testUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.ownerId").value(testMember.getId()))
                .andExpect(jsonPath("$.visitMessageDTOList").isArray())
                .andExpect(jsonPath("$.visitMessageDTOList.length()").value(1))
                .andExpect(jsonPath("$.visitMessageDTOList[0].x").value(3))
                .andExpect(jsonPath("$.visitMessageDTOList[0].y").value(1));
    }
}