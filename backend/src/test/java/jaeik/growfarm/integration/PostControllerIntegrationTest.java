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
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestConstructor;

import java.util.List;

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
 * @since 2025.05.21
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Commit
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

        this.request = mock(HttpServletRequest.class);
        this.httpResponse = mock(HttpServletResponse.class);
    }

    /**
     * <h3>테스트 데이터 초기화</h3>
     * 사용자, 게시글 데이터 생성
     *
     * @since 2025.05.21
     */
    @BeforeAll
    void setUp() {
        // 사용자 설정 생성
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        settingRepository.save(setting);

        // 토큰 생성
        Token token = Token.builder()
                .jwtRefreshToken("testRefreshToken")
                .kakaoAccessToken("testKakaoAccessToken")
                .kakaoRefreshToken("testKakaoRefreshToken")
                .build();
        tokenRepository.save(token);

        // 사용자 생성
        Users user = Users.builder()
                .kakaoId(1234567890L)
                .kakaoNickname("testNickname")
                .thumbnailImage("testImage")
                .userName("testUser")
                .role(UserRole.USER)
                .setting(setting)
                .build();
        Users testUser = userRepository.save(user);

        // 게시글 생성
        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .user(testUser)
                .views(0)
                .isNotice(false)
                .build();
        testPost = postRepository.save(post);

        // ClientDTO 생성
        ClientDTO clientDTO = new ClientDTO(testUser, token.getId(), null);
        userDetails = new CustomUserDetails(clientDTO);
    }

    /**
     * <h3>게시판 조회 통합 테스트</h3>
     * 
     * @since 2025.05.21
     */
    @Test
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

    /**
     * <h3>실시간 인기글 목록 조회 통합 테스트</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("실시간 인기글 목록 조회 통합 테스트")
    void testGetRealtimeBoard() {
        // When
        ResponseEntity<List<SimplePostDTO>> response = postController.getRealtimeBoard();

        // Then
        assertNotNull(response.getBody());
    }

    /**
     * <h3>주간 인기글 목록 조회 통합 테스트</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("주간 인기글 목록 조회 통합 테스트")
    void testGetWeeklyBoard() {
        // When
        ResponseEntity<List<SimplePostDTO>> response = postController.getWeeklyBoard();

        // Then
        assertNotNull(response.getBody());
    }

    /**
     * <h3>레전드 인기글 목록 조회 통합 테스트</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("레전드 인기글 목록 조회 통합 테스트")
    void testGetLegendBoard() {
        // When
        ResponseEntity<List<SimplePostDTO>> response = postController.getLegendBoard();

        // Then
        assertNotNull(response.getBody());
    }

    /**
     * <h3>게시글 검색 통합 테스트</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("게시글 검색 통합 테스트")
    void testSearchPost() {
        // When
        ResponseEntity<Page<SimplePostDTO>> response = postController.searchPost("title", "Test", 0, 10);

        // Then
        assertNotNull(response.getBody());
    }

    /**
     * <h3>게시글 조회 통합 테스트</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("게시글 조회 통합 테스트")
    void testGetPost() {
        // When
        ResponseEntity<PostDTO> response = postController.getPost(testPost.getId(), userDetails, request, httpResponse);

        // Then
        assertNotNull(response.getBody());
        assertEquals(testPost.getTitle(), response.getBody().getTitle());
        assertEquals(testPost.getContent(), response.getBody().getContent());
    }

    /**
     * <h3>게시글 작성 통합 테스트 - 회원</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("게시글 작성 통합 테스트 - 회원")
    void testWritePostByUser() {
        // Given
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("Test Title");
        postReqDTO.setContent("Test Content");

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<PostDTO> response = postController.writePost(userDetails, postReqDTO);

        // Then
        assertNotNull(response.getBody());
        assertEquals("Test Title", response.getBody().getTitle());
        assertEquals("Test Content", response.getBody().getContent());
    }

    /**
     * <h3>게시글 작성 통합 테스트 - 비회원</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("게시글 작성 통합 테스트 - 비회원")
    void testWritePostByGuest() {
        // Given
        PostReqDTO postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("Guest Post Title");
        postReqDTO.setContent("Guest Post Content");
        postReqDTO.setUserName("비회원");
        postReqDTO.setPassword(1234);

        // When
        ResponseEntity<PostDTO> response = postController.writePost(null, postReqDTO);

        // Then
        assertNotNull(response.getBody());
        assertEquals("Guest Post Title", response.getBody().getTitle());
        assertEquals("Guest Post Content", response.getBody().getContent());
    }

    /**
     * <h3>게시글 수정 통합 테스트 - 회원</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("게시글 수정 통합 테스트 - 회원")
    void testUpdatePostByUser() {
        // Given
        PostDTO postDTO = PostDTO.newPost(testPost);
        postDTO.setTitle("Updated Title");
        postDTO.setContent("Updated Content");

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<String> response = postController.updatePost(userDetails, postDTO);

        // Then
        assertEquals("글 수정 완료", response.getBody());
    }

    /**
     * <h3>게시글 수정 통합 테스트 - 비회원</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("게시글 수정 통합 테스트 - 비회원")
    void testUpdatePostByGuest() {
        // Given
        // 먼저 비회원 게시글을 하나 생성
        Post guestPost = Post.builder()
                .title("Guest Post")
                .content("Guest Content")
                .user(null) // 비회원
                .password(1234) // 비밀번호 설정
                .views(0)
                .isNotice(false)
                .build();
        guestPost = postRepository.save(guestPost);

        PostDTO postDTO = PostDTO.existedPost(
                guestPost.getId(),
                null,
                "비회원",
                "Updated Guest Title",
                "Updated Guest Content",
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

    /**
     * <h3>게시글 삭제 통합 테스트 - 회원</h3>
     * 
     * @since 2025.05.17
     */
    @Test
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
        assertEquals("글 삭제 완료", response.getBody());
    }

    /**
     * <h3>게시글 삭제 통합 테스트 - 비회원</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("게시글 삭제 통합 테스트 - 비회원")
    void testDeletePostByGuest() {
        // Given
        // 먼저 비회원 게시글을 하나 생성
        Post guestPost = Post.builder()
                .title("Guest Post to Delete")
                .content("Guest Content to Delete")
                .user(null) // 비회원
                .password(1234) // 비밀번호 설정
                .views(0)
                .isNotice(false)
                .build();
        guestPost = postRepository.save(guestPost);

        PostDTO postDTO = PostDTO.existedPost(
                guestPost.getId(),
                null,
                "비회원",
                "Guest Post to Delete",
                "Guest Content to Delete",
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
        assertEquals("글 삭제 완료", response.getBody());
    }

    /**
     * <h3>게시글 추천 통합 테스트</h3>
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("게시글 추천 통합 테스트")
    void testLikePost() {
        // Given
        PostDTO postDTO = new PostDTO();

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<String> response = postController.likePost(userDetails, postDTO);

        // Then
        assertEquals("추천 처리 완료", response.getBody());
    }

}
