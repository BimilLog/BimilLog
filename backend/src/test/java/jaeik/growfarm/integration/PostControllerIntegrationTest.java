//package jaeik.growfarm.integration;
//
//import jaeik.growfarm.controller.PostController;
//import jaeik.growfarm.dto.admin.ReportDTO;
//import jaeik.growfarm.dto.board.PostDTO;
//import jaeik.growfarm.dto.board.PostReqDTO;
//import jaeik.growfarm.dto.board.SimplePostDTO;
//import jaeik.growfarm.entity.board.Post;
//import jaeik.growfarm.entity.user.Setting;
//import jaeik.growfarm.entity.user.Token;
//import jaeik.growfarm.entity.user.UserRole;
//import jaeik.growfarm.entity.user.Users;
//import jaeik.growfarm.global.auth.CustomUserDetails;
//import jaeik.growfarm.repository.post.PostRepository;
//import jaeik.growfarm.repository.user.SettingRepository;
//import jaeik.growfarm.repository.user.TokenRepository;
//import jaeik.growfarm.repository.user.UserRepository;
//import jaeik.growfarm.service.PostService;
//import jaeik.growfarm.util.UserUtil;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.*;
//import org.mockito.Mock;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.domain.Page;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.test.annotation.Commit;
//import org.springframework.test.context.TestConstructor;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
//import static org.mockito.Mockito.mock;
//
///**
// * <h2>PostController 통합 테스트</h2>
// * <p>실제 데이터베이스와 서비스를 사용하여 PostController의 전체 API를 테스트합니다.</p>
// * @since 2025.05.21
// */
//@SpringBootTest
//@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@TestInstance(PER_CLASS)
//@Commit
//@Transactional
//public class PostControllerIntegrationTest {
//
//    private final PostController postController;
//    private final PostService postService;
//    private final PostRepository postRepository;
//    private final SettingRepository settingRepository;
//    private final TokenRepository tokenRepository;
//    private final UserRepository userRepository;
//    private final UserUtil userUtil;
//
//    @Mock
//    private HttpServletRequest request;
//
//    @Mock
//    private HttpServletResponse httpResponse;
//
//    private Users testUser;
//    private Post testPost;
//
//    public PostControllerIntegrationTest(PostController postController,
//                                         PostService postService,
//                                         PostRepository postRepository,
//                                         SettingRepository settingRepository,
//                                         TokenRepository tokenRepository,
//                                         UserRepository userRepository,
//                                         UserUtil userUtil) {
//        this.postController = postController;
//        this.postService = postService;
//        this.postRepository = postRepository;
//        this.settingRepository = settingRepository;
//        this.tokenRepository = tokenRepository;
//        this.userRepository = userRepository;
//        this.userUtil = userUtil;
//
//        this.request = mock(HttpServletRequest.class);
//        this.httpResponse = mock(HttpServletResponse.class);
//    }
//
//    /**
//     * <h3>테스트 데이터 초기화</h3>
//     * 사용자, 게시글 데이터 생성
//     *
//     * @since 2025.05.21
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
//    }
//
//    /**
//     * <h3>게시판 조회 통합 테스트</h3>
//     * @since 2025.05.21
//     */
//    @Test
//    @DisplayName("게시판 조회 통합 테스트")
//    void testGetBoard() {
//        // When
//        ResponseEntity<Page<SimplePostDTO>> response = postController.getBoard(0, 10);
//
//        // Then
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().getTotalElements());
//        assertEquals(testPost.getTitle(), response.getBody().getContent().getFirst().getTitle());
//        assertEquals(testPost.getUser().getFarmName(), response.getBody().getContent().getFirst().getFarmName());
//    }
//
//
//    /**
//     * <h3>실시간 인기글 목록 조회 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("실시간 인기글 목록 조회 통합 테스트")
//    void testGetRealtimeBoard() {
//        // When
//        ResponseEntity<List<SimplePostDTO>> response = postController.getRealtimeBoard();
//
//        // Then
//        assertNotNull(response.getBody());
//    }
//
//    /**
//     * <h3>주간 인기글 목록 조회 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("주간 인기글 목록 조회 통합 테스트")
//    void testGetWeeklyBoard() {
//        // When
//        ResponseEntity<List<SimplePostDTO>> response = postController.getWeeklyBoard();
//
//        // Then
//        assertNotNull(response.getBody());
//    }
//
//    /**
//     * <h3>명예의 전당 목록 조회 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("명예의 전당 목록 조회 통합 테스트")
//    void testGetHallOfFameBoard() {
//        // When
//        ResponseEntity<List<SimplePostDTO>> response = postController.getHallOfFameBoard();
//
//        // Then
//        assertNotNull(response.getBody());
//    }
//
//    /**
//     * <h3>게시글 검색 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("게시글 검색 통합 테스트")
//    void testSearchPost() {
//        // When
//        ResponseEntity<Page<SimplePostDTO>> response = postController.searchPost("제목", "Test", 0, 10);
//
//        // Then
//        assertNotNull(response.getBody());
//    }
//
//    /**
//     * <h3>게시글 조회 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("게시글 조회 통합 테스트")
//    void testGetPost() {
//        // When
//        ResponseEntity<PostDTO> response = postController.getPost(testPost.getId(), testUser.getId(), request, response);
//
//        // Then
//        assertNotNull(response.getBody());
//        assertEquals(testPost.getTitle(), response.getBody().getTitle());
//        assertEquals(testPost.getContent(), response.getBody().getContent());
//    }
//
//    /**
//     * <h3>게시글 작성 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("게시글 작성 통합 테스트")
//    void testWritePost() {
//        // Given
//        PostReqDTO postReqDTO = new PostReqDTO();
//        postReqDTO.setTitle("New Test Post");
//        postReqDTO.setContent("New Test Content");
//
//        // 인증 설정
//        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
//        );
//
//        // When
//        ResponseEntity<PostDTO> response = postController.writePost(userDetails, postReqDTO);
//
//        // Then
//        assertNotNull(response.getBody());
//        assertEquals("New Test Post", response.getBody().getTitle());
//        assertEquals("New Test Content", response.getBody().getContent());
//    }
//
//    /**
//     * <h3>게시글 수정 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("게시글 수정 통합 테스트")
//    void testUpdatePost() {
//        // Given
//        PostDTO postDTO = new PostDTO();
//        postDTO.setTitle("Updated Test Post");
//        postDTO.setContent("Updated Test Content");
//
//        // 인증 설정
//        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
//        );
//
//        // When
//        ResponseEntity<PostDTO> response = postController.updatePost(testPost.getId(), testUser.getId(), userDetails, postDTO);
//
//        // Then
//        assertNotNull(response.getBody());
//        assertEquals("Updated Test Post", response.getBody().getTitle());
//        assertEquals("Updated Test Content", response.getBody().getContent());
//    }
//
//    /**
//     * <h3>게시글 삭제 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("게시글 삭제 통합 테스트")
//    void testDeletePost() {
//        // 인증 설정
//        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
//        );
//
//        // When
//        ResponseEntity<String> response = postController.deletePost(testPost.getId(), userDetails);
//
//        // Then
//        assertEquals("게시글 삭제 완료", response.getBody());
//    }
//
//    /**
//     * <h3>게시글 추천 통합 테스트</h3>
//     * @since 2025.05.17
//     */
////    @Test
////    @DisplayName("게시글 추천 통합 테스트")
////    void testLikePost() {
////        // 인증 설정
////        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
////        SecurityContextHolder.getContext().setAuthentication(
////                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
////        );
////
////        // When
////        ResponseEntity<String> response = postController.likePost(testPost.getId(), userDetails);
////
////        // Then
////        assertEquals("게시글 추천 완료", response.getBody());
////    }
//
//    /**
//     * <h3>게시글 신고 통합 테스트</h3>
//     * @since 2025.05.17
//     */
//    @Test
//    @DisplayName("게시글 신고 통합 테스트")
//    void testReportPost() {
//        // Given
//        ReportDTO reportDTO = ReportDTO.builder()
//                .targetId(testPost.getId())
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
//        ResponseEntity<String> response = postController.reportPost(testPost.getId(), userDetails, reportDTO);
//
//        // Then
//        assertEquals("게시글 신고 완료", response.getBody());
//    }
//}
