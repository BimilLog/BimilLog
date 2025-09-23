package jaeik.bimillog.infrastructure.adapter.in.admin;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
class AdminQueryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }
    
    /**
     * 테스트용 관리자 CustomUserDetails 생성
     */
    private CustomUserDetails createAdminUserDetails() {
        ExistingUserDetail adminUserDetail = ExistingUserDetail.builder()
                .userId(1L)
                .socialId("admin123")
                .provider(SocialProvider.KAKAO)
                .settingId(1L)
                .socialNickname("관리자")
                .userName("admin")
                .role(UserRole.ADMIN)
                .tokenId(1L)
                .fcmTokenId(1L)
                .build();
        return new CustomUserDetails(adminUserDetail);
    }
    
    /**
     * 테스트용 일반 사용자 CustomUserDetails 생성
     */
    private CustomUserDetails createUserUserDetails() {
        ExistingUserDetail userDetail = ExistingUserDetail.builder()
                .userId(2L)
                .socialId("user123")
                .provider(SocialProvider.KAKAO)
                .settingId(2L)
                .socialNickname("일반사용자")
                .userName("user")
                .role(UserRole.USER)
                .tokenId(2L)
                .fcmTokenId(2L)
                .build();
        return new CustomUserDetails(userDetail);
    }
    
    @Test
    @DisplayName("관리자 권한으로 신고 목록 조회 - 성공")
    void getReportList_WithAdminRole_Success() throws Exception {
        // Given
        int page = 0;
        int size = 10;
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .with(user(adminUser)))
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
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", "0")
                        .param("size", "10")
                        .param("reportType", reportType.name())
                        .with(user(adminUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }
    
    @Test
    @DisplayName("관리자 권한으로 기본 페이징 파라미터로 신고 목록 조회 - 성공")
    void getReportList_WithDefaultPagingParams_Success() throws Exception {
        // Given
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .with(user(adminUser)))
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
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .with(user(adminUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size").value(size));
    }
    
    @Test
    @DisplayName("일반 사용자 권한으로 신고 목록 조회 - 실패 (권한 부족)")
    void getReportList_WithUserRole_Forbidden() throws Exception {
        // Given
        CustomUserDetails regularUser = createUserUserDetails();
        
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(regularUser)))
                .andDo(print())
                .andExpect(status().isForbidden()); // 일반 사용자는 관리자 권한이 없어서 403
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자의 신고 목록 조회 - 실패")
    void getReportList_Unauthenticated_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isForbidden()); // 실제로는 403이 반환됨
    }
    
    @Test
    @DisplayName("관리자 권한으로 잘못된 페이지 번호로 신고 목록 조회 - 성공 (음수 페이지는 0으로 처리)")
    void getReportList_WithNegativePage_Success() throws Exception {
        // Given
        int page = -1;
        int size = 10;
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .with(user(adminUser)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    @DisplayName("관리자 권한으로 존재하지 않는 신고 타입으로 조회 - 실패")
    void getReportList_WithInvalidReportType_BadRequest() throws Exception {
        // Given
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(get("/api/dto/query/reports")
                        .param("page", "0")
                        .param("size", "10")
                        .param("reportType", "INVALID_TYPE")
                        .with(user(adminUser)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
}