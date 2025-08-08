//package jaeik.growfarm.integration;
//
//import jaeik.growfarm.controller.PostController;
//import jaeik.growfarm.dto.post.PostDTO;
//import jaeik.growfarm.dto.post.PostReqDTO;
//import jaeik.growfarm.dto.post.SimplePostDTO;
//import jaeik.growfarm.dto.user.ClientDTO;
//import jaeik.growfarm.entity.post.Post;
//import jaeik.growfarm.entity.post.PostLike;
//import jaeik.growfarm.entity.user.Setting;
//import jaeik.growfarm.entity.user.Token;
//import jaeik.growfarm.entity.user.UserRole;
//import jaeik.growfarm.entity.user.Users;
//import jaeik.growfarm.global.auth.CustomUserDetails;
//import jaeik.growfarm.repository.post.PostLikeRepository;
//import jaeik.growfarm.repository.post.PostRepository;
//import jaeik.growfarm.repository.token.TokenRepository;
//import jaeik.growfarm.repository.user.SettingRepository;
//import jaeik.growfarm.repository.user.UserRepository;
//import jaeik.growfarm.service.post.PostService;
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
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.TestConstructor;
//
//import java.util.List;
//import java.util.Random;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
//import static org.mockito.Mockito.mock;
//
///**
// * <h2>PostController 통합 테스트</h2>
// * <p>
// * 실제 데이터베이스와 서비스를 사용하여 PostController의 전체 API를 테스트합니다.
// * </p>
// * <p>
// * 회원/비회원별 게시글 CRUD, 추천, 검색 기능을 검증합니다.
// * </p>
// *
// * @author Jaeik
// * @version 1.0.0
// */
//@SpringBootTest
//@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@TestInstance(PER_CLASS)
//@Transactional
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
//public class PostControllerIntegrationTest {
//
//    private final PostController postController;
//    private final PostRepository postRepository;
//    private final SettingRepository settingRepository;
//    private final TokenRepository tokenRepository;
//    private final UserRepository userRepository;
//    private final PostLikeRepository postLikeRepository;
//    private final PostService postService;
//
//    @Mock
//    private HttpServletRequest request;
//
//    @Mock
//    private HttpServletResponse httpResponse;
//
//    private Post testPost;
//    private Users testUser;
//    private CustomUserDetails userDetails;
//    private final Random random = new Random();
//    private Long memberPostId;
//    private Long guestPostId;
//
//    // 고유한 식별자 생성
//    private final String uniqueId = String.valueOf(System.currentTimeMillis());
//
//    public PostControllerIntegrationTest(PostController postController,
//            PostRepository postRepository,
//            SettingRepository settingRepository,
//            TokenRepository tokenRepository,
//            UserRepository userRepository,
//            PostLikeRepository postLikeRepository,
//            PostService postService) {
//        this.postController = postController;
//        this.postRepository = postRepository;
//        this.settingRepository = settingRepository;
//        this.tokenRepository = tokenRepository;
//        this.userRepository = userRepository;
//        this.postLikeRepository = postLikeRepository;
//        this.postService = postService;
//    }
//
//    @BeforeAll
//    @Transactional
//    void setUp() {
//        this.request = mock(HttpServletRequest.class);
//        this.httpResponse = mock(HttpServletResponse.class);
//
//        // 고유한 값 생성을 위한 랜덤 값
//        int randomId = random.nextInt(1000000);
//        long timestamp = System.currentTimeMillis();
//
//        // 사용자 21명 생성 (고유한 값 사용)
//        for (int i = 0; i < 21; i++) {
//            // 1. 개별 설정 먼저 저장
//            Setting setting = Setting.builder()
//                    .messageNotification(true)
//                    .commentNotification(true)
//                    .postFeaturedNotification(true)
//                    .build();
//            settingRepository.save(setting);
//
//            // 2. 해당 설정을 사용자에 연결 (고유한 사용자명 생성)
//            Users user = Users.builder()
//                    .kakaoId(timestamp + randomId + i)
//                    .kakaoNickname("testNickname" + uniqueId + i)
//                    .thumbnailImage("testImage" + uniqueId + i)
//                    .userName("testUser" + uniqueId + i) // 고유한 사용자명
//                    .role(UserRole.USER)
//                    .setting(setting)
//                    .build();
//
//            userRepository.save(user);
//        }
//
//        // 첫 번째 생성된 사용자를 테스트 유저로 설정
//        List<Users> allUsers = userRepository.findAll();
//        this.testUser = allUsers.stream()
//                .filter(u -> u.getUserName().startsWith("testUser" + uniqueId))
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Test user not found"));
//
//        // 토큰 생성
//        Token token = Token.builder()
//                .users(testUser)
//                .jwtRefreshToken("testRefreshToken" + uniqueId)
//                .kakaoAccessToken("testKakaoAccessToken" + uniqueId)
//                .kakaoRefreshToken("testKakaoRefreshToken" + uniqueId)
//                .build();
//        tokenRepository.save(token);
//
//        // 게시글 생성
//        Post post = Post.builder()
//                .title("Test Post " + uniqueId)
//                .content("Test Content " + uniqueId)
//                .user(testUser)
//                .views(0)
//                .isNotice(false)
//                .build();
//        testPost = postRepository.save(post);
//
//        // 게시글 생성 시간을 5시간 전으로 설정 (인기글 조건 확실히 만족)
//        try {
//            java.lang.reflect.Field createdAtField = testPost.getClass().getSuperclass().getDeclaredField("createdAt");
//            createdAtField.setAccessible(true);
//            java.time.Instant fiveHoursAgo = java.time.Instant.now().minus(5, java.time.temporal.ChronoUnit.HOURS);
//            createdAtField.set(testPost, fiveHoursAgo);
//            testPost = postRepository.save(testPost); // 변경된 시간으로 다시 저장
//            postRepository.flush(); // 즉시 DB에 반영
//        } catch (Exception e) {
//            System.out.println("createdAt 필드 설정 실패, 테스트 계속 진행: " + e.getMessage());
//        }
//
//        // 게시글 추천 20개 이상으로 설정 (레전드 게시글 기준)
//        List<Users> users = userRepository.findAll().stream()
//                .filter(u -> u.getUserName().startsWith("testUser" + uniqueId))
//                .toList();
//
//        for (Users user : users) {
//            PostLike postLike = PostLike.builder().user(user).post(testPost).build();
//            postLikeRepository.save(postLike);
//        }
//
//        // 데이터베이스에 강제로 반영
//        postLikeRepository.flush();
//
//        postService.updateRealtimePopularPosts();
//        postService.updateWeeklyPopularPosts();
//        postService.updateLegendPopularPosts();
//
//        // 간단한 ClientDTO 생성 (LazyInitializationException 방지를 위해 직접 생성)
//        ClientDTO clientDTO = new ClientDTO(
//                testUser.getId(),
//                testUser.getKakaoId(),
//                testUser.getKakaoNickname(),
//                testUser.getThumbnailImage(),
//                testUser.getUserName(),
//                testUser.getRole(),
//                token.getId(),
//                null,
//                null // SettingDTO를 null로 설정하여 지연 로딩 문제 회피
//        );
//
//        userDetails = new CustomUserDetails(clientDTO);
//    }
//
//    @AfterAll
//    void tearDown() {
//        // 별도 정리 로직 없음
//    }
//
//    @Test
//    @Order(1)
//    @DisplayName("게시판 조회 통합 테스트")
//    void testGetBoard() {
//        // When
//        ResponseEntity<Page<SimplePostDTO>> response = postController.getBoard(0, 10);
//
//        // Then
//        assertNotNull(response.getBody());
//        // 기존 데이터가 있을 수 있으므로 최소 1개 이상인지만 확인
//        assertTrue(response.getBody().getTotalElements() >= 1);
//    }
//
//    @Test
//    @Order(2)
//    @DisplayName("게시글 조회 통합 테스트")
//    void testGetPost() {
//        // When
////        ResponseEntity<PostDTO> response = postController.getPost(testPost.getId(), userDetails, request, httpResponse);
//
//        // Then
//        assertNotNull(response.getBody());
//        assertEquals(testPost.getTitle(), response.getBody().getTitle());
//        assertEquals(testPost.getContent(), response.getBody().getContent());
//    }
//
//    @Test
//    @Order(3)
//    @DisplayName("게시글 검색 통합 테스트 - 제목 검색")
//    @Disabled("풀텍스트 인덱스 필요로 임시로 비활성화 합니다.")
//    void testSearchPost() {
//        // Given
//        String searchType = "TITLE"; // 제목 검색
//        String searchQuery = "Test Post"; // 검색어
//
//        // When
//        ResponseEntity<Page<SimplePostDTO>> response = postController.searchPost(searchType, searchQuery, 0, 10);
//
//        // Then
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().getTotalElements() > 0); // 검색 결과가 있어야 함
//    }
//
//    @Test
//    @Order(4)
//    @DisplayName("게시글 검색 통합 테스트 - 제목/내용 검색")
//    @Disabled("풀텍스트 인덱스 필요로 임시로 비활성화 합니다.")
//    void testSearchPostByContent() {
//        // Given
//        String searchType = "TITLE_CONTENT"; // 내용 검색
//        String searchQuery = "Test Content"; // 검색어
//
//        // When
//        ResponseEntity<Page<SimplePostDTO>> response = postController.searchPost(searchType, searchQuery, 0, 10);
//
//        // Then
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().getTotalElements() > 0); // 검색 결과가 있어야 함
//    }
//
//    @Test
//    @Order(5)
//    @DisplayName("게시글 검색 통합 테스트 - 작성자 검색")
//    void testSearchPostByAuthor() {
//        // Given
//        String searchType = "AUTHOR"; // 작성자 검색
//        String searchQuery = testUser.getUserName(); // 실제 생성된 테스트 사용자명 사용
//
//        // When
//        ResponseEntity<Page<SimplePostDTO>> response = postController.searchPost(searchType, searchQuery, 0, 10);
//
//        // Then
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().getTotalElements() > 0); // 검색 결과가 있어야 함
//    }
//
//    @Test
//    @Order(6)
//    @DisplayName("실시간 인기글 목록 조회 통합 테스트")
//    void testGetRealTimeBoard() {
//        // 디버깅: 게시글과 추천 수 확인
//        System.out.println("=== 실시간 인기글 테스트 디버깅 ===");
//        System.out.println("테스트 게시글 ID: " + testPost.getId());
//        System.out.println("테스트 게시글 생성 시간: " + testPost.getCreatedAt());
//        System.out.println("현재 시간: " + java.time.Instant.now());
//
//        long likeCount = postLikeRepository.count();
//        System.out.println("전체 추천 수: " + likeCount);
//
//        // When
//        ResponseEntity<List<SimplePostDTO>> response = postController.getRealtimeBoard();
//
//        // Then
//        assertNotNull(response.getBody());
//        System.out.println("실시간 인기글 개수: " + response.getBody().size());
//
//        // Redis 업데이트가 성공했을 때만 빈 리스트가 아닌지 검증
//        assertFalse(response.getBody().isEmpty(), "Redis 업데이트가 성공했으므로 실시간 인기글이 있어야 합니다");
//
//    }
//
//    @Test
//    @Order(7)
//    @DisplayName("주간 인기글 목록 조회 통합 테스트")
//    void testGetWeeklyBoard() {
//
//        // When
//        ResponseEntity<List<SimplePostDTO>> response = postController.getWeeklyBoard();
//
//        // Then
//        assertNotNull(response.getBody());
//        System.out.println("주간 인기글 개수: " + response.getBody().size());
//
//        assertFalse(response.getBody().isEmpty(), "Redis 업데이트가 성공했으므로 주간 인기글이 있어야 합니다");
//
//    }
//
//    @Test
//    @Order(8)
//    @DisplayName("레전드글 목록 조회 통합 테스트")
//    void testGetLegendBoard() {
//
//        // When
//        ResponseEntity<List<SimplePostDTO>> response = postController.getLegendBoard();
//
//        // Then
//        assertNotNull(response.getBody());
//        System.out.println("레전드글 개수: " + response.getBody().size());
//
//        assertFalse(response.getBody().isEmpty(), "Redis 업데이트가 성공했으므로 레전드글이 있어야 합니다");
//
//    }
//
//    @Test
//    @Order(9)
//    @DisplayName("회원 게시글 작성, 수정, 삭제 통합 테스트")
//    void testMemberPostCrud() {
//        // 1. 회원 게시글 작성
//        int randomValue = random.nextInt(1000000);
//        PostReqDTO writeReqDTO = new PostReqDTO();
//        writeReqDTO.setTitle("Member Post Title " + randomValue);
//        writeReqDTO.setContent("Member Post Content " + randomValue);
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
//
//        ResponseEntity<PostDTO> writeResponse = postController.writePost(userDetails, writeReqDTO);
//        assertNotNull(writeResponse.getBody());
//        Long postId = writeResponse.getBody().getPostId();
//        assertNotNull(postId, "회원 게시글 생성 후 ID가 null입니다.");
//        assertEquals("Member Post Title " + randomValue, writeResponse.getBody().getTitle());
//
//        // 2. 회원 게시글 수정
//        PostDTO updateReqDTO = PostDTO.existedPost(
//                postId,
//                testUser.getId(),
//                testUser.getUserName(),
//                "Updated Member Title",
//                "Updated Member Content",
//                writeResponse.getBody().getViews(),
//                writeResponse.getBody().getLikes(),
//                false,
//                null,
//                writeResponse.getBody().getCreatedAt(),
//                false);
//
//        ResponseEntity<String> updateResponse = postController.updatePost(userDetails, updateReqDTO);
//        assertEquals("게시글 수정 완료", updateResponse.getBody());
//
//        // 3. 회원 게시글 삭제
//        ResponseEntity<String> deleteResponse = postController.deletePost(userDetails, updateReqDTO);
//        assertEquals("게시글 삭제 완료", deleteResponse.getBody());
//    }
//
//    @Test
//    @Order(10)
//    @DisplayName("비회원 게시글 작성, 수정, 삭제 통합 테스트")
//    void testGuestPostCrud() {
//        // 1. 비회원 게시글 작성
//        PostReqDTO writeReqDTO = new PostReqDTO();
//        writeReqDTO.setTitle("Guest Post Title");
//        writeReqDTO.setContent("Guest Post Content");
//        writeReqDTO.setPassword(1234);
//
//        ResponseEntity<PostDTO> writeResponse = postController.writePost(null, writeReqDTO);
//        assertNotNull(writeResponse.getBody());
//        Long postId = writeResponse.getBody().getPostId();
//        assertNotNull(postId, "비회원 게시글 생성 후 ID가 null입니다.");
//        assertEquals("Guest Post Title", writeResponse.getBody().getTitle());
//
//        // 2. 비회원 게시글 수정
//        PostDTO updateReqDTO = PostDTO.existedPost(
//                postId,
//                null,
//                "비회원",
//                "Updated Guest Title",
//                "Updated Guest Content",
//                writeResponse.getBody().getViews(),
//                writeResponse.getBody().getLikes(),
//                false,
//                null,
//                writeResponse.getBody().getCreatedAt(),
//                false);
//        updateReqDTO.setPassword(1234);
//
//        ResponseEntity<String> updateResponse = postController.updatePost(null, updateReqDTO);
//        assertEquals("게시글 수정 완료", updateResponse.getBody());
//
//        // 3. 비회원 게시글 삭제
//        ResponseEntity<String> deleteResponse = postController.deletePost(null, updateReqDTO);
//        assertEquals("게시글 삭제 완료", deleteResponse.getBody());
//    }
//
//    @Test
//    @Order(15)
//    @DisplayName("게시글 추천 통합 테스트")
//    void testLikePost() {
//        // Given
//        PostDTO postDTO = PostDTO.existedPost(
//                testPost.getId(),
//                testUser.getId(),
//                testUser.getUserName(),
//                testPost.getTitle(),
//                testPost.getContent(),
//                testPost.getViews(),
//                0,
//                testPost.isNotice(),
//                null,
//                testPost.getCreatedAt(),
//                false);
//
//        // 인증 설정
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
//
//        // When
//        ResponseEntity<String> response = postController.likePost(userDetails, postDTO);
//
//        // Then
//        assertEquals("추천 처리 완료", response.getBody());
//    }
//}
