package jaeik.bimillog.springboot.h2;

import jaeik.bimillog.domain.admin.dto.BanUserDTO;
import jaeik.bimillog.domain.admin.dto.ForceWithdrawDTO;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
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
@Tag("integration")
class AdminCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Override
    protected void setUpChild() {
        if (adminMember != null) {
            adminUserDetails = createCustomUserDetails(adminMember);
        }
    }

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("관리자 권한으로 사용자 차단 - 성공")
    void banUser_WithAdminRole_Success() throws Exception {
        // Given - 테스트용 사용자와 게시글 생성
        Member testTargetMember = saveMember(TestMembers.createUniqueWithPrefix("target"));

        Post testPost = PostTestDataBuilder.createPost(testTargetMember, "테스트 게시글", "테스트 내용");
        Post savedPost = postRepository.save(testPost);

        BanUserDTO banUserDTO = BanUserDTO.builder()
                .reportType(ReportType.POST)
                .targetId(savedPost.getId())
                .build();

        // When & Then
        performPost("/api/admin/ban", banUserDTO, adminUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("유저를 성공적으로 차단했습니다."));
    }

    @Test
    @DisplayName("관리자 권한으로 사용자 강제 탈퇴 - 성공")
    void forceWithdrawUser_WithAdminRole_Success() throws Exception {
        // Given
        Member targetMember = saveMember(TestMembers.createUniqueWithPrefix("withdraw"));

        Post testPost = PostTestDataBuilder.createPost(targetMember, "테스트 게시글", "테스트 내용");
        Post savedPost = postRepository.save(testPost);

        ForceWithdrawDTO forceWithdrawDTO = ForceWithdrawDTO.builder()
                .reportType(ReportType.POST)
                .targetId(savedPost.getId())
                .build();

        // When & Then
        performPost("/api/admin/withdraw", forceWithdrawDTO, adminUserDetails)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("사용자 탈퇴 처리를 시작했습니다. 백그라운드에서 처리됩니다."));
    }

    @Test
    @DisplayName("존재하지 않는 게시글로 사용자 강제 탈퇴 - 실패")
    void forceWithdrawUser_PostNotFound_BadRequest() throws Exception {
        // Given
        Long nonExistentPostId = 99999L;

        ForceWithdrawDTO forceWithdrawDTO = ForceWithdrawDTO.builder()
                .reportType(ReportType.POST)
                .targetId(nonExistentPostId)
                .build();

        // When & Then
        // 존재하지 않는 게시글 조회 실패 시 ADMIN_POST_ALREADY_DELETED 에러 반환 (400 BAD_REQUEST)
        performPost("/api/admin/withdraw", forceWithdrawDTO, adminUserDetails)
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
