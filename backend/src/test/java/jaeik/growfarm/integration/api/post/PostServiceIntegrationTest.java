//package jaeik.growfarm.integration.api.post;
//
//import jaeik.growfarm.dto.post.PostDTO;
//import jaeik.growfarm.dto.post.PostReqDTO;
//import jaeik.growfarm.dto.post.SimplePostDTO;
//import jaeik.growfarm.entity.post.Post;
//import jaeik.growfarm.entity.post.PostLike;
//import jaeik.growfarm.entity.user.Setting;
//import jaeik.growfarm.entity.user.UserRole;
//import jaeik.growfarm.entity.user.Users;
//import jaeik.growfarm.global.auth.CustomUserDetails;
//import jaeik.growfarm.global.exception.CustomException;
//import jaeik.growfarm.global.exception.ErrorCode;
//import jaeik.growfarm.repository.post.PostLikeRepository;
//import jaeik.growfarm.repository.post.PostRepository;
//import jaeik.growfarm.repository.user.SettingRepository;
//import jaeik.growfarm.repository.user.UserRepository;
//import jaeik.growfarm.service.post.command.PostCommandService;
//import jaeik.growfarm.service.post.read.PostReadService;
//import jaeik.growfarm.service.post.search.PostSearchService;
//import jaeik.growfarm.service.redis.RedisPostService;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.domain.Page;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Instant;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.mock;
//
///**
// * <h2>Post 서비스 통합 테스트</h2>
// * <p>
// * 실제 데이터베이스를 사용하여 Post 관련 서비스들의 통합 동작을 테스트합니다.
// * </p>
// * <p>
// * Service → Repository → Database 전체 플로우를 검증합니다.
// * </p>
// *
// * @author Jaeik
// * @version 1.0.0
// */
//@SpringBootTest
//@ActiveProfiles("test")
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@Transactional
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
//public class PostServiceIntegrationTest {
//
//    @Autowired
//    private PostReadService postReadService;
//
//    @Autowired
//    private PostCommandService postCommandService;
//
//    @Autowired
//    private PostSearchService postSearchService;
//
//    @Autowired
//    private RedisPostService redisPostService;
//
//    @Autowired
//    private PostRepository postRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private SettingRepository settingRepository;
//
//    @Autowired
//    private PostLikeRepository postLikeRepository;
//
//    private Users testUser;
//    private CustomUserDetails userDetails;
//    private Post testPost;
//    private static Long createdPostId;
//
//    @BeforeEach
//    void setUp() {
//        // 테스트용 사용자 설정
//        Setting setting = Setting.builder()
//                .messageNotification(true)
//                .commentNotification(true)
//                .postFeaturedNotification(true)
//                .build();
//        settingRepository.save(setting);
//
//        testUser = Users.builder()
//                .kakaoId(12345L)
//                .kakaoNickname("testNickname")
//                .thumbnailImage("testImage.jpg")
//                .userName("integrationTestUser")
//                .role(UserRole.USER)
//                .setting(setting)
//                .build();
//        userRepository.save(testUser);
//
//        // CustomUserDetails 생성 (간단한 형태로)
//        userDetails = mock(CustomUserDetails.class);
//        org.mockito.Mockito.when(userDetails.getUserId()).thenReturn(testUser.getId());
//
//        // 테스트용 게시글 생성
//        testPost = Post.builder()
//                .title("Integration Test Post")
//                .content("Integration Test Content")
//                .user(testUser)
//                .views(0)
//                .isNotice(false)
//                .build();
//        postRepository.save(testPost);
//    }
//
//    @Test
//    @Order(1)
//    @DisplayName("게시글 목록 조회 서비스 통합 테스트")
//    void testGetBoardIntegration() {
//        // When
//        Page<SimplePostDTO> result = postReadService.getBoard(0, 10);
//
//        // Then
//        assertNotNull(result);
//        assertTrue(result.getTotalElements() >= 1);
//    }
//
//    @Test
//    @Order(2)
//    @DisplayName("게시글 상세 조회 서비스 통합 테스트")
//    void testGetPostIntegration() {
//        // When
//        PostDTO result = postReadService.getPost(testPost.getId(), userDetails);
//
//        // Then
//        assertNotNull(result);
//        assertEquals("Integration Test Post", result.getTitle());
//        assertEquals("Integration Test Content", result.getContent());
//        assertEquals("integrationTestUser", result.getUserName());
//    }
//
//    @Test
//    @Order(3)
//    @DisplayName("존재하지 않는 게시글 조회 서비스 통합 테스트")
//    void testGetNonExistentPostIntegration() {
//        // When & Then
//        CustomException exception = assertThrows(CustomException.class,
//                () -> postReadService.getPost(999999L, userDetails));
//
//        assertEquals(ErrorCode.POST_NOT_FOUND, exception.getErrorCode());
//    }
//
//    @Test
//    @Order(4)
//    @DisplayName("조회수 증가 서비스 통합 테스트")
//    void testIncrementViewCountIntegration() {
//        // Given
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        HttpServletResponse response = mock(HttpServletResponse.class);
//        org.mockito.Mockito.when(request.getCookies()).thenReturn(null);
//
//        int initialViews = testPost.getViews();
//
//        // When
//        postReadService.incrementViewCount(testPost.getId(), request, response);
//
//        // Then
//        Post updatedPost = postRepository.findById(testPost.getId()).orElse(null);
//        assertNotNull(updatedPost);
//        assertEquals(initialViews + 1, updatedPost.getViews());
//    }
//
//    @Test
//    @Order(5)
//    @DisplayName("게시글 작성 서비스 통합 테스트 - 회원")
//    void testWritePostByUserIntegration() {
//        // Given
//        PostReqDTO postReqDTO = new PostReqDTO();
//        postReqDTO.setTitle("User Integration Test Post");
//        postReqDTO.setContent("User Integration Test Content");
//
//        // When
//        PostDTO result = postCommandService.writePost(userDetails, postReqDTO);
//
//        // Then
//        assertNotNull(result);
//        assertEquals("User Integration Test Post", result.getTitle());
//        assertEquals("User Integration Test Content", result.getContent());
//        assertEquals("integrationTestUser", result.getUserName());
//
//        createdPostId = result.getPostId();
//
//        // 데이터베이스에 실제로 저장되었는지 확인
//        Post savedPost = postRepository.findById(result.getPostId()).orElse(null);
//        assertNotNull(savedPost);
//        assertEquals("User Integration Test Post", savedPost.getTitle());
//    }
//
//    @Test
//    @Order(6)
//    @DisplayName("게시글 작성 서비스 통합 테스트 - 비회원")
//    void testWritePostByGuestIntegration() {
//        // Given
//        PostReqDTO postReqDTO = new PostReqDTO();
//        postReqDTO.setTitle("Guest Integration Test Post");
//        postReqDTO.setContent("Guest Integration Test Content");
//        postReqDTO.setPassword(1234);
//
//        // When
//        PostDTO result = postCommandService.writePost(null, postReqDTO);
//
//        // Then
//        assertNotNull(result);
//        assertEquals("Guest Integration Test Post", result.getTitle());
//        assertEquals("Guest Integration Test Content", result.getContent());
//        assertEquals("익명", result.getUserName());
//        assertNull(result.getUserId());
//
//        // 데이터베이스에 실제로 저장되었는지 확인
//        Post savedPost = postRepository.findById(result.getPostId()).orElse(null);
//        assertNotNull(savedPost);
//        assertEquals("Guest Integration Test Post", savedPost.getTitle());
//        assertEquals(1234, savedPost.getPassword());
//    }
//
//    @Test
//    @Order(7)
//    @DisplayName("게시글 수정 서비스 통합 테스트 - 회원")
//    void testUpdatePostByUserIntegration() {
//        if (createdPostId == null) {
//            return; // 게시글이 생성되지 않았다면 건너뛰기
//        }
//
//        // Given
//        PostDTO updateDTO = PostDTO.existedPost(
//                createdPostId,
//                testUser.getId(),
//                "integrationTestUser",
//                "Updated Integration Test Post",
//                "Updated Integration Test Content",
//                0,
//                0,
//                false,
//                null,
//                Instant.now(),
//                false);
//
//        // When
//        postCommandService.updatePost(userDetails, updateDTO);
//
//        // Then
//        Post updatedPost = postRepository.findById(createdPostId).orElse(null);
//        assertNotNull(updatedPost);
//        assertEquals("Updated Integration Test Post", updatedPost.getTitle());
//        assertEquals("Updated Integration Test Content", updatedPost.getContent());
//    }
//
//    @Test
//    @Order(8)
//    @DisplayName("게시글 추천 서비스 통합 테스트 - 새로운 추천")
//    void testLikePostNewIntegration() {
//        // Given
//        PostDTO postDTO = PostDTO.existedPost(
//                testPost.getId(),
//                testUser.getId(),
//                "integrationTestUser",
//                testPost.getTitle(),
//                testPost.getContent(),
//                0,
//                0,
//                false,
//                null,
//                Instant.now(),
//                false);
//
//        long initialLikeCount = postLikeRepository.count();
//
//        // When
//        postCommandService.likePost(postDTO, userDetails);
//
//        // Then
//        long finalLikeCount = postLikeRepository.count();
//        assertEquals(initialLikeCount + 1, finalLikeCount);
//
//        // 추천 관계가 실제로 생성되었는지 확인
//        List<PostLike> likes = postLikeRepository.findByPostIdAndUserId(testPost.getId(), testUser.getId()).stream().toList();
//        assertEquals(1, likes.size());
//    }
//
//    @Test
//    @Order(9)
//    @DisplayName("게시글 추천 서비스 통합 테스트 - 기존 추천 취소")
//    void testLikePostCancelIntegration() {
//        // Given
//        PostDTO postDTO = PostDTO.existedPost(
//                testPost.getId(),
//                testUser.getId(),
//                "integrationTestUser",
//                testPost.getTitle(),
//                testPost.getContent(),
//                0,
//                0,
//                false,
//                null,
//                Instant.now(),
//                false);
//
//        long initialLikeCount = postLikeRepository.count();
//
//        // When (두 번째 추천 - 취소)
//        postCommandService.likePost(postDTO, userDetails);
//
//        // Then
//        long finalLikeCount = postLikeRepository.count();
//        assertEquals(initialLikeCount - 1, finalLikeCount);
//
//        // 추천 관계가 실제로 삭제되었는지 확인
//        List<PostLike> likes = postLikeRepository.findByPostIdAndUserId(testPost.getId(), testUser.getId()).stream().toList();
//        assertEquals(0, likes.size());
//    }
//
//    @Test
//    @Order(10)
//    @DisplayName("게시글 검색 서비스 통합 테스트 - 제목 검색")
//    void testSearchPostByTitleIntegration() {
//        // When
//        Page<SimplePostDTO> result = postSearchService.searchPost("title", "Integration", 0, 10);
//
//        // Then
//        assertNotNull(result);
//        assertTrue(result.getTotalElements() >= 1);
//
//        // 검색 결과에 "Integration"이 포함된 제목이 있는지 확인
//        boolean found = result.getContent().stream()
//                .anyMatch(post -> post.getTitle().contains("Integration"));
//        assertTrue(found);
//    }
//
//    @Test
//    @Order(11)
//    @DisplayName("게시글 검색 서비스 통합 테스트 - 작성자 검색")
//    void testSearchPostByAuthorIntegration() {
//        // When
//        Page<SimplePostDTO> result = postSearchService.searchPost("author", "integrationTestUser", 0, 10);
//
//        // Then
//        assertNotNull(result);
//        assertTrue(result.getTotalElements() >= 1);
//
//        // 검색 결과에 해당 작성자의 게시글이 있는지 확인
//        boolean found = result.getContent().stream()
//                .anyMatch(post -> "integrationTestUser".equals(post.getUserName()));
//        assertTrue(found);
//    }
//
//    @Test
//    @Order(12)
//    @DisplayName("Redis 인기글 캐싱 서비스 통합 테스트")
//    void testRedisPopularPostsIntegration() {
//        // Given
//        List<SimplePostDTO> mockPosts = List.of(
//                SimplePostDTO.builder()
//                        .postId(1L)
//                        .title("Popular Post")
//                        .userName("testUser")
//                        .likes(10)
//                        .views(100)
//                        .commentCount(5)
//                        .build()
//        );
//
//        // When - 캐시 저장
//        redisPostService.cachePopularPosts(RedisPostService.CachePostType.REALTIME, mockPosts);
//
//        // Then - 캐시 조회
//        List<SimplePostDTO> cachedPosts = redisPostService.getCachedPopularPosts(RedisPostService.CachePostType.REALTIME);
//
//        assertNotNull(cachedPosts);
//        assertEquals(1, cachedPosts.size());
//        assertEquals("Popular Post", cachedPosts.get(0).getTitle());
//
//        // 캐시 삭제 테스트
//        redisPostService.deletePopularPostsCache(RedisPostService.CachePostType.REALTIME);
//
//        // 캐시가 삭제되었는지 확인
//        boolean hasCache = redisPostService.hasPopularPostsCache(RedisPostService.CachePostType.REALTIME);
//        assertFalse(hasCache);
//    }
//
//    @Test
//    @Order(13)
//    @DisplayName("게시글 삭제 서비스 통합 테스트")
//    void testDeletePostIntegration() {
//        if (createdPostId == null) {
//            return;
//        }
//
//        // Given
//        PostDTO deleteDTO = PostDTO.existedPost(
//                createdPostId,
//                testUser.getId(),
//                "integrationTestUser",
//                "Title",
//                "Content",
//                0,
//                0,
//                false,
//                null,
//                Instant.now(),
//                false);
//
//        // When
//        postCommandService.deletePost(userDetails, deleteDTO);
//
//        // Then
//        Post deletedPost = postRepository.findById(createdPostId).orElse(null);
//        assertNull(deletedPost);
//    }
//
//    @Test
//    @Order(14)
//    @DisplayName("권한 없는 게시글 수정 시도 서비스 통합 테스트")
//    void testUpdatePostUnauthorizedIntegration() {
//        // Given
//        CustomUserDetails otherUserDetails = mock(CustomUserDetails.class);
//        org.mockito.Mockito.when(otherUserDetails.getUserId()).thenReturn(999L); // 존재하지 않는 사용자
//
//        PostDTO updateDTO = PostDTO.existedPost(
//                testPost.getId(),
//                testUser.getId(),
//                "integrationTestUser",
//                "Unauthorized Update",
//                "Unauthorized Content",
//                0,
//                0,
//                false,
//                null,
//                Instant.now(),
//                false);
//
//        // When & Then
//        CustomException exception = assertThrows(CustomException.class,
//                () -> postCommandService.updatePost(otherUserDetails, updateDTO));
//
//        assertEquals(ErrorCode.POST_UPDATE_FORBIDDEN, exception.getErrorCode());
//    }
//
//    @Test
//    @Order(15)
//    @DisplayName("빈 검색 결과 서비스 통합 테스트")
//    void testSearchPostEmptyResultIntegration() {
//        // When
//        Page<SimplePostDTO> result = postSearchService.searchPost("title", "NonExistentKeyword", 0, 10);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(0, result.getTotalElements());
//        assertTrue(result.getContent().isEmpty());
//    }
//}