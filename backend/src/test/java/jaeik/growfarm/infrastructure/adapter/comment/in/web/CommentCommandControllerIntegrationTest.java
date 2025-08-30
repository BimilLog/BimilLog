package jaeik.growfarm.infrastructure.adapter.comment.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentReqDTO;
import jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.comment.CommentRepository;
import jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.post.PostJpaRepository;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import jaeik.growfarm.infrastructure.adapter.user.out.social.dto.UserDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.util.TestContainersConfiguration;
import jaeik.growfarm.util.TestSocialLoginPortConfig;
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

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>댓글 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Comment Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>댓글 작성, 수정, 삭제, 추천 API 동작을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("댓글 Command 컨트롤러 통합 테스트")
class CommentCommandControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostJpaRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    private MockMvc mockMvc;
    private User testUser;
    private Post testPost;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        // 테스트용 사용자 생성
        testUser = createTestUser();
        userRepository.save(testUser);
        
        // 테스트용 게시글 생성
        testPost = createTestPost(testUser);
        postRepository.save(testPost);
    }
    
    @Test
    @DisplayName("댓글 작성 통합 테스트 - 로그인 사용자")
    void writeComment_LoggedInUser_IntegrationTest() throws Exception {
        // Given
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setPostId(testPost.getId());
        requestDto.setContent("통합 테스트용 댓글입니다.");
        
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(post("/api/comment/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(user(userDetails))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("댓글 작성 완료"));
        
        // 데이터베이스 검증
        Optional<Comment> savedComment = commentRepository.findAll()
                .stream()
                .filter(comment -> "통합 테스트용 댓글입니다.".equals(comment.getContent()))
                .findFirst();
        
        assertThat(savedComment).isPresent();
        assertThat(savedComment.get().getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedComment.get().getPost().getId()).isEqualTo(testPost.getId());
    }
    
    @Test
    @DisplayName("대댓글 작성 통합 테스트")
    void writeReplyComment_IntegrationTest() throws Exception {
        // Given - 부모 댓글 생성
        Comment parentComment = createTestComment(testUser, testPost);
        commentRepository.save(parentComment);
        
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setPostId(testPost.getId());
        requestDto.setParentId(parentComment.getId());
        requestDto.setContent("대댓글 테스트입니다.");
        
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(post("/api/comment/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(user(userDetails))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("댓글 작성 완료"));
        
        // 데이터베이스 검증
        Optional<Comment> savedReply = commentRepository.findAll()
                .stream()
                .filter(comment -> "대댓글 테스트입니다.".equals(comment.getContent()))
                .findFirst();
        
        assertThat(savedReply).isPresent();
        // 대댓글의 부모 댓글 관계는 별도 테이블에서 관리됨
        // TODO: 댓글 계층 구조 확인 로직 추가 필요
    }
    
    @Test
    @DisplayName("댓글 수정 통합 테스트")
    void updateComment_IntegrationTest() throws Exception {
        // Given
        Comment existingComment = createTestComment(testUser, testPost);
        commentRepository.save(existingComment);
        
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setId(existingComment.getId());
        requestDto.setContent("수정된 댓글 내용입니다.");
        
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(post("/api/comment/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(user(userDetails))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("댓글 수정 완료"));
        
        // 데이터베이스 검증
        Comment updatedComment = commentRepository.findById(existingComment.getId()).orElseThrow();
        assertThat(updatedComment.getContent()).isEqualTo("수정된 댓글 내용입니다.");
    }
    
    @Test
    @DisplayName("댓글 삭제 통합 테스트")
    void deleteComment_IntegrationTest() throws Exception {
        // Given
        Comment existingComment = createTestComment(testUser, testPost);
        commentRepository.save(existingComment);
        
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setId(existingComment.getId());
        
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(post("/api/comment/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(user(userDetails))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("댓글 삭제 완료"));
        
        // 데이터베이스 검증 - 댓글 삭제 확인 (hard delete 또는 soft delete)
        Optional<Comment> deletedComment = commentRepository.findById(existingComment.getId());
        if (deletedComment.isPresent()) {
            // Soft delete 경우
            assertThat(deletedComment.get().isDeleted()).isTrue();
        } else {
            // Hard delete 경우 - 댓글이 완전히 삭제됨
            assertThat(deletedComment).isEmpty();
        }
    }
    
    @Test
    @DisplayName("댓글 추천 통합 테스트")
    void likeComment_IntegrationTest() throws Exception {
        // Given
        Comment existingComment = createTestComment(testUser, testPost);
        commentRepository.save(existingComment);
        
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setId(existingComment.getId());
        
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(post("/api/comment/like")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(user(userDetails))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("추천 처리 완료"));
    }
    
    @Test
    @DisplayName("댓글 작성 실패 - 잘못된 요청 데이터")
    void writeComment_InvalidRequest_IntegrationTest() throws Exception {
        // Given - 내용이 너무 긴 요청
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setPostId(testPost.getId());
        requestDto.setContent("A".repeat(1001)); // 1000자 초과
        
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(post("/api/comment/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(user(userDetails))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // === 누락된 댓글 삭제 통합 테스트 케이스들 ===

    @Test
    @DisplayName("익명 댓글 삭제 통합 테스트 - 패스워드 인증")
    void deleteAnonymousComment_IntegrationTest() throws Exception {
        // Given: 패스워드가 있는 익명 댓글 생성
        Comment anonymousComment = Comment.builder()
                .post(testPost)
                .user(null)
                .content("익명 댓글입니다")
                .password(1234)
                .deleted(false)
                .build();
        commentRepository.save(anonymousComment);
        
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setId(anonymousComment.getId());
        requestDto.setPassword(1234);
        
        // When & Then
        mockMvc.perform(post("/api/comment/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("댓글 삭제 완료"));
        
        // 데이터베이스 검증
        Optional<Comment> deletedComment = commentRepository.findById(anonymousComment.getId());
        if (deletedComment.isPresent()) {
            assertThat(deletedComment.get().isDeleted()).isTrue();
        } else {
            assertThat(deletedComment).isEmpty();
        }
    }

    @Test
    @DisplayName("익명 댓글 삭제 실패 - 잘못된 패스워드")
    void deleteAnonymousComment_WrongPassword_IntegrationTest() throws Exception {
        // Given: 패스워드가 있는 익명 댓글 생성
        Comment anonymousComment = Comment.builder()
                .post(testPost)
                .user(null)
                .content("익명 댓글입니다")
                .password(1234)
                .deleted(false)
                .build();
        commentRepository.save(anonymousComment);
        
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setId(anonymousComment.getId());
        requestDto.setPassword(9999); // 잘못된 패스워드
        
        // When & Then
        mockMvc.perform(post("/api/comment/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
        
        // 데이터베이스 검증 - 댓글이 삭제되지 않아야 함
        Optional<Comment> comment = commentRepository.findById(anonymousComment.getId());
        assertThat(comment).isPresent();
        assertThat(comment.get().isDeleted()).isFalse();
        assertThat(comment.get().getContent()).isEqualTo("익명 댓글입니다");
    }

    @Test
    @DisplayName("다른 사용자 댓글 삭제 시도 - 권한 없음")
    void deleteOtherUserComment_Unauthorized_IntegrationTest() throws Exception {
        // Given: 다른 사용자의 댓글
        User anotherUser = createTestUser();
        userRepository.save(anotherUser);
        
        Comment otherUserComment = Comment.builder()
                .post(testPost)
                .user(anotherUser)
                .content("다른 사용자의 댓글")
                .password(null)
                .deleted(false)
                .build();
        commentRepository.save(otherUserComment);
        
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setId(otherUserComment.getId());
        
        CustomUserDetails userDetails = createUserDetails(testUser); // 현재 사용자
        
        // When & Then
        mockMvc.perform(post("/api/comment/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(user(userDetails))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
        
        // 데이터베이스 검증 - 댓글이 삭제되지 않아야 함
        Optional<Comment> comment = commentRepository.findById(otherUserComment.getId());
        assertThat(comment).isPresent();
        assertThat(comment.get().isDeleted()).isFalse();
    }
    
    /**
     * 테스트용 사용자 생성
     */
    private User createTestUser() {
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        
        return User.builder()
                .socialId("12345")
                .socialNickname("테스트사용자")
                .thumbnailImage("test-profile.jpg")
                .userName("testuser")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
    }
    
    /**
     * 테스트용 게시글 생성
     */
    private Post createTestPost(User user) {
        return Post.builder()
                .title("테스트 게시글")
                .content("테스트 게시글 내용입니다.")
                .user(user)
                .build();
    }
    
    /**
     * 테스트용 댓글 생성
     */
    private Comment createTestComment(User user, Post post) {
        return Comment.builder()
                .content("테스트 댓글입니다.")
                .user(user)
                .post(post)
                .deleted(false)
                .build();
    }
    
    /**
     * 테스트용 CustomUserDetails 생성
     */
    private CustomUserDetails createUserDetails(User user) {
        UserDTO userDTO = UserDTO.builder()
                .userId(user.getId())
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