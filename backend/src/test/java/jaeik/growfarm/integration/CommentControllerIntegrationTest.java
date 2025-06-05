//package jaeik.growfarm.integration;
//
//import jaeik.growfarm.controller.CommentController;
//import jaeik.growfarm.dto.admin.ReportDTO;
//import jaeik.growfarm.dto.board.CommentDTO;
//import jaeik.growfarm.dto.board.PostDTO;
//import jaeik.growfarm.dto.board.PostReqDTO;
//import jaeik.growfarm.entity.board.Comment;
//import jaeik.growfarm.entity.board.Post;
//import jaeik.growfarm.entity.user.Setting;
//import jaeik.growfarm.entity.user.Token;
//import jaeik.growfarm.entity.user.UserRole;
//import jaeik.growfarm.entity.user.Users;
//import jaeik.growfarm.global.auth.CustomUserDetails;
//import jaeik.growfarm.repository.comment.CommentRepository;
//import jaeik.growfarm.repository.post.PostRepository;
//import jaeik.growfarm.repository.user.SettingRepository;
//import jaeik.growfarm.repository.user.TokenRepository;
//import jaeik.growfarm.repository.user.UserRepository;
//import jaeik.growfarm.service.PostService;
//import jaeik.growfarm.util.UserUtil;
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.*;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.test.annotation.Commit;
//import org.springframework.test.context.TestConstructor;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
//
///**
// * <h2>CommentController 통합 테스트</h2>
// * <p>실제 데이터베이스와 서비스를 사용하여 CommentController의 전체 API를 테스트합니다.</p>
// * @since 2025.05.17
// */
//@SpringBootTest
//@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@TestInstance(PER_CLASS)
//@Commit
//@Transactional
//public class CommentControllerIntegrationTest {
//
//    private final CommentController commentController;
//    private final CommentRepository commentRepository;
//    private final PostRepository postRepository;
//    private final SettingRepository settingRepository;
//    private final TokenRepository tokenRepository;
//    private final UserRepository userRepository;
//    private final PostService postService;
//    private final UserUtil userUtil;
//
//    private Users testUser;
//    private Post testPost;
//    private Comment testComment;
//
//    public CommentControllerIntegrationTest(CommentController commentController,
//                                           CommentRepository commentRepository,
//                                           PostRepository postRepository,
//                                           SettingRepository settingRepository,
//                                           TokenRepository tokenRepository,
//                                           UserRepository userRepository,
//                                           PostService postService,
//                                           UserUtil userUtil) {
//        this.commentController = commentController;
//        this.commentRepository = commentRepository;
//        this.postRepository = postRepository;
//        this.settingRepository = settingRepository;
//        this.tokenRepository = tokenRepository;
//        this.userRepository = userRepository;
//        this.postService = postService;
//        this.userUtil = userUtil;
//    }
//
//    /**
//     * <h3>테스트 데이터 초기화</h3>
//     * 사용자, 게시글, 댓글 데이터 생성
//     *
//     * @since 2025.05.17
//     */
//    @BeforeAll
//    void setUp() {
//        // 사용자 설정 생성
//        Setting setting = Setting.builder()
//                .isFarmNotification(true)
//                .isCommentNotification(true)
//                .isPostFeaturedNotification(true)
//                .isCommentFeaturedNotification(true)
//                .build();
//        settingRepository.save(setting);
//
//        // 토큰 생성
//        Token token = Token.builder()
//                .jwtRefreshToken("testRefreshToken")
//                .kakaoAccessToken("testKakaoAccessToken")
//                .kakaoRefreshToken("testKakaoRefreshToken")
//                .build();
//        tokenRepository.save(token);
//
//        // 사용자 생성
//        Users user = Users.builder()
//                .kakaoId(1234567890L)
//                .kakaoNickname("testNickname")
//                .thumbnailImage("testImage")
//                .farmName("testFarm")
//                .role(UserRole.USER)
//                .setting(setting)
//                .token(token)
//                .build();
//        testUser = userRepository.save(user);
//
//        // 게시글 생성
//        Post post = Post.builder()
//                .title("Test Post")
//                .content("Test Content")
//                .user(testUser)
//                .views(0)
//                .isNotice(false)
//                .isRealtimePopular(false)
//                .isWeeklyPopular(false)
//                .isHallOfFame(false)
//                .build();
//        testPost = postRepository.save(post);
//
//        // 댓글 생성
//        Comment comment = Comment.builder()
//                .content("Test Comment")
//                .user(testUser)
//                .post(testPost)
//                .isFeatured(false)
//                .build();
//        testComment = commentRepository.save(comment);
//    }
//
//    /**
//     * <h3>댓글 작성 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("댓글 작성 통합 테스트")
//    void testWriteComment() throws IOException {
//        // Given
//        CommentDTO commentDTO = new CommentDTO(null, testPost.getId(), testUser.getId(),
//                testUser.getFarmName(), "New Test Comment", 0, LocalDateTime.now(), false, false);
//
//        // 인증 설정
//        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
//        );
//
//        // When
//        ResponseEntity<String> response = commentController.writeComment(userDetails, testPost.getId(), commentDTO);
//
//        // Then
//        assertEquals("댓글 작성 완료", response.getBody());
//    }
//
//    /**
//     * <h3>댓글 수정 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("댓글 수정 통합 테스트")
//    void testUpdateComment() {
//        // Given
//        CommentDTO commentDTO = new CommentDTO(testComment.getId(), testPost.getId(), testUser.getId(),
//                testUser.getFarmName(), "Updated Test Comment", 0, LocalDateTime.now(), false, false);
//
//        // 인증 설정
//        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
//        );
//
//        // When
//        ResponseEntity<String> response = commentController.updateComment(userDetails, testPost.getId(), testComment.getId(), commentDTO);
//
//        // Then
//        assertEquals("댓글 수정 완료", response.getBody());
//    }
//
//    /**
//     * <h3>댓글 삭제 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("댓글 삭제 통합 테스트")
//    void testDeleteComment() {
//        // 인증 설정
//        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
//        );
//
//        // When
//        ResponseEntity<String> response = commentController.deleteComment(userDetails, testPost.getId(), testComment.getId());
//
//        // Then
//        assertEquals("댓글 삭제 완료", response.getBody());
//    }
//
//    /**
//     * <h3>댓글 추천 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("댓글 추천 통합 테스트")
//    void testLikeComment() {
//        // 인증 설정
//        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
//        );
//
//        // When
//        ResponseEntity<String> response = commentController.likeComment(testPost.getId(), testComment.getId(), userDetails);
//
//        // Then
//        assertEquals("댓글 추천 완료", response.getBody());
//    }
//
//    /**
//     * <h3>댓글 신고 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("댓글 신고 통합 테스트")
//    void testReportComment() {
//        // Given
//        ReportDTO reportDTO = ReportDTO.builder()
//                .targetId(testComment.getId())
//                .content("Test Report Reason")
//                .build();
//
//        // 인증 설정
//        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
//        );
//
//        // When
//        ResponseEntity<String> response = commentController.reportComment(userDetails, testPost.getId(), reportDTO);
//
//        // Then
//        assertEquals("댓글 신고 완료", response.getBody());
//    }
//}
