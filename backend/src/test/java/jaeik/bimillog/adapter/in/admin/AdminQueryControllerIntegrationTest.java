package jaeik.bimillog.adapter.in.admin;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>관리자 Query 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 관리자 Query API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@IntegrationTest
@DisplayName("관리자 Query 컨트롤러 통합 테스트")
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
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
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
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }
}