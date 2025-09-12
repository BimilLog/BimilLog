package jaeik.bimillog.integration.api.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.global.entity.UserDetail;
import jaeik.bimillog.infrastructure.adapter.post.dto.PostCreateDTO;
import jaeik.bimillog.infrastructure.adapter.post.dto.PostUpdateDTO;
import jaeik.bimillog.infrastructure.adapter.post.out.jpa.PostRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.UserRepository;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>게시글 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Post Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>게시글 작성, 수정, 삭제, 추천 API 동작을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("게시글 Command 컨트롤러 통합 테스트")
class PostCommandControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private CustomUserDetails testUser;
    private User savedUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 테스트용 사용자 생성 및 저장
        User user = User.builder()
                .socialId("12345")
                .userName("테스트사용자")
                .thumbnailImage("http://test-profile.jpg")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(Setting.createSetting())
                .build();
        
        savedUser = userRepository.save(user);
        
        // UserDetail 생성하여 CustomUserDetails 생성
        UserDetail userDetail = UserDetail.builder()
                .userId(savedUser.getId())
                .userName(savedUser.getUserName())
                .role(savedUser.getRole())
                .socialId(savedUser.getSocialId())
                .provider(savedUser.getProvider())
                .settingId(savedUser.getSetting().getId())
                .socialNickname(savedUser.getSocialNickname())
                .thumbnailImage(savedUser.getThumbnailImage())
                .tokenId(1L) // 테스트용
                .fcmTokenId(null)
                .build();
                
        testUser = new CustomUserDetails(userDetail);
    }

    @Test
    @DisplayName("게시글 작성 성공 - 유효한 데이터")
    void writePost_Success() throws Exception {
        // Given
        PostCreateDTO postCreateDTO = PostCreateDTO.builder()
                .title("통합 테스트 게시글")
                .content("게시글 작성 통합 테스트 내용입니다.")
                .build();

        // When & Then
        mockMvc.perform(post("/api/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postCreateDTO))
                        .with(user(testUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        // DB에 실제로 저장되었는지 확인
        Optional<Post> savedPost = postRepository.findAll().stream()
                .filter(post -> "통합 테스트 게시글".equals(post.getTitle()))
                .findFirst();
        
        assertThat(savedPost).isPresent();
        assertThat(savedPost.get().getContent()).isEqualTo("게시글 작성 통합 테스트 내용입니다.");
        assertThat(savedPost.get().getUser().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("게시글 작성 실패 - 제목 누락")
    void writePost_Fail_NoTitle() throws Exception {
        // Given
        PostCreateDTO postCreateDTO = PostCreateDTO.builder()
                .content("제목이 없는 게시글 내용")
                .password("1234")
                .build();

        // When & Then
        mockMvc.perform(post("/api/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postCreateDTO))
                        .with(user(testUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("게시글 작성 성공 - 비로그인 사용자 (익명 작성)")
    void writePost_Success_Anonymous() throws Exception {
        // Given
        PostCreateDTO postCreateDTO = PostCreateDTO.builder()
                .title("익명 게시글")
                .content("비로그인 사용자의 익명 게시글 작성")
                .password("1234")
                .build();

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
        // Given - 기존 게시글 생성
        Post existingPost = Post.builder()
                .user(savedUser)
                .title("수정 전 제목")
                .content("수정 전 내용")
                .password(123456)
                .views(0)
                .isNotice(false)
                .build();
        
        Post savedPost = postRepository.save(existingPost);

        PostUpdateDTO updateReqDTO = PostUpdateDTO.builder()
                .title("수정된 제목")
                .content("수정된 내용입니다. 10자 이상으로 작성합니다.")
                .build();

        // When & Then
        mockMvc.perform(put("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqDTO))
                        .with(user(testUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // DB에서 수정된 내용 확인
        Optional<Post> updatedPost = postRepository.findById(savedPost.getId());
        assertThat(updatedPost).isPresent();
        assertThat(updatedPost.get().getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedPost.get().getContent()).isEqualTo("수정된 내용입니다. 10자 이상으로 작성합니다.");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 다른 사용자의 게시글")
    void updatePost_Fail_NotAuthor() throws Exception {
        // Given - 다른 사용자의 게시글 생성
        User anotherUser = User.builder()
                .socialId("99999")
                .userName("다른사용자")
                .thumbnailImage("http://another-profile.jpg")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(Setting.createSetting())
                .build();
        
        User savedAnotherUser = userRepository.save(anotherUser);

        Post anotherPost = Post.builder()
                .user(savedAnotherUser)
                .title("다른 사용자 게시글")
                .content("다른 사용자가 작성한 게시글")
                .password(123456)
                .views(0)
                .isNotice(false)
                .build();
        
        Post savedPost = postRepository.save(anotherPost);

        PostUpdateDTO updateReqDTO = PostUpdateDTO.builder()
                .title("수정 시도")
                .content("수정 시도 내용입니다. 10자 이상으로 작성합니다.")
                .build();

        // When & Then
        mockMvc.perform(put("/api/post/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqDTO))
                        .with(user(testUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() throws Exception {
        // Given
        Post existingPost = Post.builder()
                .user(savedUser)
                .title("삭제할 게시글")
                .content("삭제할 게시글 내용")
                .password(123456)
                .views(0)
                .isNotice(false)
                .build();
        
        Post savedPost = postRepository.save(existingPost);

        // When & Then
        mockMvc.perform(delete("/api/post/{postId}", savedPost.getId())
                        .with(user(testUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // DB에서 삭제되었는지 확인
        Optional<Post> deletedPost = postRepository.findById(savedPost.getId());
        assertThat(deletedPost).isEmpty();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 존재하지 않는 게시글")
    void deletePost_Fail_NotFound() throws Exception {
        // Given
        Long nonExistentPostId = 99999L;

        // When & Then
        mockMvc.perform(delete("/api/post/{postId}", nonExistentPostId)
                        .with(user(testUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 추천 성공")
    void likePost_Success() throws Exception {
        // Given
        Post existingPost = Post.builder()
                .user(savedUser)
                .title("추천할 게시글")
                .content("추천할 게시글 내용")
                .password(123456)
                .views(0)
                .isNotice(false)
                .build();
        
        Post savedPost = postRepository.save(existingPost);

        // When & Then
        mockMvc.perform(post("/api/post/{postId}/like", savedPost.getId())
                        .with(user(testUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 추천 실패 - 존재하지 않는 게시글")
    void likePost_Fail_NotFound() throws Exception {
        // Given
        Long nonExistentPostId = 99999L;

        // When & Then
        mockMvc.perform(post("/api/post/{postId}/like", nonExistentPostId)
                        .with(user(testUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 추천 실패 - 인증되지 않은 사용자")
    void likePost_Fail_Unauthenticated() throws Exception {
        // Given
        Post existingPost = Post.builder()
                .user(savedUser)
                .title("추천할 게시글")
                .content("추천할 게시글 내용")
                .password(123456)
                .views(0)
                .isNotice(false)
                .build();
        
        Post savedPost = postRepository.save(existingPost);

        // When & Then - 인증되지 않은 사용자는 500 에러 (NullPointerException)가 발생
        mockMvc.perform(post("/api/post/{postId}/like", savedPost.getId())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글 작성 - 적당한 길이의 제목과 내용")
    void writePost_LongTitleAndContent() throws Exception {
        // Given - PostCreateDTO 검증: title 최대 30자, content 최대 1000자
        String title = "a".repeat(30); // 제한 내의 제목
        String content = "가".repeat(1000); // 제한 내의 내용

        PostCreateDTO postCreateDTO = PostCreateDTO.builder()
                .title(title)
                .content(content)
                .build();

        // When & Then
        mockMvc.perform(post("/api/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postCreateDTO))
                        .with(user(testUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated());

        // DB에 저장된 내용 확인
        Optional<Post> savedPost = postRepository.findAll().stream()
                .filter(post -> title.equals(post.getTitle()))
                .findFirst();
        
        assertThat(savedPost).isPresent();
        assertThat(savedPost.get().getContent()).isEqualTo(content);
    }
}