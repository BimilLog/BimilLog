package jaeik.growfarm.controller;

import jaeik.growfarm.global.auth.JwtTokenProvider;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.UserUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class PostControllerTest {
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserUtil userUtil;
    private final JwtTokenProvider jwtTokenProvider;
    private final PostController postController;

    private Long userId;
    private String farmName;


    public PostControllerTest(TokenRepository tokenRepository,
                                 UserRepository userRepository,
                                 UserUtil userUtil,
                                 JwtTokenProvider jwtTokenProvider,
                              PostController postController) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.userUtil = userUtil;
        this.jwtTokenProvider = jwtTokenProvider;
        this.postController = postController;
    }

    @BeforeEach
    void setUp() {
//        Token token = tokenRepository.save(Token.builder()
//                .kakaoAccessToken("kakaoAccessToken")
//                .kakaoRefreshToken("kakaoRefreshToken")
//                .build());
//
//        Users user = userRepository.save(Users.builder()
//                .token(token)
//                .farmName("farmName")
//                .kakaoId(12345L)
//                .kakaoNickname("kakaoNickname")
//                .thumbnailImage("thumbnailImage")
//                .role(UserRole.USER)
//                .modifiedAt(LocalDateTime.now())
//                .createdAt(LocalDateTime.now())
//                .build());
//
//        UserDTO userDTO = userUtil.UserToDTO(user);
//
//        String accessToken = jwtTokenProvider.generateAccessToken(userDTO);
//        String refreshToken = jwtTokenProvider.generateRefreshToken(userDTO);
//        token.updateJwtRefreshToken(refreshToken);
//
//        Users user1 = userRepository.findByKakaoId(12345L);
//        this.userId = user1.getId();
//        this.farmName = user1.getFarmName();
//        UserDTO userDTO1 = userUtil.UserToDTO(user1);
//
//        CustomUserDetails customUserDetails = new CustomUserDetails(userDTO1);
//        Authentication authentication = new UsernamePasswordAuthenticationToken(
//                customUserDetails,
//                null,
//                customUserDetails.getAuthorities());
//
//        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

//    @Test
//    @DisplayName("게시글 쓰기 테스트")
//    void testWritePost() {
//        // Given
//        PostReqDTO postReqDTO = new PostReqDTO();
//        postReqDTO.setUserId(userId);
//        postReqDTO.setTitle("Test Title");
//        postReqDTO.setContent("Test Content");
//        postReqDTO.setFarmName(farmName);
//
//        // When
//        ResponseEntity<PostDTO> response = postController.writePost(postReqDTO);
//
//        // Then
//        HttpStatusCode code = response.getStatusCode();
//        PostDTO postDTO = response.getBody();
//
//        assert code.is2xxSuccessful() : "게시글 쓰기 실패";
//        assert response.getBody() != null : "게시글 DTO가 아닙니다.";
//        assert postDTO.getTitle().equals("Test Title") : "게시글 제목이 다릅니다.";
//        assert postDTO.getContent().equals("Test Content") : "게시글 내용이 다릅니다.";
//    }
}
