package jaeik.bimillog.adapter.in.post;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.in.dto.PostCreateDTO;
import jaeik.bimillog.domain.post.in.dto.PostUpdateDTO;
import jaeik.bimillog.domain.auth.out.CustomUserDetails;
import jaeik.bimillog.domain.post.out.PostRepository;
import jaeik.bimillog.testutil.*;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import jaeik.bimillog.testutil.config.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>게시글 Command 컨트롤러 통합 테스트</h2>
 * <p>Redis 동기화가 포함된 게시글 명령 API를 TestContainers 환경에서 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@IntegrationTest
@Import(TestSocialLoginPortConfig.class)
@DisplayName("게시글 Command 컨트롤러 통합 테스트")
class PostCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    private Member savedMember;
    private CustomUserDetails savedUserDetails;

    @Override
    protected void setUpChild() {
        savedMember = testMember;
        savedUserDetails = testUserDetails;
    }

    @Test
    @DisplayName("게시글 작성 성공 - 유효한 데이터")
    void writePost_Success() throws Exception {
        PostCreateDTO postCreateDTO = TestFixtures.createPostRequest(
                "통합 테스트 게시글",
                "게시글 작성 통합 테스트 내용입니다."
        );

        mockMvc.perform(post("/api/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postCreateDTO))
                        .with(user(savedUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        Optional<Post> savedPost = postRepository.findAll().stream()
                .filter(post -> "통합 테스트 게시글".equals(post.getTitle()))
                .findFirst();

        assertThat(savedPost).isPresent();
        assertThat(savedPost.get().getContent()).isEqualTo("게시글 작성 통합 테스트 내용입니다.");
        assertThat(savedPost.get().getMember().getId()).isEqualTo(savedMember.getId());
    }

    @Test
    @DisplayName("게시글 작성 실패 - 제목 누락")
    void writePost_Fail_NoTitle() throws Exception {
        PostCreateDTO postCreateDTO = PostCreateDTO.builder()
                .content("제목이 없는 게시글 내용")
                .password(1234)
                .build();

        mockMvc.perform(post("/api/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postCreateDTO))
                        .with(user(savedUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("게시글 작성 성공 - 비로그인 사용자 (익명 작성)")
    void writePost_Success_Anonymous() throws Exception {
        PostCreateDTO postCreateDTO = TestFixtures.createPostRequest(
                "익명 게시글",
                "비로그인 사용자의 익명 게시글 작성"
        );

        mockMvc.perform(post("/api/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postCreateDTO))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() throws Exception {
        Post existingPost = PostTestDataBuilder.createPost(savedMember, "수정 전 제목", "수정 전 내용");
        Post savedPost = postRepository.save(existingPost);

        PostUpdateDTO updateReqDTO = PostUpdateDTO.builder()
                .title("수정된 제목")
                .content("수정된 내용입니다. 10자 이상으로 작성합니다.")
                .build();;

        mockMvc.perform(put("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqDTO))
                        .with(user(savedUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        Optional<Post> updatedPost = postRepository.findById(savedPost.getId());
        assertThat(updatedPost).isPresent();
        assertThat(updatedPost.get().getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedPost.get().getContent()).isEqualTo("수정된 내용입니다. 10자 이상으로 작성합니다.");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 다른 사용자의 게시글")
    void updatePost_Fail_NotAuthor() throws Exception {
        Member anotherMember = saveMember(TestMembers.createUniqueWithPrefix("another"));

        Post anotherPost = PostTestDataBuilder.createPost(
                anotherMember,
                "다른 사용자 게시글",
                "다른 사용자가 작성한 게시글"
        );
        Post savedPost = postRepository.save(anotherPost);

        PostUpdateDTO updateReqDTO = PostUpdateDTO.builder()
                .title("수정 시도")
                .content("수정 시도 내용입니다. 10자 이상으로 작성합니다.")
                .build();;

        mockMvc.perform(put("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqDTO))
                        .with(user(savedUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() throws Exception {
        Post existingPost = PostTestDataBuilder.createPost(
                savedMember,
                "삭제할 게시글",
                "삭제할 게시글 내용"
        );
        Post savedPost = postRepository.save(existingPost);

        mockMvc.perform(delete("/api/post/{postId}", savedPost.getId())
                        .with(user(savedUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());

        Optional<Post> deletedPost = postRepository.findById(savedPost.getId());
        assertThat(deletedPost).isEmpty();
    }

    @Test
    @DisplayName("게시글 삭제 성공 - 익명 게시글 (비밀번호 일치)")
    void deletePost_Success_Anonymous() throws Exception {
        Post anonymousPost = Post.createPost(
                null,  // 익명 게시글 (member = null)
                "익명 게시글",
                "익명 게시글 내용",
                1234
        );
        Post savedPost = postRepository.save(anonymousPost);

        String requestBody = """
                {
                    "password": "1234"
                }
                """;

        mockMvc.perform(delete("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());

        Optional<Post> deletedPost = postRepository.findById(savedPost.getId());
        assertThat(deletedPost).isEmpty();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 익명 게시글 (비밀번호 불일치)")
    void deletePost_Fail_Anonymous_WrongPassword() throws Exception {
        Post anonymousPost = Post.createPost(
                null,  // 익명 게시글 (member = null)
                "익명 게시글",
                "익명 게시글 내용",
                1234
        );
        Post savedPost = postRepository.save(anonymousPost);

        String requestBody = """
                {
                    "password": "9999"
                }
                """;

        mockMvc.perform(delete("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());

        // 게시글이 삭제되지 않아야 함
        Optional<Post> post = postRepository.findById(savedPost.getId());
        assertThat(post).isPresent();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 익명 게시글 (비밀번호 누락)")
    void deletePost_Fail_Anonymous_NoPassword() throws Exception {
        Post anonymousPost = Post.createPost(
                null,  // 익명 게시글 (member = null)
                "익명 게시글",
                "익명 게시글 내용",
                1234
        );
        Post savedPost = postRepository.save(anonymousPost);

        mockMvc.perform(delete("/api/post/{postId}", savedPost.getId())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 게시글이 삭제되지 않아야 함
        Optional<Post> post = postRepository.findById(savedPost.getId());
        assertThat(post).isPresent();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 존재하지 않는 게시글")
    void deletePost_Fail_NotFound() throws Exception {
        mockMvc.perform(delete("/api/post/{postId}", 99999L)
                        .with(user(savedUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 추천 성공")
    void likePost_Success() throws Exception {
        Post existingPost = PostTestDataBuilder.createPost(
                savedMember,
                "추천할 게시글",
                "추천할 게시글 내용"
        );
        Post savedPost = postRepository.save(existingPost);

        mockMvc.perform(post("/api/post/{postId}/like", savedPost.getId())
                        .with(user(savedUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 추천 실패 - 존재하지 않는 게시글")
    void likePost_Fail_NotFound() throws Exception {
        mockMvc.perform(post("/api/post/{postId}/like", 99999L)
                        .with(user(savedUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 추천 실패 - 인증되지 않은 사용자")
    void likePost_Fail_Unauthenticated() throws Exception {
        Post existingPost = PostTestDataBuilder.createPost(
                savedMember,
                "추천할 게시글",
                "추천할 게시글 내용"
        );
        Post savedPost = postRepository.save(existingPost);

        mockMvc.perform(post("/api/post/{postId}/like", savedPost.getId())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글 작성 - 적당한 길이의 제목과 내용")
    void writePost_LongTitleAndContent() throws Exception {
        String title = "a".repeat(30);
        String content = "가".repeat(1000);

        PostCreateDTO postCreateDTO = TestFixtures.createPostRequest(title, content);

        mockMvc.perform(post("/api/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postCreateDTO))
                        .with(user(savedUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated());

        Optional<Post> savedPost = postRepository.findAll().stream()
                .filter(post -> title.equals(post.getTitle()))
                .findFirst();

        assertThat(savedPost).isPresent();
        assertThat(savedPost.get().getContent()).isEqualTo(content);
    }

    @Test
    @DisplayName("비회원 게시글 수정 성공 - 올바른 비밀번호")
    void updateAnonymousPost_Success_WithCorrectPassword() throws Exception {
        Post anonymousPost = Post.createPost(null, "익명 게시글", "익명 게시글 내용입니다.", 1234);
        Post savedPost = postRepository.save(anonymousPost);

        PostUpdateDTO updateReqDTO = PostUpdateDTO.builder()
                .title("수정된 익명 게시글")
                .content("수정된 익명 게시글 내용입니다. 10자 이상으로 작성합니다.")
                .password(1234)
                .build();

        mockMvc.perform(put("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqDTO))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        Optional<Post> updatedPost = postRepository.findById(savedPost.getId());
        assertThat(updatedPost).isPresent();
        assertThat(updatedPost.get().getTitle()).isEqualTo("수정된 익명 게시글");
        assertThat(updatedPost.get().getContent()).isEqualTo("수정된 익명 게시글 내용입니다. 10자 이상으로 작성합니다.");
    }

    @Test
    @DisplayName("비회원 게시글 수정 실패 - 잘못된 비밀번호")
    void updateAnonymousPost_Fail_WithWrongPassword() throws Exception {
        Post anonymousPost = Post.createPost(null, "익명 게시글", "익명 게시글 내용입니다.", 1234);
        Post savedPost = postRepository.save(anonymousPost);

        PostUpdateDTO updateReqDTO = PostUpdateDTO.builder()
                .title("수정된 익명 게시글")
                .content("수정된 익명 게시글 내용입니다. 10자 이상으로 작성합니다.")
                .password(9999)
                .build();

        mockMvc.perform(put("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqDTO))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비회원 게시글 수정 실패 - 비밀번호 없음")
    void updateAnonymousPost_Fail_WithoutPassword() throws Exception {
        Post anonymousPost = Post.createPost(null, "익명 게시글", "익명 게시글 내용입니다.", 1234);
        Post savedPost = postRepository.save(anonymousPost);

        PostUpdateDTO updateReqDTO = PostUpdateDTO.builder()
                .title("수정된 익명 게시글")
                .content("수정된 익명 게시글 내용입니다. 10자 이상으로 작성합니다.")
                .build();

        mockMvc.perform(put("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqDTO))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원이 비회원 게시글 수정 실패")
    void updateAnonymousPost_Fail_WithMemberUser() throws Exception {
        Post anonymousPost = Post.createPost(null, "익명 게시글", "익명 게시글 내용입니다.", 1234);
        Post savedPost = postRepository.save(anonymousPost);

        PostUpdateDTO updateReqDTO = PostUpdateDTO.builder()
                .title("수정 시도")
                .content("수정 시도 내용입니다. 10자 이상으로 작성합니다.")
                .build();

        mockMvc.perform(put("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqDTO))
                        .with(user(savedUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비회원이 회원 게시글 수정 실패")
    void updateMemberPost_Fail_WithAnonymousUser() throws Exception {
        Post memberPost = PostTestDataBuilder.createPost(savedMember, "회원 게시글", "회원 게시글 내용");
        Post savedPost = postRepository.save(memberPost);

        PostUpdateDTO updateReqDTO = PostUpdateDTO.builder()
                .title("수정 시도")
                .content("수정 시도 내용입니다. 10자 이상으로 작성합니다.")
                .password(1234)
                .build();

        mockMvc.perform(put("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqDTO))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
