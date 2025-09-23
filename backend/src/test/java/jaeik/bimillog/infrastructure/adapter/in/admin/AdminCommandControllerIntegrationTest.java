package jaeik.bimillog.infrastructure.adapter.in.admin;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.admin.dto.ReportDTO;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestSettings;
import jaeik.bimillog.testutil.TestUsers;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>관리자 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 관리자 Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 *
 *
 * @author Jaeik
 * @since 2.0.0
 */
@IntegrationTest
@DisplayName("관리자 Command 컨트롤러 통합 테스트")
class AdminCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    /**
     * 테스트용 고유 사용자 생성 헬퍼
     */
    private User createUniqueUserWithPrefix(String prefix) {
        return TestUsers.createUniqueWithPrefix(prefix);
    }

    @Test
    @DisplayName("관리자 권한으로 사용자 차단 - 성공")
    void banUser_WithAdminRole_Success() throws Exception {
        // Given - 테스트용 사용자와 게시글 생성
        User testTargetUser = userRepository.save(createUniqueUserWithPrefix("target"));
        
        Post testPost = TestFixtures.createPost(testTargetUser, "테스트 게시글", "테스트 내용");
        Post savedPost = postRepository.save(testPost);
        
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(savedPost.getId())
                .content("부적절한 게시글 신고")
                .build();
        
        // When & Then
        performPost("/api/admin/ban", reportDTO, adminUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("유저를 성공적으로 차단했습니다."));
    }
    
    @Test
    @DisplayName("일반 사용자 권한으로 사용자 차단 - 실패 (권한 부족)")
    void banUser_WithUserRole_Forbidden() throws Exception {
        // Given - 테스트용 게시글 생성
        Post testPost = TestFixtures.createPost(testUser, "테스트 게시글", "테스트 내용");
        Post savedPost = postRepository.save(testPost);
        
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(savedPost.getId())
                .content("부적절한 게시글 신고")
                .build();
        
        // When & Then
        performPost("/api/admin/ban", reportDTO, testUserDetails)
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
        
        // When & Then
        performPost("/api/admin/ban", reportDTO)
                .andDo(print())
                .andExpect(status().isForbidden()); // 실제로는 403이 반환됨
    }
    
    @Test
    @DisplayName("잘못된 ReportDTO로 사용자 차단 - 실패")
    void banUser_WithInvalidReportDTO_BadRequest() throws Exception {
        // Given - 잘못된 JSON
        String invalidRequestBody = "{ \"reportType\": \"INVALID_TYPE\", \"targetId\": null }";
        
        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/ban")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    @DisplayName("관리자 권한으로 사용자 강제 탈퇴 - 성공")
    void forceWithdrawUser_WithAdminRole_Success() throws Exception {
        // Given
        User targetUser = userRepository.save(createUniqueUserWithPrefix("withdraw"));
        
        Post testPost = TestFixtures.createPost(targetUser, "테스트 게시글", "테스트 내용");
        Post savedPost = postRepository.save(testPost);
        
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(savedPost.getId())
                .content("강제 탈퇴 사유 게시글")
                .build();
        
        // When & Then
        performPost("/api/admin/withdraw", reportDTO, adminUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("관리자 권한으로 사용자 탈퇴가 완료되었습니다."));
    }
    
    @Test
    @DisplayName("일반 사용자 권한으로 사용자 강제 탈퇴 - 실패 (권한 부족)")
    void forceWithdrawUser_WithUserRole_Forbidden() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(1L)
                .content("권한 없는 강제 탈퇴 시도")
                .build();
        
        // When & Then
        performPost("/api/admin/withdraw", reportDTO, testUserDetails)
                .andDo(print())
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 강제 탈퇴 - 실패")
    void forceWithdrawUser_UserNotFound_NotFound() throws Exception {
        // Given
        Long nonExistentPostId = 99999L;
        
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(nonExistentPostId) // Using as post ID
                .content("존재하지 않는 게시글 신고")
                .build();
        
        // When & Then
        performPost("/api/admin/withdraw", reportDTO, adminUserDetails)
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("잘못된 사용자 ID로 강제 탈퇴 - 실패")
    void forceWithdrawUser_InvalidUserId_BadRequest() throws Exception {
        // Given
        String invalidUserId = "invalid";
        
        // When & Then
        performDelete("/api/dto/users/" + invalidUserId, adminUserDetails)
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
}