package jaeik.growfarm.integration.api.comment;

import jaeik.growfarm.controller.CommentController;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestConstructor;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>CommentController 통합 테스트</h2>
 * <p>
 * 실제 데이터베이스와 서비스를 사용하여 CommentController의 전체 API를 테스트합니다.
 * </p>
 * <p>
 * 회원/비회원별 댓글 CRUD 및 추천 기능을 검증합니다.
 * </p>
 * 
 * @version 1.0.0
 * @author Jaeik
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Transactional
public class CommentControllerIntegrationTest {

    private final CommentController commentController;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final SettingRepository settingRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    private Post testPost;
    private Comment testComment;
    private CustomUserDetails userDetails;
    private final Random random = new Random();

    public CommentControllerIntegrationTest(CommentController commentController,
            CommentRepository commentRepository,
            PostRepository postRepository,
            SettingRepository settingRepository,
            TokenRepository tokenRepository,
            UserRepository userRepository) {
        this.commentController = commentController;
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.settingRepository = settingRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @BeforeAll
    void setUp() {
        // 고유한 값 생성을 위한 랜덤 값
        int uniqueId = random.nextInt(1000000);
        long timestamp = System.currentTimeMillis();

        // 사용자 설정 생성
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        settingRepository.save(setting);

        // 사용자 생성 (고유한 값 사용)
        Users user = Users.builder()
                .kakaoId(timestamp + uniqueId)
                .kakaoNickname("testNickname" + uniqueId)
                .thumbnailImage("testImage")
                .userName("testUser" + uniqueId)
                .role(UserRole.USER)
                .setting(setting)
                .build();
        Users testUser = userRepository.save(user);

        // 토큰 생성
        Token token = Token.builder()
                .users(testUser)
                .jwtRefreshToken("testRefreshToken" + uniqueId)
                .kakaoAccessToken("testKakaoAccessToken" + uniqueId)
                .kakaoRefreshToken("testKakaoRefreshToken" + uniqueId)
                .build();
        tokenRepository.save(token);

        // 게시글 생성
        Post post = Post.builder()
                .title("Test Post " + uniqueId)
                .content("Test Content " + uniqueId)
                .user(testUser)
                .views(0)
                .isNotice(false)
                .build();
        testPost = postRepository.save(post);

        // 댓글 생성
        Comment comment = Comment.builder()
                .content("Test Comment " + uniqueId)
                .user(testUser)
                .post(testPost)
                .build();
        testComment = commentRepository.save(comment);

        // ClientDTO 생성
        ClientDTO clientDTO = new ClientDTO(testUser, token.getId(), null);
        userDetails = new CustomUserDetails(clientDTO);
    }

    @Test
    @Order(1)
    @DisplayName("댓글 조회 통합 테스트")
    void testGetComments() {
        // When
        ResponseEntity<Page<CommentDTO>> response = commentController.getComments(userDetails, testPost.getId(), 0);

        // Then
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    @Order(2)
    @DisplayName("인기댓글 조회 통합 테스트")
    void testGetPopularComments() {
        // When
        ResponseEntity<List<CommentDTO>> response = commentController.getPopularComments(userDetails, testPost.getId());

        // Then
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    @Order(3)
    @DisplayName("댓글 작성 통합 테스트 - 회원")
    void testWriteCommentByUser() {
        // Given
        int uniqueId = random.nextInt(1000000);
        CommentDTO commentDTO = new CommentDTO(testComment);
        commentDTO.setContent("New Test Comment " + uniqueId);

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<String> response = commentController.writeComment(commentDTO, userDetails);

        // Then
        assertEquals("댓글 작성 완료", response.getBody());
    }

    @Test
    @Order(4)
    @DisplayName("댓글 작성 통합 테스트 - 비회원")
    void testWriteCommentByGuest() {
        // Given
        int uniqueId = random.nextInt(1000000);
        CommentDTO commentDTO = new CommentDTO(testComment);
        commentDTO.setContent("Guest Comment " + uniqueId);
        commentDTO.setUserName("비회원" + uniqueId);
        commentDTO.setPassword(1234);

        // When
        ResponseEntity<String> response = commentController.writeComment(commentDTO, null);

        // Then
        assertEquals("댓글 작성 완료", response.getBody());
    }

    @Test
    @Order(5)
    @DisplayName("댓글 수정 통합 테스트 - 회원")
    void testUpdateCommentByUser() {
        // Given
        int uniqueId = random.nextInt(1000000);
        CommentDTO commentDTO = new CommentDTO(testComment);
        commentDTO.setContent("Updated Test Comment " + uniqueId);

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<String> response = commentController.updateComment(userDetails, commentDTO);

        // Then
        assertEquals("댓글 수정 완료", response.getBody());
    }

    @Test
    @Order(6)
    @DisplayName("댓글 수정 통합 테스트 - 비회원")
    void testUpdateCommentByGuest() {
        // Given
        int uniqueId = random.nextInt(1000000);
        Comment guestComment = Comment.builder()
                .content("Guest Comment " + uniqueId)
                .user(null)
                .post(testPost)
                .password(1234)
                .build();
        guestComment = commentRepository.save(guestComment);

        CommentDTO commentDTO = new CommentDTO(guestComment);
        commentDTO.setContent("Updated Guest Comment " + uniqueId);
        commentDTO.setPassword(1234);

        // When
        ResponseEntity<String> response = commentController.updateComment(null, commentDTO);

        // Then
        assertEquals("댓글 수정 완료", response.getBody());
    }

    @Test
    @Order(7)
    @DisplayName("댓글 삭제 통합 테스트 - 회원")
    void testDeleteCommentByUser() {
        // Given
        CommentDTO commentDTO = new CommentDTO(testComment);

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<String> response = commentController.deleteComment(userDetails, commentDTO);

        // Then
        assertEquals("댓글 삭제 완료", response.getBody());
    }

    @Test
    @Order(8)
    @DisplayName("댓글 삭제 통합 테스트 - 비회원")
    void testDeleteCommentByGuest() {
        // Given
        int uniqueId = random.nextInt(1000000);
        Comment guestComment = Comment.builder()
                .content("Guest Comment to Delete " + uniqueId)
                .user(null)
                .post(testPost)
                .password(1234)
                .build();
        guestComment = commentRepository.save(guestComment);

        CommentDTO commentDTO = new CommentDTO(guestComment);
        commentDTO.setPassword(1234);

        // When
        ResponseEntity<String> response = commentController.deleteComment(null, commentDTO);

        // Then
        assertEquals("댓글 삭제 완료", response.getBody());
    }

    @Test
    @Order(9)
    @DisplayName("댓글 추천 통합 테스트")
    void testLikeComment() {
        // Given
        CommentDTO commentDTO = new CommentDTO(testComment);

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<String> response = commentController.likeComment(commentDTO, userDetails);

        // Then
        assertEquals("추천 처리 완료", response.getBody());
    }
}