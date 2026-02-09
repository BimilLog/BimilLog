package jaeik.bimillog.domain.post.controller;

import jaeik.bimillog.domain.post.entity.jpa.Post;

import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>게시글 Admin 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Post Admin API 통합 테스트</p>
 * <p>관리자 권한이 필요한 공지 설정/해제 API 동작을 검증</p>
 * <p>공지 상태는 Post.isNotice 플래그로 관리됨</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@IntegrationTest
@DisplayName("게시글 Admin 컨트롤러 통합 테스트")
class PostAdminControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    private Post testPost;

    @Override
    protected void setUpChild() {
        createTestPost();
    }

    private void createTestPost() {
        testPost = PostTestDataBuilder.createPost(testMember, "테스트 게시글", "테스트 게시글 내용입니다.");
        testPost = postRepository.save(testPost);
    }

    /**
     * 해당 게시글이 공지사항인지 확인 (Post.isNotice 플래그 기반)
     */
    private boolean isNotice(Long postId) {
        return postRepository.findById(postId)
                .map(Post::isNotice)
                .orElse(false);
    }

    /**
     * 해당 게시글을 공지사항으로 설정 (테스트 setup용)
     */
    private void setAsNotice(Post post) {
        post.updateNotice(true);
        postRepository.save(post);
    }

    @Test
    @DisplayName("게시글 공지 토글 성공 - 관리자 권한 (비공지 -> 공지)")
    void togglePostNotice_Success_WithAdminRole_NormalToNotice() throws Exception {
        // Given - 초기상태: 비공지
        assertThat(isNotice(testPost.getId())).isFalse();

        // When & Then
        performPost("/api/post/" + testPost.getId() + "/notice", null, adminUserDetails)
                .andDo(print())
                .andExpect(status().isOk());

        // 실제 DB 확인 - 공지로 변경됨
        assertThat(isNotice(testPost.getId())).isTrue();
    }

    @Test
    @DisplayName("게시글 공지 토글 성공 - 관리자 권한 (공지 -> 비공지)")
    void togglePostNotice_Success_WithAdminRole_NoticeToNormal() throws Exception {
        // Given - 먼저 공지로 설정
        setAsNotice(testPost);
        assertThat(isNotice(testPost.getId())).isTrue();

        // When & Then
        performPost("/api/post/" + testPost.getId() + "/notice", null, adminUserDetails)
                .andDo(print())
                .andExpect(status().isOk());

        // 실제 DB 확인 - 비공지로 변경됨
        assertThat(isNotice(testPost.getId())).isFalse();
    }

    @Test
    @DisplayName("게시글 공지 토글 실패 - 일반 사용자 권한 없음")
    void togglePostNotice_Fail_WithMemberRole() throws Exception {
        // When & Then - 403 Forbidden 예상
        performPost("/api/post/" + testPost.getId() + "/notice", null, testUserDetails)
                .andDo(print())
                .andExpect(status().isForbidden());

        // DB 확인 - 상태 변경되지 않음
        assertThat(isNotice(testPost.getId())).isFalse();
    }

    @Test
    @DisplayName("게시글 공지 토글 실패 - 인증되지 않은 사용자")
    void togglePostNotice_Fail_Unauthorized() throws Exception {
        // When & Then - 403 Forbidden 예상
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/post/" + testPost.getId() + "/notice")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());

        // DB 확인 - 상태 변경되지 않음
        assertThat(isNotice(testPost.getId())).isFalse();
    }

    @Test
    @DisplayName("게시글 공지 토글 실패 - 존재하지 않는 게시글")
    void togglePostNotice_Fail_PostNotFound() throws Exception {
        // Given
        Long nonExistentPostId = Long.MAX_VALUE;

        // When & Then - 404 Not Found 예상 (CustomException -> PostExceptionHandler)
        performPost("/api/post/" + nonExistentPostId + "/notice", null, adminUserDetails)
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 공지 토글 - 연속 두 번 토글 (멱등성 확인)")
    void togglePostNotice_TwiceToggle_Idempotency() throws Exception {
        // Given - 초기 비공지 상태
        assertThat(isNotice(testPost.getId())).isFalse();

        // When & Then - 첫 번째 토글: 비공지 -> 공지
        performPost("/api/post/" + testPost.getId() + "/notice", null, adminUserDetails)
                .andDo(print())
                .andExpect(status().isOk());

        // DB 확인 - 공지로 변경
        assertThat(isNotice(testPost.getId())).isTrue();

        // When & Then - 두 번째 토글: 공지 -> 비공지
        performPost("/api/post/" + testPost.getId() + "/notice", null, adminUserDetails)
                .andDo(print())
                .andExpect(status().isOk());

        // DB 확인 - 비공지로 되돌아감
        assertThat(isNotice(testPost.getId())).isFalse();
    }

    @Test
    @DisplayName("게시글 공지 토글 - 세 번 토글 시나리오")
    void togglePostNotice_ThreeTimes() throws Exception {
        // Given - 초기 비공지 상태
        assertThat(isNotice(testPost.getId())).isFalse();

        // 첫 번째 토글: 비공지 -> 공지
        performPost("/api/post/" + testPost.getId() + "/notice", null, adminUserDetails)
                .andExpect(status().isOk());
        assertThat(isNotice(testPost.getId())).isTrue();

        // 두 번째 토글: 공지 -> 비공지
        performPost("/api/post/" + testPost.getId() + "/notice", null, adminUserDetails)
                .andExpect(status().isOk());
        assertThat(isNotice(testPost.getId())).isFalse();

        // 세 번째 토글: 비공지 -> 공지
        performPost("/api/post/" + testPost.getId() + "/notice", null, adminUserDetails)
                .andExpect(status().isOk());
        assertThat(isNotice(testPost.getId())).isTrue();
    }

    @Test
    @DisplayName("게시글 공지 토글 성공 - 정상 케이스 추가 검증")
    void togglePostNotice_Success_AdditionalVerification() throws Exception {
        // Given - 초기상태: 비공지
        assertThat(isNotice(testPost.getId())).isFalse();

        // When & Then - API 호출 성공
        performPost("/api/post/" + testPost.getId() + "/notice", null, adminUserDetails)
                .andDo(print())
                .andExpect(status().isOk());

        // 실제 DB 확인 - 공지로 변경됨
        assertThat(isNotice(testPost.getId())).isTrue();
    }

}
