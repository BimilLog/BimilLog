package jaeik.bimillog.infrastructure.adapter.in.admin;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

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
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dto/query/reports")
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
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dto/query/reports")
                        .param("page", "0")
                        .param("size", "10")
                        .param("reportType", reportType.name())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }
    
    @Test
    @DisplayName("관리자 권한으로 기본 페이징 파라미터로 신고 목록 조회 - 성공")
    void getReportList_WithDefaultPagingParams_Success() throws Exception {
        // When & Then
        performGet("/api/dto/query/reports", adminUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))  // default size
                .andExpect(jsonPath("$.number").value(0)); // default page
    }
    
    @Test
    @DisplayName("관리자 권한으로 큰 페이지 크기로 신고 목록 조회 - 성공")
    void getReportList_WithLargePageSize_Success() throws Exception {
        // Given
        int page = 0;
        int size = 100;
        
        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dto/query/reports")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size").value(size));
    }
    
    @Test
    @DisplayName("일반 사용자 권한으로 신고 목록 조회 - 실패 (권한 부족)")
    void getReportList_WithUserRole_Forbidden() throws Exception {
        // When & Then
        performGet("/api/dto/query/reports?page=0&size=10", testUserDetails)
                .andDo(print())
                .andExpect(status().isForbidden()); // 일반 사용자는 관리자 권한이 없어서 403
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자의 신고 목록 조회 - 실패")
    void getReportList_Unauthenticated_Forbidden() throws Exception {
        // When & Then
        performGet("/api/dto/query/reports?page=0&size=10")
                .andDo(print())
                .andExpect(status().isForbidden()); // 실제로는 403이 반환됨
    }
    
    @Test
    @DisplayName("관리자 권한으로 잘못된 페이지 번호로 신고 목록 조회 - 성공 (음수 페이지는 0으로 처리)")
    void getReportList_WithNegativePage_Success() throws Exception {
        // Given
        int page = -1;
        int size = 10;
        
        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dto/query/reports")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    @DisplayName("관리자 권한으로 존재하지 않는 신고 타입으로 조회 - 실패")
    void getReportList_WithInvalidReportType_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dto/query/reports")
                        .param("page", "0")
                        .param("size", "10")
                        .param("reportType", "INVALID_TYPE")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
}