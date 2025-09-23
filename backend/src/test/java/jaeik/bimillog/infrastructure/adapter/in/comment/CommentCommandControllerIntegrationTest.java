package jaeik.bimillog.infrastructure.adapter.in.comment;

import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.comment.dto.CommentLikeReqDTO;
import jaeik.bimillog.infrastructure.adapter.in.comment.dto.CommentReqDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentClosureRepository;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.CommentTestDataBuilder;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>댓글 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Comment Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>댓글 작성, 수정, 삭제, 추천 API 동작을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Import(TestSocialLoginPortConfig.class)
@DisplayName("댓글 Command 컨트롤러 통합 테스트")
class CommentCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentCommandUseCase commentCommandUseCase;

    @Autowired
    private CommentClosureRepository commentClosureRepository;

    private Post testPost;

    @Override
    protected void setUpChild() {
        // testUser는 BaseIntegrationTest에서 이미 생성됨
        testPost = TestFixtures.createPostWithUser(testUser);
        postRepository.save(testPost);
    }
    
    @AfterEach
    void tearDown() {
        // 수동 데이터 정리 (트랜잭션 충돌 방지)
        try {
            commentRepository.findAll().forEach(comment -> {
                try {
                    Long commentId = comment.getId();
                    // 더티 체킹으로 하드 삭제만 처리
                    commentRepository.deleteClosuresByDescendantId(commentId);
                    commentRepository.hardDeleteComment(commentId);
                } catch (Exception ignored) {}
            });
            postRepository.deleteAll();
            userRepository.deleteAll();
        } catch (Exception e) {
            System.err.println("tearDown failed: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("댓글 작성 통합 테스트 - 로그인 사용자")
    void writeComment_LoggedInUser_IntegrationTest() throws Exception {
        // Given
        CommentReqDTO requestDto = CommentTestDataBuilder.createCommentReqDTO(
                testPost.getId(), "통합 테스트용 댓글입니다.");

        // When & Then
        performPost("/api/comment/write", requestDto, testUserDetails)
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
        // Given - 부모 댓글 생성 (비즈니스 로직 사용하여 클로저 테이블 보장)
        commentCommandUseCase.writeComment(testUser.getId(), testPost.getId(), null, "부모 댓글입니다.", null);
        
        // 생성된 부모 댓글 조회
        Comment parentComment = commentRepository.findAll()
                .stream()
                .filter(c -> "부모 댓글입니다.".equals(c.getContent()))
                .findFirst()
                .orElseThrow();
        
        CommentReqDTO requestDto = CommentTestDataBuilder.createReplyCommentReqDTO(
                testPost.getId(), parentComment.getId(), "대댓글 테스트입니다.");
        
        CustomUserDetails userDetails = CommentTestDataBuilder.createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(post("/api/comment/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(user(userDetails))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("댓글 작성 완료"));
        
        // 데이터베이스 검증 - 대댓글이 저장되었는지만 확인
        Optional<Comment> savedReply = commentRepository.findAll()
                .stream()
                .filter(comment -> "대댓글 테스트입니다.".equals(comment.getContent()))
                .findFirst();

        assertThat(savedReply).isPresent();
        // 클로저 테이블 검증은 Repository 테스트의 책임으로 삭제
    }
    
    @Test
    @DisplayName("댓글 수정 통합 테스트")
    void updateComment_IntegrationTest() throws Exception {
        // Given
        Comment existingComment = CommentTestDataBuilder.createTestComment(
                testUser, testPost, "원본 댓글 내용입니다.");
        commentRepository.save(existingComment);
        
        CommentReqDTO requestDto = CommentTestDataBuilder.createUpdateCommentReqDTO(
                existingComment.getId(), "수정된 댓글 내용입니다.");
        
        CustomUserDetails userDetails = CommentTestDataBuilder.createUserDetails(testUser);
        
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
        // Given - 비즈니스 로직으로 댓글 생성 (클로저 포함)
        commentCommandUseCase.writeComment(testUser.getId(), testPost.getId(), null, "테스트 댓글입니다.", null);
        
        // 생성된 댓글 조회
        Comment existingComment = commentRepository.findAll()
                .stream()
                .filter(c -> "테스트 댓글입니다.".equals(c.getContent()))
                .findFirst()
                .orElseThrow();
        
        CommentReqDTO requestDto = CommentTestDataBuilder.createDeleteCommentReqDTO(existingComment.getId());
        
        CustomUserDetails userDetails = CommentTestDataBuilder.createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(post("/api/comment/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(user(userDetails))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("댓글 삭제 완료"));
        
        // 데이터베이스 검증 - 자손이 없는 댓글은 하드 삭제됨
        Optional<Comment> deletedComment = commentRepository.findById(existingComment.getId());
        assertThat(deletedComment).isEmpty(); // 하드 삭제로 완전히 제거됨
    }
    
    @Test
    @DisplayName("댓글 추천 통합 테스트")
    void likeComment_IntegrationTest() throws Exception {
        // Given
        Comment existingComment = CommentTestDataBuilder.createTestComment(
                testUser, testPost, "추천할 댓글입니다.");
        commentRepository.save(existingComment);
        
        // 추천 API용 DTO 생성 (commentId만 필요)
        CommentLikeReqDTO requestDto = new CommentLikeReqDTO();
        requestDto.setCommentId(existingComment.getId());
        
        CustomUserDetails userDetails = CommentTestDataBuilder.createUserDetails(testUser);
        
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
        CommentReqDTO requestDto = CommentTestDataBuilder.createCommentReqDTO(
                testPost.getId(), "A".repeat(1001)); // 1000자 초과
        
        CustomUserDetails userDetails = CommentTestDataBuilder.createUserDetails(testUser);
        
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
        // Given: 비즈니스 로직으로 익명 댓글 생성 (클로저 포함)
        commentCommandUseCase.writeComment(null, testPost.getId(), null, "익명 댓글입니다", 1234);
        
        // 생성된 익명 댓글 조회
        Comment anonymousComment = commentRepository.findAll()
                .stream()
                .filter(c -> "익명 댓글입니다".equals(c.getContent()))
                .findFirst()
                .orElseThrow();
        
        CommentReqDTO requestDto = CommentTestDataBuilder.createAnonymousDeleteCommentReqDTO(
                anonymousComment.getId(), 1234);
        
        // When & Then
        mockMvc.perform(post("/api/comment/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("댓글 삭제 완료"));
        
        // 데이터베이스 검증 - 자손이 없는 익명 댓글도 하드 삭제됨
        Optional<Comment> deletedComment = commentRepository.findById(anonymousComment.getId());
        assertThat(deletedComment).isEmpty(); // 하드 삭제로 완전히 제거됨
    }

    @Test
    @DisplayName("익명 댓글 삭제 실패 - 잘못된 패스워드")
    void deleteAnonymousComment_WrongPassword_IntegrationTest() throws Exception {
        // Given: 비즈니스 로직으로 익명 댓글 생성
        commentCommandUseCase.writeComment(null, testPost.getId(), null, "익명 댓글입니다", 1234);
        
        // 생성된 익명 댓글 조회
        Comment anonymousComment = commentRepository.findAll()
                .stream()
                .filter(c -> "익명 댓글입니다".equals(c.getContent()))
                .findFirst()
                .orElseThrow();
        
        CommentReqDTO requestDto = CommentTestDataBuilder.createAnonymousDeleteCommentReqDTO(
                anonymousComment.getId(), 9999); // 잘못된 패스워드
        
        // When & Then
        mockMvc.perform(post("/api/comment/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
        
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
        User anotherUser = TestUsers.createUniqueWithPrefix("another");
        userRepository.save(anotherUser);
        
        Comment otherUserComment = CommentTestDataBuilder.createTestComment(
                anotherUser, testPost, "다른 사용자의 댓글");
        commentRepository.save(otherUserComment);
        
        CommentReqDTO requestDto = CommentTestDataBuilder.createDeleteCommentReqDTO(otherUserComment.getId());
        
        CustomUserDetails userDetails = CommentTestDataBuilder.createUserDetails(testUser); // 현재 사용자
        
        // When & Then
        mockMvc.perform(post("/api/comment/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(user(userDetails))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
        
        // 데이터베이스 검증 - 댓글이 삭제되지 않아야 함
        Optional<Comment> comment = commentRepository.findById(otherUserComment.getId());
        assertThat(comment).isPresent();
        assertThat(comment.get().isDeleted()).isFalse();
    }
}