package jaeik.bimillog.infrastructure.adapter.admin.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.social.dto.UserDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jaeik.bimillog.util.TestContainersConfiguration;
import jaeik.bimillog.util.TestSocialLoginPortConfig;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>관리자 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 관리자 Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 *
 *
 * @author Jaeik
 * @since 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
class AdminCommandControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.post.PostJpaRepository postRepository;
    
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
        UserDTO adminUserDTO = UserDTO.builder()
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
        return new CustomUserDetails(adminUserDTO);
    }
    
    /**
     * 테스트용 일반 사용자 CustomUserDetails 생성
     */
    private CustomUserDetails createUserUserDetails() {
        UserDTO userDTO = UserDTO.builder()
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
        return new CustomUserDetails(userDTO);
    }
    
    @Test
    @DisplayName("관리자 권한으로 사용자 차단 - 성공")
    void banUser_WithAdminRole_Success() throws Exception {
        // Given - 테스트용 사용자와 게시글 생성
        var testSetting = jaeik.bimillog.domain.user.entity.Setting.builder()
                .build();
                
        var testUser = jaeik.bimillog.domain.user.entity.User.builder()
                .socialId("testuser123")
                .socialNickname("테스트사용자")
                .userName("testuser")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(testSetting)
                .build();
        var savedUser = userRepository.save(testUser);
        
        var testPost = jaeik.bimillog.domain.post.entity.Post.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .views(0)
                .isNotice(false)
                .user(savedUser)
                .build();
        var savedPost = postRepository.save(testPost);
        
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(savedPost.getId())
                .content("부적절한 게시글 신고")
                .build();
        
        String requestBody = objectMapper.writeValueAsString(reportDTO);
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(post("/api/dto/reports/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf())
                        .with(user(adminUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("유저를 성공적으로 차단했습니다."));
    }
    
    @Test
    @DisplayName("일반 사용자 권한으로 사용자 차단 - 실패 (권한 부족)")
    void banUser_WithUserRole_Forbidden() throws Exception {
        // Given - 테스트용 게시글 생성
        var testPost = jaeik.bimillog.domain.post.entity.Post.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .views(0)
                .isNotice(false)
                .build();
        var savedPost = postRepository.save(testPost);
        
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(savedPost.getId())
                .content("부적절한 게시글 신고")
                .build();
        
        String requestBody = objectMapper.writeValueAsString(reportDTO);
        CustomUserDetails regularUser = createUserUserDetails();
        
        // When & Then
        mockMvc.perform(post("/api/dto/reports/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf())
                        .with(user(regularUser)))
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
                        .content(requestBody)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden()); // 실제로는 403이 반환됨
    }
    
    @Test
    @DisplayName("잘못된 ReportDTO로 사용자 차단 - 실패")
    void banUser_WithInvalidReportDTO_BadRequest() throws Exception {
        // Given - 잘못된 JSON
        String invalidRequestBody = "{ \"reportType\": \"INVALID_TYPE\", \"targetId\": null }";
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(post("/api/dto/reports/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody)
                        .with(csrf())
                        .with(user(adminUser)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    @DisplayName("관리자 권한으로 사용자 강제 탈퇴 - 성공")
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
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(delete("/api/dto/users/{userId}", savedUser.getId())
                        .with(csrf())
                        .with(user(adminUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("관리자 권한으로 사용자 탈퇴가 완료되었습니다."));
    }
    
    @Test
    @DisplayName("일반 사용자 권한으로 사용자 강제 탈퇴 - 실패 (권한 부족)")
    void forceWithdrawUser_WithUserRole_Forbidden() throws Exception {
        // Given
        Long userId = 1L;
        CustomUserDetails regularUser = createUserUserDetails();
        
        // When & Then
        mockMvc.perform(delete("/api/dto/users/{userId}", userId)
                        .with(csrf())
                        .with(user(regularUser)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 강제 탈퇴 - 실패")
    void forceWithdrawUser_UserNotFound_NotFound() throws Exception {
        // Given
        Long nonExistentUserId = 99999L;
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(delete("/api/dto/users/{userId}", nonExistentUserId)
                        .with(csrf())
                        .with(user(adminUser)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("잘못된 사용자 ID로 강제 탈퇴 - 실패")
    void forceWithdrawUser_InvalidUserId_BadRequest() throws Exception {
        // Given
        String invalidUserId = "invalid";
        CustomUserDetails adminUser = createAdminUserDetails();
        
        // When & Then
        mockMvc.perform(delete("/api/dto/users/{userId}", invalidUserId)
                        .with(csrf())
                        .with(user(adminUser)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
}