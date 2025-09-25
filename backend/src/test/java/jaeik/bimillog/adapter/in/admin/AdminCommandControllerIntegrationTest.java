package jaeik.bimillog.adapter.in.admin;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.in.admin.dto.ReportDTO;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.H2TestConfiguration;
import jaeik.bimillog.testutil.PostTestDataBuilder;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>관리자 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 관리자 Command API 통합 테스트</p>
 * <p>H2 인메모리 데이터베이스를 사용하여 빠른 테스트 실행</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@ActiveProfiles("h2test")
@Import(H2TestConfiguration.class)
@DisplayName("관리자 Command 컨트롤러 통합 테스트 (H2)")
class AdminCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Override
    protected void setUpChild() {
        if (adminUser != null) {
            adminUserDetails = createCustomUserDetails(adminUser);
        }
    }

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("관리자 권한으로 사용자 차단 - 성공")
    void banUser_WithAdminRole_Success() throws Exception {
        // Given - 테스트용 사용자와 게시글 생성
        User testTargetUser = userRepository.save(TestUsers.createUniqueWithPrefix("target"));

        Post testPost = PostTestDataBuilder.createPost(testTargetUser, "테스트 게시글", "테스트 내용");
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
    @DisplayName("관리자 권한으로 사용자 강제 탈퇴 - 성공")
    void forceWithdrawUser_WithAdminRole_Success() throws Exception {
        // Given
        User targetUser = userRepository.save(TestUsers.createUniqueWithPrefix("withdraw"));

        Post testPost = PostTestDataBuilder.createPost(targetUser, "테스트 게시글", "테스트 내용");
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
    @DisplayName("존재하지 않는 게시글로 사용자 강제 탈퇴 - 실패")
    void forceWithdrawUser_UserNotFound_NotFound() throws Exception {
        // Given
        Long nonExistentPostId = 99999L;

        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(nonExistentPostId)
                .content("존재하지 않는 게시글 신고")
                .build();

        // When & Then
        performPost("/api/admin/withdraw", reportDTO, adminUserDetails)
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
