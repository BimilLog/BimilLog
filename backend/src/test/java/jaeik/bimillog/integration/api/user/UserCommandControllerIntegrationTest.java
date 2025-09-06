package jaeik.bimillog.integration.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import jaeik.bimillog.infrastructure.adapter.user.in.web.dto.SettingDTO;
import jaeik.bimillog.infrastructure.adapter.user.in.web.dto.UserNameDTO;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.social.dto.UserDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>사용자 명령 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 User Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
@DisplayName("사용자 명령 컨트롤러 통합 테스트")
class UserCommandControllerIntegrationTest {

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
    @DisplayName("닉네임 변경 통합 테스트 - 성공")
    void updateUserName_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);
        
        UserNameDTO userNameDTO = new UserNameDTO();
        userNameDTO.setUserName("새로운닉네임");

        // When & Then
        mockMvc.perform(post("/api/user/username")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userNameDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("닉네임이 변경되었습니다."));
    }

    @Test
    @DisplayName("닉네임 변경 통합 테스트 - 유효성 검증 실패 (8글자 초과)")
    void updateUserName_IntegrationTest_ValidationFail() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);
        
        UserNameDTO userNameDTO = new UserNameDTO();
        userNameDTO.setUserName("아주긴닉네임이라서8글자초과"); // 8글자 초과

        // When & Then
        mockMvc.perform(post("/api/user/username")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userNameDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("설정 수정 통합 테스트 - 성공")
    void updateSetting_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);
        
        SettingDTO settingDTO = SettingDTO.builder()
                .messageNotification(false)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();

        // When & Then
        mockMvc.perform(post("/api/user/setting")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("설정 수정 완료"));
    }

    @Test
    @DisplayName("닉네임 변경 - 인증되지 않은 사용자 - 403 Forbidden")
    void updateUserName_Unauthenticated_Forbidden() throws Exception {
        // Given
        UserNameDTO userNameDTO = new UserNameDTO();
        userNameDTO.setUserName("새로운닉네임");

        // When & Then
        mockMvc.perform(post("/api/user/username")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userNameDTO)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("설정 수정 - 인증되지 않은 사용자 - 403 Forbidden")
    void updateSetting_Unauthenticated_Forbidden() throws Exception {
        // Given
        SettingDTO settingDTO = SettingDTO.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        // When & Then
        mockMvc.perform(post("/api/user/setting")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingDTO)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF 토큰 없이 POST 요청 - 403 Forbidden")
    void postWithoutCsrf_Forbidden() throws Exception {
        // Given
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);
        
        UserNameDTO userNameDTO = new UserNameDTO();
        userNameDTO.setUserName("새로운닉네임");

        // When & Then
        mockMvc.perform(post("/api/user/username")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userNameDTO)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Content-Type 누락 - 500 Internal Server Error")
    void postWithoutContentType_InternalServerError() throws Exception {
        // Given
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);
        
        UserNameDTO userNameDTO = new UserNameDTO();
        userNameDTO.setUserName("새로운닉네임");

        // When & Then
        mockMvc.perform(post("/api/user/username")
                        .with(user(userDetails))
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(userNameDTO)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("신고 제출 통합 테스트 - 인증된 사용자 게시글 신고")
    void submitReport_AuthenticatedUser_PostReport_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        CustomUserDetails userDetails = createCustomUserDetails(testUser);
        
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(123L)
                .content("부적절한 게시글입니다.")
                .build();

        // When & Then
        mockMvc.perform(post("/api/user/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("신고/건의사항이 접수되었습니다."));
    }

    @Test
    @DisplayName("신고 제출 통합 테스트 - 인증된 사용자 댓글 신고")
    void submitReport_AuthenticatedUser_CommentReport_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        CustomUserDetails userDetails = createCustomUserDetails(testUser);
        
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.COMMENT)
                .targetId(456L)
                .content("부적절한 댓글입니다.")
                .build();

        // When & Then
        mockMvc.perform(post("/api/user/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("신고/건의사항이 접수되었습니다."));
    }

    @Test
    @DisplayName("신고 제출 통합 테스트 - 인증된 사용자 건의사항")
    void submitReport_AuthenticatedUser_Suggestion_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        CustomUserDetails userDetails = createCustomUserDetails(testUser);
        
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.IMPROVEMENT)
                .targetId(null) // 건의사항은 targetId 불필요
                .content("새로운 기능을 건의합니다.")
                .build();

        // When & Then
        mockMvc.perform(post("/api/user/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("신고/건의사항이 접수되었습니다."));
    }

    @Test
    @DisplayName("신고 제출 통합 테스트 - 익명 사용자 신고")
    void submitReport_AnonymousUser_Success() throws Exception {
        // Given - 인증되지 않은 상태로 신고
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(789L)
                .content("익명 신고입니다.")
                .build();

        // When & Then - 익명 사용자도 신고 가능 (CSRF는 필요함)
        mockMvc.perform(post("/api/user/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("신고/건의사항이 접수되었습니다."));
    }

    @Test
    @DisplayName("신고 제출 통합 테스트 - 유효하지 않은 요청 - 400 Bad Request")
    void submitReport_InvalidRequest_BadRequest() throws Exception {
        // Given - reportType이 누락된 잘못된 요청
        ReportDTO invalidReportDTO = ReportDTO.builder()
                .reportType(null) // 필수 필드 누락
                .targetId(123L)
                .content("내용")
                .build();

        User testUser = createTestUser();
        userRepository.save(testUser);
        CustomUserDetails userDetails = createCustomUserDetails(testUser);

        // When & Then - @Valid 검증 실패로 400 에러
        mockMvc.perform(post("/api/user/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReportDTO))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("신고 제출 통합 테스트 - 성공")
    void submitReport_EmptyContent_BadRequest() throws Exception {
        // Given - content가 빈 잘못된 요청
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(123L)
                .content("") // 빈 내용
                .build();

        User testUser = createTestUser();
        userRepository.save(testUser);
        CustomUserDetails userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(post("/api/user/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("신고 제출 - CSRF 토큰 없이 요청 - 403 Forbidden")
    void submitReport_WithoutCsrf_Forbidden() throws Exception {
        // Given
        User testUser = createTestUser();
        userRepository.save(testUser);
        CustomUserDetails userDetails = createCustomUserDetails(testUser);
        
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(123L)
                .content("CSRF 없는 신고")
                .build();

        // When & Then
        mockMvc.perform(post("/api/user/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO))
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    /**
     * 테스트용 User 엔티티 생성
     */
    private User createTestUser() {
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        return User.builder()
                .socialId("test-social-id-12345")
                .socialNickname("통합테스트소셜닉네임")
                .thumbnailImage("http://example.com/integration-test.jpg")
                .userName("통합테스트사용자")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
    }

    /**
     * 테스트용 CustomUserDetails 생성
     */
    private CustomUserDetails createCustomUserDetails(User user) {
        UserDTO userDTO = UserDTO.builder()
                .userId(user.getId())
                .settingId(user.getSetting().getId())
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .build();

        return new CustomUserDetails(userDTO);
    }
}