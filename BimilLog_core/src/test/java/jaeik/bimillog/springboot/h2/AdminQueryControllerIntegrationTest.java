package jaeik.bimillog.springboot.h2;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>관리자 Query 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest + H2 인메모리 데이터베이스 환경에서 관리자 조회 API를 검증합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@ActiveProfiles("h2test")
@Import(H2TestConfiguration.class)
@DisplayName("관리자 Query 컨트롤러 통합 테스트")
@Tag("springboot-h2")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminQueryControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("관리자 권한으로 신고 목록 조회 - 성공")
    void getReportList_WithAdminRole_Success() throws Exception {
        // Given
        int page = 0;
        int size = 10;

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/reports")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .with(SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.number").value(page));
    }

    @Test
    @DisplayName("관리자 권한으로 신고 타입 필터로 신고 목록 조회 - 성공")
    void getReportList_WithReportTypeFilter_Success() throws Exception {
        // Given
        ReportType reportType = ReportType.POST;

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/reports")
                        .param("page", "0")
                        .param("size", "10")
                        .param("reportType", reportType.name())
                        .with(SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }
}