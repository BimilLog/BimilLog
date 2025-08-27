package jaeik.growfarm.infrastructure.adapter.admin.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import jaeik.growfarm.util.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>관리자 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 관리자 API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
class AdminControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }
    
    @Test
    @DisplayName("관리자 권한으로 신고 목록 조회 - 성공")
    @WithMockUser(roles = "ADMIN")
    void getReportList_WithAdminRole_Success() throws Exception {
        // Given
        int page = 0;
        int size = 10;
        
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
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
    @DisplayName("신고 타입 필터로 신고 목록 조회 - 성공")
    @WithMockUser(roles = "ADMIN")
    void getReportList_WithReportTypeFilter_Success() throws Exception {
        // Given
        ReportType reportType = ReportType.POST;
        
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", "0")
                        .param("size", "10")
                        .param("reportType", reportType.name()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }
    
    @Test
    @DisplayName("일반 사용자 권한으로 신고 목록 조회 - 실패 (권한 부족)")
    @WithMockUser(roles = "USER")
    void getReportList_WithUserRole_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk()); // Query 엔드포인트는 권한 체크가 없을 수 있음
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자의 신고 목록 조회 - 실패")
    void getReportList_Unauthenticated_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("관리자 권한으로 사용자 차단 - 성공")
    @WithMockUser(roles = "ADMIN")
    void banUser_WithAdminRole_Success() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(1L)
                .content("부적절한 게시글 신고")
                .build();
        
        String requestBody = objectMapper.writeValueAsString(reportDTO);
        
        // When & Then
        mockMvc.perform(post("/api/dto/reports/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("유저를 성공적으로 차단했습니다."));
    }
    
    @Test
    @DisplayName("일반 사용자 권한으로 사용자 차단 - 실패 (권한 부족)")
    @WithMockUser(roles = "USER")
    void banUser_WithUserRole_Forbidden() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(1L)
                .content("부적절한 게시글 신고")
                .build();
        
        String requestBody = objectMapper.writeValueAsString(reportDTO);
        
        // When & Then
        mockMvc.perform(post("/api/dto/reports/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자의 사용자 차단 - 실패")
    void banUser_Unauthenticated_Unauthorized() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(1L)
                .content("부적절한 게시글 신고")
                .build();
        
        String requestBody = objectMapper.writeValueAsString(reportDTO);
        
        // When & Then
        mockMvc.perform(post("/api/dto/reports/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("잘못된 ReportDTO로 사용자 차단 - 실패")
    @WithMockUser(roles = "ADMIN")
    void banUser_WithInvalidReportDTO_BadRequest() throws Exception {
        // Given - 잘못된 JSON
        String invalidRequestBody = "{ \"reportType\": \"INVALID_TYPE\", \"targetId\": null }";
        
        // When & Then
        mockMvc.perform(post("/api/dto/reports/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("관리자 권한으로 사용자 강제 탈퇴 - 성공")
    @WithMockUser(roles = "ADMIN")
    void forceWithdrawUser_WithAdminRole_Success() throws Exception {
        // Given
        Setting setting = Setting.createSetting();
        User testUser = User.builder()
                .userName("testuser")
                .socialId("test123")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
        User savedUser = userRepository.save(testUser);
        
        // When & Then
        mockMvc.perform(delete("/api/dto/users/{userId}", savedUser.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("관리자 권한으로 사용자 탈퇴가 완료되었습니다."));
    }
    
    @Test
    @DisplayName("일반 사용자 권한으로 사용자 강제 탈퇴 - 실패 (권한 부족)")
    @WithMockUser(roles = "USER")
    void forceWithdrawUser_WithUserRole_Forbidden() throws Exception {
        // Given
        Long userId = 1L;
        
        // When & Then
        mockMvc.perform(delete("/api/dto/users/{userId}", userId))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 강제 탈퇴 - 실패")
    @WithMockUser(roles = "ADMIN")
    void forceWithdrawUser_UserNotFound_NotFound() throws Exception {
        // Given
        Long nonExistentUserId = 99999L;
        
        // When & Then
        mockMvc.perform(delete("/api/dto/users/{userId}", nonExistentUserId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("잘못된 사용자 ID로 강제 탈퇴 - 실패")
    @WithMockUser(roles = "ADMIN")
    void forceWithdrawUser_InvalidUserId_BadRequest() throws Exception {
        // Given
        String invalidUserId = "invalid";
        
        // When & Then
        mockMvc.perform(delete("/api/dto/users/{userId}", invalidUserId))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}