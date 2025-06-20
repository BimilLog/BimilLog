package jaeik.growfarm.integration;

import jaeik.growfarm.controller.PostController;
import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestConstructor;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.Mockito.mock;

/**
 * <h2>PostController 통합 테스트</h2>
 * <p>
 * 실제 데이터베이스와 서비스를 사용하여 PostController의 전체 API를 테스트합니다.
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
public class PostControllerIntegrationTest {

    private final PostController postController;
    private final PostRepository postRepository;
    private final SettingRepository settingRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse httpResponse;

    private Post testPost;
    private CustomUserDetails userDetails;
    private final Random random = new Random();

    public PostControllerIntegrationTest(PostController postController,
            PostRepository postRepository,
            SettingRepository settingRepository,
            TokenRepository tokenRepository,
            UserRepository userRepository) {
        this.postController = postController;
        this.postRepository = postRepository;
        this.settingRepository = settingRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @BeforeAll
    void setUp() {
        this.request = mock(HttpServletRequest.class);
        this.httpResponse = mock(HttpServletResponse.class);

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

        // ClientDTO 생성
        ClientDTO clientDTO = new ClientDTO(testUser, token.getId(), null);
        userDetails = new CustomUserDetails(clientDTO);
    }

    @AfterAll
    void tearDown() {
        // 별도 정리 로직 없음
    }

    @Test
    @Order(1)
    @DisplayName("게시판 조회 통합 테스트")
    void testGetBoard() {
        // When
        ResponseEntity<Page<SimplePostDTO>> response = postController.getBoard(0, 10);

        // Then
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(testPost.getTitle(), response.getBody().getContent().getFirst().getTitle());
        assertEquals(testPost.getUser().getUserName(), response.getBody().getContent().getFirst().getUserName());
    }

    @Test
    @Order(2)
    @DisplayName("실시간 인기글 목록 조회 통합 테스트")
    void testGetRealtimeBoard() {
        // When
        ResponseEntity<List<SimplePostDTO>> response = postController.getRealtimeBoard();

        // Then
        assertNotNull(response.getBody());
    }

    @Test
    @Order(3)
    @DisplayName("주간 인기글 목록 조회 통합 테스트")
    void testGetWeeklyBoard() {
        // When
        ResponseEntity<List<SimplePostDTO>> response = postController.getWeeklyBoard();

        // Then
        assertNotNull(response.getBody());
    }


    @Test
    @Order(4)
    @DisplayName("레전드 인기글 목록 조회 통합 테스트")
    void testGetLegendBoard() {
        // When
        ResponseEntity<List<SimplePostDTO>> response = postController.getLegendBoard();

        // Then
        assertNotNull(response.getBody());
    }


    @Test
    @Order(5)
    @DisplayName("게시글 검색 통합 테스트")
    void testSearchPost() {
        // When
        ResponseEntity<Page<SimplePostDTO>> response = postController.searchPost("title", "Test", 0, 10);

        // Then
        assertNotNull(response.getBody());
    }


    @Test
    @Order(6)
    @DisplayName("게시글 조회 통합 테스트")
    void testGetPost() {
        // When
        ResponseEntity<PostDTO> response = postController.getPost(testPost.getId(), userDetails, request, httpResponse);

        // Then
        assertNotNull(response.getBody());
        assertEquals(testPost.getTitle(), response.getBody().getTitle());
        assertEquals(testPost.getContent(), response.getBody().getContent());
    }


    @Test
    @Order(7)
    @DisplayName("게시글 작성 통합 테스트 - 회원")
    void testWritePostByUser() {
        // Given
        int uniqueId = random.nextInt(1000000);
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("Test Title " + uniqueId);
        postReqDTO.setContent("Test Content " + uniqueId);

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<PostDTO> response = postController.writePost(userDetails, postReqDTO);

        // Then
        assertNotNull(response.getBody());
        assertEquals("Test Title " + uniqueId, response.getBody().getTitle());
        assertEquals("Test Content " + uniqueId, response.getBody().getContent());
    }


    @Test
    @Order(8)
    @DisplayName("게시글 작성 통합 테스트 - 비회원")
    void testWritePostByGuest() {
        // Given
        int uniqueId = random.nextInt(1000000);
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("Guest Post Title " + uniqueId);
        postReqDTO.setContent("Guest Post Content " + uniqueId);
        postReqDTO.setUserName("비회원" + uniqueId);
        postReqDTO.setPassword(1234);

        // When
        ResponseEntity<PostDTO> response = postController.writePost(null, postReqDTO);

        // Then
        assertNotNull(response.getBody());
        assertEquals("Guest Post Title " + uniqueId, response.getBody().getTitle());
        assertEquals("Guest Post Content " + uniqueId, response.getBody().getContent());
    }

    @Test
    @Order(9)
    @DisplayName("게시글 수정 통합 테스트 - 회원")
    void testUpdatePostByUser() {
        // Given
        int uniqueId = random.nextInt(1000000);
        PostDTO postDTO = PostDTO.newPost(testPost);
        postDTO.setTitle("Updated Title " + uniqueId);
        postDTO.setContent("Updated Content " + uniqueId);

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<String> response = postController.updatePost(userDetails, postDTO);

        // Then
        assertEquals("글 수정 완료", response.getBody());
    }

    @Test
    @Order(10)
    @DisplayName("게시글 수정 통합 테스트 - 비회원")
    void testUpdatePostByGuest() {
        // Given
        int uniqueId = random.nextInt(1000000);
        // 먼저 비회원 게시글을 하나 생성
        Post guestPost = Post.builder()
                .title("Guest Post " + uniqueId)
                .content("Guest Content " + uniqueId)
                .user(null) // 비회원
                .password(1234) // 비밀번호 설정
                .views(0)
                .isNotice(false)
                .build();
        guestPost = postRepository.save(guestPost);

        PostDTO postDTO = PostDTO.existedPost(
                guestPost.getId(),
                null,
                "비회원" + uniqueId,
                "Updated Guest Title " + uniqueId,
                "Updated Guest Content " + uniqueId,
                0,
                0,
                false,
                null,
                guestPost.getCreatedAt(),
                false);
        postDTO.setPassword(1234);

        // When
        ResponseEntity<String> response = postController.updatePost(null, postDTO);

        // Then
        assertEquals("글 수정 완료", response.getBody());
    }

    @Test
    @Order(11)
    @DisplayName("게시글 삭제 통합 테스트 - 회원")
    void testDeletePostByUser() {
        // Given
        PostDTO postDTO = PostDTO.newPost(testPost);

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<String> response = postController.deletePost(userDetails, postDTO);

        // Then
        assertEquals("게시글 삭제 완료", response.getBody());
    }

    @Test
    @Order(12)
    @DisplayName("게시글 삭제 통합 테스트 - 비회원")
    void testDeletePostByGuest() {
        // Given
        int uniqueId = random.nextInt(1000000);
        // 먼저 비회원 게시글을 하나 생성
        Post guestPost = Post.builder()
                .title("Guest Post to Delete " + uniqueId)
                .content("Guest Content to Delete " + uniqueId)
                .user(null) // 비회원
                .password(1234) // 비밀번호 설정
                .views(0)
                .isNotice(false)
                .build();
        guestPost = postRepository.save(guestPost);

        PostDTO postDTO = PostDTO.existedPost(
                guestPost.getId(),
                null,
                "비회원" + uniqueId,
                "Guest Post to Delete " + uniqueId,
                "Guest Content to Delete " + uniqueId,
                0,
                0,
                false,
                null,
                guestPost.getCreatedAt(),
                false);
        postDTO.setPassword(1234);

        // When
        ResponseEntity<String> response = postController.deletePost(null, postDTO);

        // Then
        assertEquals("게시글 삭제 완료", response.getBody());
    }

    @Test
    @Order(13)
    @DisplayName("게시글 추천 통합 테스트")
    void testLikePost() {
        // Given
        PostDTO postDTO = PostDTO.newPost(testPost);

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<String> response = postController.likePost(userDetails, postDTO);

        // Then
        assertEquals("추천 처리 완료", response.getBody());
    }
}
